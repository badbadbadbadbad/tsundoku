package com.github.badbadbadbadbad.tsundoku.views;

import com.github.badbadbadbadbad.tsundoku.controllers.DatabaseRequestListener;
import com.github.badbadbadbadbad.tsundoku.external.FlowGapPane;
import com.github.badbadbadbadbad.tsundoku.external.FlowGridPane;
import com.github.badbadbadbadbad.tsundoku.external.SmoothScroll;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import com.github.badbadbadbadbad.tsundoku.models.AnimeListInfo;
import com.github.badbadbadbadbad.tsundoku.util.LazyLoader;
import com.github.badbadbadbadbad.tsundoku.util.ListFinder;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.util.Duration;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The full view component displayed in the main content pane for browse mode "Log" and media mode "Anime".
 *
 * <p>It would be far cleaner to have some LogView superclass with this inheriting, but I don't want to overcomplicate things
 * before I know what quirks the main content views for other media modes may involve (due to relying on data from external APIs).</p>
 */
public class AnimeLogView implements LazyLoaderView, PopupMakerView {

    /**
     * Aspect ratio we use for anime images.
     * This matches most of them well enough (there's no perfect standard used for anime covers).
     */
    private final double RATIO = 318.0 / 225.0;

    private final DatabaseRequestListener databaseRequestListener;

    private List<List<VBox>> unfilteredAnimeLists;
    private List<ObservableList<VBox>> filteredAnimeLists;    // ObservableList so grid headers can watch for these being empty
    private List<FlowGapPane> filteredGrids;                 // The actual grids used for UI


    private ScrollPane scrollPane;
    private StackPane stackPane;
    private LazyLoader lazyLoader;
    private ListFinder listFinder;
    private SmoothScroll smoothScroll;

    private String searchString = "";
    private String personalStatusFilter = "Any";
    private String releaseStatusFilter = "Any";
    private String startYearFilter = "";
    private String endYearFilter = "";

    private final BooleanProperty filtersHidden = new SimpleBooleanProperty(true);
    private String languagePreference;

    public AnimeLogView(DatabaseRequestListener databaseRequestListener, String languagePreference) {
        this.databaseRequestListener = databaseRequestListener;
        this.languagePreference = languagePreference;

        // Initialize empty lists
        this.unfilteredAnimeLists = new ArrayList<>(List.of(
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
        ));

        this.filteredAnimeLists = new ArrayList<>(List.of(
                FXCollections.observableArrayList(), FXCollections.observableArrayList(),
                FXCollections.observableArrayList(), FXCollections.observableArrayList(),
                FXCollections.observableArrayList()
        ));
    }


    /**
     * Called once by ViewsController, creates the whole View component
     * @return The finished view
     */
    public Region createGridView() {

        VBox root = new VBox();
        VBox.setVgrow(root, Priority.ALWAYS);
        HBox.setHgrow(root, Priority.ALWAYS);


        // StackPane wrapper to allow for popup functionality when grid element is clicked
        stackPane = new StackPane();
        VBox.setVgrow(stackPane, Priority.ALWAYS);
        HBox.setHgrow(stackPane, Priority.ALWAYS);
        stackPane.getChildren().add(root);


        // Controls above the grid
        FlowGridPane filters = createFilters();
        HBox searchAndFilterToggleBox = createSearchAndFilterToggle(filters);

        VBox controls = new VBox();
        controls.getStyleClass().add("log-grid-content-pane-controls");
        controls.setMinHeight(Control.USE_PREF_SIZE);

        controls.getChildren().addAll(searchAndFilterToggleBox, filters);


        // ScrollPane and the headers / grids it contains
        this.scrollPane = createScrollPane();


        // Loading the actual content - both the full collection, and then the filtered collection into UI grids
        loadDatabaseIntoGridsAsync();


        // ScrollPane listener to give controls a bottom border when scrolling
        Region separator = new Region();
        separator.getStyleClass().add("separator");
        separator.setOpacity(0.0);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.2), separator);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.2), separator);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);


        scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> {

            // This is so the controls-bottom-border can't start showing if the pane scroll bar is fully vertical (no scrolling possible)
            boolean canScroll = scrollPane.getContent().getBoundsInLocal().getHeight() > scrollPane.getViewportBounds().getHeight();

            if (newValue.doubleValue() > 0.00001 && canScroll) {
                if (separator.getOpacity() == 0.0 || fadeOut.getStatus() == Animation.Status.RUNNING) {
                    fadeOut.stop();
                    fadeIn.playFromStart();
                }
            } else {
                if (separator.getOpacity() == 1.0 || fadeIn.getStatus() == Animation.Status.RUNNING) {
                    fadeIn.stop();
                    fadeOut.playFromStart();
                }
            }
        });


        root.getChildren().addAll(controls, separator, scrollPane);
        return stackPane;
    }


    /**
     * First part of the controls component above the grid, the search bar and filters toggle button.
     * @param filters The FlowPane of the filters to be shown / hidden on toggle button click
     * @return The finished component
     */
    private HBox createSearchAndFilterToggle(FlowGridPane filters) {
        // Wrapper
        HBox searchAndModeBox = new HBox();
        searchAndModeBox.getStyleClass().add("search-bar-and-filter-toggle");

        // Search bar
        TextField searchBar = new TextField();
        searchBar.setId("search-bar");
        searchBar.setPromptText("Enter anime title..");
        HBox.setHgrow(searchBar, Priority.ALWAYS);


        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            // Regex to allow only english alphanumeric, JP / CN / KR characters, and spaces
            if (newValue.matches("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{Alnum} ]*")) {
                this.searchString = newValue;

                // Refresh content according to search bar text
                scrollPane.setVvalue(0);
                smoothScroll.resetAccumulatedVValue();

                if (lazyLoader != null) {
                    lazyLoader.unloadVisible();
                }

                onFiltersChanged();
            } else {
                searchBar.setText(oldValue);
            }
        });


        // Filter toggle button
        ToggleButton toggleFiltersButton = new ToggleButton("Show filters");
        toggleFiltersButton.getStyleClass().add("controls-button");

        // Filter toggle logic
        toggleFiltersButton.setOnAction(e -> {
            toggleFiltersButton.setDisable(true);

            if (!toggleFiltersButton.isSelected()) {
                toggleFiltersButton.setText("Show filters");
                hideFilters(filters, toggleFiltersButton);
            } else {
                toggleFiltersButton.setText("Hide filters");
                showFilters(filters, toggleFiltersButton);
            }
        });

        searchAndModeBox.getChildren().addAll(searchBar, toggleFiltersButton);
        return searchAndModeBox;
    }


    /**
     * The filter FlowPane of the controls component above the grid.
     * @return The finished component
     */
    private FlowGridPane createFilters() {
        FlowGridPane filtersGrid = new FlowGridPane(2, 2);
        filtersGrid.setHgap(10);
        filtersGrid.setVgap(10);
        filtersGrid.setMaxWidth(Double.MAX_VALUE);
        filtersGrid.setMinHeight(0); // Necessary for fade animation

        // We initialize with filters hidden
        filtersGrid.setMaxHeight(0);
        filtersGrid.setOpacity(0.0);


        // Filters
        VBox personalStatusFilter = createDropdownFilter("Personal status", new String[]{
                "Any",
                "In progress", "Backlog",
                "Completed", "Paused",
                "Dropped"}, "Any");

        VBox statusFilter = createDropdownFilter("Status",
                new String[]{"Any", "Complete", "Airing", "Upcoming"}, "Any");

        VBox startYearFilter = createNumberFilter("Start year ≥");
        VBox endYearFilter = createNumberFilter("End year ≤");


        filtersGrid.getChildren().addAll(personalStatusFilter, statusFilter, startYearFilter, endYearFilter);


        if (!filtersHidden.get()) {
            filtersGrid.setMaxHeight(filtersGrid.prefHeight(filtersGrid.getWidth()));
        }


        return filtersGrid;
    }


    /**
     * Creates a single "dropdown" type filter to be used in the createFilters function.
     * @param labelText String to be used for label above the dropdown filter
     * @param options Array of Strings to be used for the dropdown options
     * @param defaultValue String to be selected as the default choice
     * @return The finished component
     */
    private VBox createDropdownFilter(String labelText, String[] options, String defaultValue) {
        Label label = new Label(labelText);
        label.getStyleClass().add("filter-label");

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(options);
        comboBox.setValue(defaultValue);
        comboBox.getStyleClass().add("filter-combo-box");
        comboBox.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(comboBox, Priority.ALWAYS);

        // Filter change listeners
        if (labelText.equals("Personal status")) {
            comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                personalStatusFilter = newVal;

                scrollPane.setVvalue(0);
                smoothScroll.resetAccumulatedVValue();

                if (lazyLoader != null) {
                    lazyLoader.unloadVisible();
                }

                onFiltersChanged();
            });
        } else if (labelText.equals("Status")) {
            comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                releaseStatusFilter = newVal;

                scrollPane.setVvalue(0);
                smoothScroll.resetAccumulatedVValue();

                if (lazyLoader != null) {
                    lazyLoader.unloadVisible();
                }

                onFiltersChanged();
            });
        }

        // Even when filters are hidden, mouse cursor changes to text field cursor when hovering
        // where the number filters would be.
        filtersHidden.addListener((obs, oldValue, newValue) -> {
            comboBox.setDisable(newValue);
        });
        comboBox.setDisable(filtersHidden.get()); // Since filters are hidden on startup

        return new VBox(5, label, comboBox);
    }


    /**
     * Creates a single "number input" type filter to be used in the createFilters function.
     * @param labelText String to be used for label above the dropdown filter
     * @return The finished component
     */
    private VBox createNumberFilter(String labelText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("filter-label");

        TextField textField = new TextField("");
        textField.getStyleClass().add("filter-text-box");
        textField.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(textField, Priority.ALWAYS);


        // Numeric input and at most four digits regex filter
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.matches("\\d{0,4}")) {
                // textField.setText(oldValue);
                if (labelText.equals("Start year ≥")) {
                    this.startYearFilter = newValue;
                } else if (labelText.equals("End year ≤")) {
                    this.endYearFilter = newValue;
                }

                scrollPane.setVvalue(0);
                smoothScroll.resetAccumulatedVValue();

                if (lazyLoader != null) {
                    lazyLoader.unloadVisible();
                }

                onFiltersChanged();

            } else {
                textField.setText(oldValue);
            }
        });


        // Even when filters are hidden, mouse cursor changes to text field cursor when hovering
        // where the number filters would be.
        filtersHidden.addListener((obs, oldValue, newValue) -> {
            textField.setDisable(newValue);
        });
        textField.setDisable(filtersHidden.get()); // Since filters are hidden on startup


        // Listener to fire searches on enter press.
        // Feels more natural when it fires not only when in the search bar, but also these year fields.
        return new VBox(5, label, textField);
    }


    /**
     * Run when the filter toggle button is set to hide filters.
     * Animates the filters out of existence.
     * @param filters The FlowPane of the filters to be hidden
     * @param button The filter toggle button (included here so it can be disabled during the animation)
     */
    private void hideFilters(FlowGridPane filters, ToggleButton button) {

        filtersHidden.set(true);

        // Fade animation
        FadeTransition fade = new FadeTransition(Duration.millis(150), filters);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        // Move animation
        KeyFrame visible = new KeyFrame(Duration.ZERO, new KeyValue(filters.maxHeightProperty(), filters.getHeight()));
        KeyFrame hidden = new KeyFrame(Duration.millis(100), new KeyValue(filters.maxHeightProperty(), 0));
        Timeline move = new Timeline(visible, hidden);

        // Filters bottom padding
        KeyFrame paddingVisible = new KeyFrame(Duration.ZERO, new KeyValue(filters.paddingProperty(), new Insets(0, 0, 15, 0)));
        KeyFrame paddingHidden = new KeyFrame(Duration.millis(100), new KeyValue(filters.paddingProperty(), new Insets(0, 0, 0, 0)));
        Timeline movePadding = new Timeline(paddingVisible, paddingHidden);

        // Cooldown
        PauseTransition cooldown = new PauseTransition(Duration.millis(200));
        cooldown.setOnFinished(event -> button.setDisable(false));

        // Gather animations
        ParallelTransition test = new ParallelTransition(move, movePadding);
        SequentialTransition st = new SequentialTransition(fade, test);
        ParallelTransition pt = new ParallelTransition(cooldown, st);


        // TODO Is this right here?
        pt.setOnFinished(e -> {
            if (lazyLoader != null) {
                lazyLoader.updateVisibilityFull();
            }

        });

        pt.play();
    }


    /**
     * Run when the filter toggle button is set to show filters.
     * Animates the filters into existence.
     * @param filters The FlowPane of the filters to be shown
     * @param button The filter toggle button (included here so it can be disabled during the animation)
     */
    private void showFilters(FlowGridPane filters, ToggleButton button) {

        filtersHidden.set(false);

        // Fade animation
        FadeTransition fade = new FadeTransition(Duration.millis(150), filters);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        // Move animation
        KeyFrame hidden = new KeyFrame(Duration.ZERO, new KeyValue(filters.maxHeightProperty(), 0));
        KeyFrame visible = new KeyFrame(Duration.millis(100), new KeyValue(filters.maxHeightProperty(), filters.prefHeight(filters.getWidth())));
        Timeline move = new Timeline(hidden, visible);

        // Filters bottom padding
        KeyFrame paddingHidden = new KeyFrame(Duration.ZERO, new KeyValue(filters.paddingProperty(), new Insets(0, 0, 0, 0)));
        KeyFrame paddingVisible = new KeyFrame(Duration.millis(100), new KeyValue(filters.paddingProperty(), new Insets(0, 0, 15, 0)));
        Timeline movePadding = new Timeline(paddingHidden, paddingVisible);

        // Cooldown
        PauseTransition cooldown = new PauseTransition(Duration.millis(200));
        cooldown.setOnFinished(event -> button.setDisable(false));

        // Gather animations
        ParallelTransition test = new ParallelTransition(move, movePadding);
        SequentialTransition st = new SequentialTransition(test, fade);
        ParallelTransition pt = new ParallelTransition(cooldown, st);

        // TODO Is this right here?
        pt.setOnFinished(e -> {
            if (lazyLoader != null) {
                lazyLoader.updateVisibilityFull();
            }
        });

        // Filters are a bit squished after clicking "show filters" button until a resize if this is not done
        test.setOnFinished(e -> {
            filters.setMaxHeight(filters.prefHeight(filters.getWidth()));
        });

        pt.play();
    }


    /**
     * Creates a header component used for one of the log grids (to show which grid this is, e.g. the Backlog grid).
     * The header automatically hides if the corresponding grid is empty.
     * @param labelText The text used for the header
     * @param animeList The list of items (filtered) which the header listens to to know if it should be hidden
     * @return The finished component
     */
    private HBox createGridHeader(String labelText, ObservableList<VBox> animeList) {
        HBox headerBox = new HBox(5);
        headerBox.setPrefHeight(40);
        headerBox.setPadding(new Insets(0, 0, 10, 0));
        HBox.setHgrow(headerBox, Priority.ALWAYS);
        headerBox.setAlignment(Pos.CENTER);

        if (animeList.isEmpty()) {
            headerBox.setManaged(false);
            headerBox.setVisible(false);
        }


        // Left region
        Region leftRegion = new Region();
        leftRegion.getStyleClass().add("log-grid-header-separator");
        HBox.setHgrow(leftRegion, Priority.ALWAYS);


        // Middle label
        Label label = new Label(labelText);
        label.getStyleClass().add("log-grid-header-text");


        // Right region
        Region rightRegion = new Region();
        rightRegion.getStyleClass().add("log-grid-header-separator");
        HBox.setHgrow(rightRegion, Priority.ALWAYS);


        // Add components to the HBox
        headerBox.getChildren().addAll(leftRegion, label, rightRegion);


        // Show / hide depending on if the corresponding filtered grid has items
        animeList.addListener((ListChangeListener<VBox>) change -> {

            boolean hasItems = animeList.size() > 0;
            boolean shouldDisplay = personalStatusFilter.equals("Any") || personalStatusFilter.equals(labelText);

            if (hasItems) {
                headerBox.setMinHeight(40);
                headerBox.setMaxHeight(40);
            } else {
                headerBox.setMinHeight(0);
                headerBox.setMaxHeight(0);
            }

            headerBox.setManaged(hasItems && shouldDisplay);
            headerBox.setVisible(hasItems && shouldDisplay);
        });

        return headerBox;
    }


    /**
     * Creates a FlowPane of anime.
     * Does not actually create the child elements themselves, that is done seperately to work with the filtering.
     * @return The finished component
     */
    private FlowGapPane createGrid() {
        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();

        FlowGapPane animeGrid = new FlowGapPane(screenWidth / 9, screenWidth / 9 * RATIO, 20);
        // animeGrid.setMaxWidth(Double.MAX_VALUE);
        animeGrid.setPadding(new Insets(0, 0, 30, 0));

        return animeGrid;
    }


    /**
     * Gets the full local anime database, then splits it into sub-lists based on personal status and sorts them.
     * The UI grids are then initialized (async) with the created lists.
     *
     * <p>When the async calls (one per grid) are all finished, the filter refresh mechanism is invoked
     * so the finished correct UI can be displayed.</p>
     */
    private void loadDatabaseIntoGridsAsync() {
        // The full database
        AnimeListInfo fullDatabase = databaseRequestListener.requestFullAnimeDatabase();

        // ownRating sort
        Comparator<AnimeInfo> byRating = Comparator.comparingInt(anime -> switch (anime.getOwnRating()) {
            case "Heart" -> 1;
            case "Liked" -> 2;
            case "Disliked" -> 3;
            case "Unscored" -> 4;
            default -> Integer.MAX_VALUE; // fallback, in case of unexpected value
        });

        // Alphanumerical title sort
        Comparator<AnimeInfo> byTitle;
        if (languagePreference.equals("Japanese")) {
            byTitle = Comparator.comparing(AnimeInfo::getTitleJapanese);
        } else if (languagePreference.equals("English")) {
            byTitle = Comparator.comparing(AnimeInfo::getTitleEnglish);
        } else {
            byTitle = Comparator.comparing(AnimeInfo::getTitle);
        }

        // Combine the two sorts
        Comparator<AnimeInfo> combinedComparator = byRating.thenComparing(byTitle);


        // Split the full database into disjunct subsets based on the personal status
        ObservableList<AnimeInfo> inProgressAnimeList = FXCollections.observableArrayList(
                fullDatabase.getAnimeList().stream()
                        .filter(anime -> "In progress".equals(anime.getOwnStatus()))
                        .sorted(combinedComparator)
                        .toList()
        );

        ObservableList<AnimeInfo> backlogAnimeList = FXCollections.observableArrayList(
                fullDatabase.getAnimeList().stream()
                        .filter(anime -> "Backlog".equals(anime.getOwnStatus()))
                        .sorted(combinedComparator)
                        .toList()
        );

        ObservableList<AnimeInfo> completedAnimeList = FXCollections.observableArrayList(
                fullDatabase.getAnimeList().stream()
                        .filter(anime -> "Completed".equals(anime.getOwnStatus()))
                        .sorted(combinedComparator)
                        .toList()
        );

        ObservableList<AnimeInfo> pausedAnimeList = FXCollections.observableArrayList(
                fullDatabase.getAnimeList().stream()
                        .filter(anime -> "Paused".equals(anime.getOwnStatus()))
                        .sorted(combinedComparator)
                        .toList()
        );

        ObservableList<AnimeInfo> droppedAnimeList = FXCollections.observableArrayList(
                fullDatabase.getAnimeList().stream()
                        .filter(anime -> "Dropped".equals(anime.getOwnStatus()))
                        .sorted(combinedComparator)
                        .toList()
        );

        // Async calls for the actual loading of the database content into VBoxes
        List<CompletableFuture<Void>> gridFutures = new ArrayList<>();
        gridFutures.add(reloadAnimeGridAsync(unfilteredAnimeLists.get(0), inProgressAnimeList));
        gridFutures.add(reloadAnimeGridAsync(unfilteredAnimeLists.get(1), backlogAnimeList));
        gridFutures.add(reloadAnimeGridAsync(unfilteredAnimeLists.get(2), completedAnimeList));
        gridFutures.add(reloadAnimeGridAsync(unfilteredAnimeLists.get(3), pausedAnimeList));
        gridFutures.add(reloadAnimeGridAsync(unfilteredAnimeLists.get(4), droppedAnimeList));
        CompletableFuture<Void> allGridsLoaded = CompletableFuture.allOf(
                gridFutures.toArray(new CompletableFuture[0])
        );

        // Once all content is loaded, call onFiltersChanged so the UI grids load the content in
        allGridsLoaded.thenRun(() -> {
            Platform.runLater(() -> {
                onFiltersChanged();     // First call here also initializes the lazyLoader
                this.listFinder = new ListFinder(unfilteredAnimeLists);
            });
        });
    }


    /**
     * Invoked by loadDatabaseIntoGridsAsync.
     * Async call to create grid elements from a List of anime info.
     * @param animeGrid The grid to fill with the finished elements
     * @param animeList The information to turn into grid elements
     * @return A CompletableFuture call so loadDatabaseIntoGridsAsync can track when all grids have finished this async call
     */
    private CompletableFuture<Void> reloadAnimeGridAsync(List<VBox> animeGrid, List<AnimeInfo> animeList) {
        return CompletableFuture.supplyAsync(() -> createAnimeGridItems(animeList))
                .thenAccept(animeBoxes -> {
                    Platform.runLater(() -> {
                        animeGrid.clear();
                        animeGrid.addAll(animeBoxes);
                    });
                });
    }


    private List<VBox> createAnimeGridItems(List<AnimeInfo> animeList) {
        List<VBox> animeBoxes = new ArrayList<>();
        for (AnimeInfo anime : animeList) {
            VBox animeBox = createAnimeBox(anime);
            animeBoxes.add(animeBox);
        }
        return animeBoxes;
    }


    /**
     * Makes a VBox component to be used in the anime grids.
     * @param anime The information on the corresponding anime.
     * @return The finished component.
     */
    private VBox createAnimeBox(AnimeInfo anime) {

        // Make image into VBox background, CSS cover sizing to look okay
        // Yes, JavaFX has an Image class, but I could not get it to work properly
        VBox animeBox = new VBox();
        animeBox.setAlignment(Pos.CENTER);
        animeBox.getStyleClass().add("grid-media-box");
        animeBox.setUserData(anime);

        setRatingBorder(animeBox);


        // Clipping rectangle because JavaFX doesn't have any kind of background image clipping. WHY??
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(animeBox.widthProperty());
        clip.heightProperty().bind(animeBox.heightProperty());

        // Needs to be set here, it doesn't work if set in CSS. Thanks, JavaFX.
        clip.setArcHeight(40);
        clip.setArcWidth(40);

        animeBox.setClip(clip);




        Label testLabel = new Label();

        // Label with anime name to be shown on animeBox hover
        // Change title depending on language preference
        String title = anime.getTitle();
        if (languagePreference.equals("Japanese") && !anime.getTitleJapanese().equals("Not yet provided")) {
            title = anime.getTitleJapanese();
            testLabel.getStyleClass().add("grid-media-box-text-jp");
        } else if (languagePreference.equals("English") && !anime.getTitleEnglish().equals("Not yet provided")) {
            title = anime.getTitleEnglish();
            testLabel.getStyleClass().add("grid-media-box-text-en");
        } else {
            testLabel.getStyleClass().add("grid-media-box-text-en");
        }


        testLabel.setText(title);

        // Label with anime name to be shown on animeBox hover
        // Label testLabel = new Label(anime.getTitle());
        testLabel.setAlignment(Pos.CENTER);
        testLabel.getStyleClass().add("grid-media-box-text");
        testLabel.setOpacity(0.0); // Seperate out to allow for fade animation


        // AnchorPane wrapper to hold the label because JavaFX freaks out with animeBox sizing otherwise
        AnchorPane ap = new AnchorPane();
        ap.setMaxHeight(Double.MAX_VALUE);
        ap.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(ap, Priority.ALWAYS);
        HBox.setHgrow(ap, Priority.ALWAYS);
        ap.getStyleClass().add("grid-media-box-anchor");


        // We set the anchors to grow two pixels outwards because the animeBox borders look a little aliased otherwise.
        AnchorPane.setBottomAnchor(testLabel, -2.0);
        AnchorPane.setTopAnchor(testLabel, -2.0);
        AnchorPane.setLeftAnchor(testLabel, -2.0);
        AnchorPane.setRightAnchor(testLabel, -2.0);


        ap.getChildren().add(testLabel);
        animeBox.getChildren().add(ap);


        // This fixes the different label sizes causing different animeBox sizes.
        // I don't know why..
        testLabel.setMaxWidth(0.0);


        // Fade events for the label popup
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.2), testLabel);
        fadeIn.setToValue(1.0);
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.2), testLabel);
        fadeOut.setToValue(0.0);
        animeBox.setOnMouseEntered(event -> fadeIn.playFromStart());
        animeBox.setOnMouseExited(event -> fadeOut.playFromStart());


        animeBox.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            // Platform.runLater needed to trigger layout update post-resizing
            // Has a chance to get a bit wonky on window snaps otherwise
            Platform.runLater(() -> {
                double newHeight = newWidth.doubleValue() * RATIO;
                animeBox.setMinHeight(newHeight);
                animeBox.setPrefHeight(newHeight);
                animeBox.setMaxHeight(newHeight);
            });
        });

        // Popup when the box is clicked
        animeBox.setOnMouseClicked(event -> {
            createPopupScreen(animeBox);
        });


        // Initialize as non-visible so the scrollpane image loading listener updates it correctly
        animeBox.setVisible(false);

        return animeBox;
    }


    /**
     * Creates a PopupView for an anime (and a window darkener effect) when its VBox in the FlowPane is clicked.
     * @param parentBox The anime box that was clicked
     */
    private void createPopupScreen(VBox parentBox) {
        // Fake darkener effect
        VBox darkBackground = new VBox();
        darkBackground.getStyleClass().add("grid-media-popup-background");
        VBox.setVgrow(darkBackground, Priority.ALWAYS);
        HBox.setHgrow(darkBackground, Priority.ALWAYS);

        // The actual popup
        AnimePopupView animePopupView = new AnimePopupView(parentBox, this, databaseRequestListener, darkBackground, languagePreference);
        VBox popupBox = animePopupView.createPopup();

        // Initially transparent for fade-in effect
        darkBackground.setOpacity(0);
        popupBox.setOpacity(0);

        stackPane.getChildren().addAll(darkBackground, popupBox);

        // Fade-in animations
        FadeTransition fadeInBackground = new FadeTransition(Duration.seconds(0.2), darkBackground);
        fadeInBackground.setFromValue(0);
        fadeInBackground.setToValue(0.8);

        FadeTransition fadeInInfoBox = new FadeTransition(Duration.seconds(0.2), popupBox);
        fadeInInfoBox.setFromValue(0);
        fadeInInfoBox.setToValue(1);

        fadeInBackground.play();
        fadeInInfoBox.play();

        darkBackground.setOnMouseClicked(backGroundEvent -> {

            // Fade-out animations
            FadeTransition fadeOutBackground = new FadeTransition(Duration.seconds(0.2), darkBackground);
            fadeOutBackground.setFromValue(0.8);
            fadeOutBackground.setToValue(0);

            FadeTransition fadeOutInfoBox = new FadeTransition(Duration.seconds(0.2), popupBox);
            fadeOutInfoBox.setFromValue(1);
            fadeOutInfoBox.setToValue(0);

            // Destroy after fade-out
            fadeOutInfoBox.setOnFinished(e -> stackPane.getChildren().removeAll(darkBackground, popupBox));
            fadeOutBackground.play();
            fadeOutInfoBox.play();
        });
    }


    /**
     * Invoked when a created PopupView is closed.
     * If the anime was set to Untracked in the PopupView, it is deleted from the corresponding grid.
     * Else, its position in the grids is adjusted (depending on the new personal status and rating).
     * At the end, onFiltersChanged is invoked to update visibilities correctly.
     * @param popupParent The parent PopupView whose closing invoked this function call
     */
    @Override
    public void onPopupClosed(VBox popupParent) {
        // Old info of popup spawner
        AnimeInfo animeOld = (AnimeInfo) popupParent.getUserData();

        // New info of popup spawner from database
        AnimeInfo animeNew = databaseRequestListener.requestAnimeFromDatabase(animeOld.getId());

        // Get node index of the popup spawner
        int index = listFinder.findNodeIndexByGridChild(popupParent);

        // Get popup spawner and corresponding pane
        Pair<List<VBox>, Integer> pair = listFinder.findPaneAndChildIndex(index);


        // If anime not in database any longer, delete from unfiltered list
        if (animeNew == null) {
            int idx = pair.getValue();
            pair.getKey().remove(idx);
        }

        // Else: Update position in grids (remove old item, make new animeBox at new position)
        // This includes the case of the anime not changing status / rating, but we'll just be lazy here
        else {

            VBox newAnimeBox = createAnimeBox(animeNew);
            AnimeInfo newAnimeInfo = (AnimeInfo) newAnimeBox.getUserData();

            // Get correct grid to insert in
            List<VBox> targetGrid = switch (newAnimeInfo.getOwnStatus()) {
                case "In progress" -> unfilteredAnimeLists.get(0);
                case "Backlog" -> unfilteredAnimeLists.get(1);
                case "Completed" -> unfilteredAnimeLists.get(2);
                case "Paused" -> unfilteredAnimeLists.get(3);
                case "Dropped" -> unfilteredAnimeLists.get(4);
                default -> throw new IllegalArgumentException("Invalid status trying to insert new animeBox into log: " + newAnimeInfo.getOwnStatus());
            };

            // ownRating sort
            Comparator<AnimeInfo> byRating = Comparator.comparingInt(anime -> switch (anime.getOwnRating()) {
                case "Heart" -> 1;
                case "Liked" -> 2;
                case "Disliked" -> 3;
                case "Unscored" -> 4;
                default -> Integer.MAX_VALUE; // fallback, in case of unexpected value
            });

            // Alphanumerical title sort
            Comparator<AnimeInfo> byTitle = Comparator.comparing(AnimeInfo::getTitle);


            int insertIndex = 0;
            int oldIndex = pair.getValue(); // Index of this anime's current position in grid
            boolean hasPassedOldIndex = false;
            boolean sortedByRating = false;

            // Loop through grid to find correct position
            while (insertIndex <= targetGrid.size()) {

                if (insertIndex == targetGrid.size())
                    break;

                // Special case: New item passes itself still in the list (only possible if status hasn't changed)
                if (insertIndex == oldIndex && pair.getKey() == targetGrid) {
                    insertIndex++;
                    hasPassedOldIndex = true;
                    continue;
                }

                VBox box = (VBox) targetGrid.get(insertIndex);
                AnimeInfo existingAnimeInfo = (AnimeInfo) box.getUserData();


                // If next box has worse rating than new box, then we're sorted
                int ratingComparison = byRating.compare(newAnimeInfo, existingAnimeInfo);
                if (ratingComparison < 0) {
                    break;
                }

                // If not sorted by rating yet, keep going until next box has same rating as new box
                if (!sortedByRating) {
                    if (ratingComparison == 0) {
                        sortedByRating = true;
                        continue;
                    }
                    insertIndex++;
                }

                // Once rating-sorted, keep going until name-sorted
                else {

                    int nameComparison = byTitle.compare(newAnimeInfo, existingAnimeInfo);
                    if (nameComparison <= 0) {
                        break;
                    }
                    insertIndex++;
                }

            }


            // If anime passed itself during sorting process, we need to decrement once because its old entry is about to be deleted
            if (hasPassedOldIndex) {
                insertIndex--;
            }


            // Remove old entry
            int idx = pair.getValue();
            pair.getKey().remove(idx);

            // Insert at new position
            targetGrid.add(insertIndex, newAnimeBox);
        }


        onFiltersChanged();
    }


    /**
     * Invoked when any filters are changed or a PopupView is closed.
     * Refreshes the filtered anime lists based on the current filter contents, then
     * reloads the grid with the new filtered lists to display, and finally invokes a
     * LazyLoader visibility update.
     *
     * <p>The LazyLoader is also created here during creation of the Log View.</p>
     */
    private void onFiltersChanged() {

        for (int i = 0; i < unfilteredAnimeLists.size(); i++) {
            List<VBox> currentGrid = unfilteredAnimeLists.get(i);
            ObservableList<VBox> filteredList = filteredAnimeLists.get(i);

            filteredList.clear();

            for (VBox node : currentGrid) {
                if (node instanceof VBox animeBox) {
                    AnimeInfo animeInfo = (AnimeInfo) animeBox.getUserData();
                    if (animeInfo == null) continue;



                    boolean matches = true;

                    // 1. Apply searchString filter
                    if (searchString != null && !searchString.isEmpty()) {
                        String lowerSearchString = searchString.toLowerCase();
                        String title = animeInfo.getTitle() != null ? animeInfo.getTitle().toLowerCase() : null;
                        String titleJapanese = animeInfo.getTitleJapanese() != null ? animeInfo.getTitleJapanese().toLowerCase() : null;
                        String titleEnglish = animeInfo.getTitleEnglish() != null ? animeInfo.getTitleEnglish().toLowerCase() : null;

                        matches = (title != null && title.contains(lowerSearchString)) ||
                                (titleJapanese != null && titleJapanese.contains(lowerSearchString)) ||
                                (titleEnglish != null && titleEnglish.contains(lowerSearchString));
                    }

                    // 2. Apply personalStatusFilter
                    if (matches && !"Any".equals(personalStatusFilter)) {
                        String ownStatus = animeInfo.getOwnStatus();
                        matches = personalStatusFilter.equals(ownStatus);
                    }

                    // 3. Apply releaseStatusFilter
                    if (matches && !"Any".equals(releaseStatusFilter)) {
                        String publicationStatus = animeInfo.getPublicationStatus();
                        matches = releaseStatusFilter.equals(publicationStatus);
                    }

                    // 4. Apply startYearFilter
                    if (matches && startYearFilter != null && !startYearFilter.isEmpty()) {
                        String release = animeInfo.getRelease();
                        if (!"Not yet provided".equals(release)) {
                            try {
                                int startYear = Integer.parseInt(startYearFilter);
                                int releaseYear = Integer.parseInt(release.substring(release.length() - 4));
                                matches = releaseYear >= startYear;
                            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                                matches = false; // Filter out if invalid release year
                            }
                        } else {
                            matches = false; // Filter out if release year is not provided
                        }
                    }

                    // 5. Apply endYearFilter
                    if (matches && endYearFilter != null && !endYearFilter.isEmpty()) {
                        String release = animeInfo.getRelease();
                        if (!"Not yet provided".equals(release)) {
                            try {
                                int endYear = Integer.parseInt(endYearFilter);
                                int releaseYear = Integer.parseInt(release.substring(release.length() - 4));
                                matches = releaseYear <= endYear;
                            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                                matches = false; // Filter out if invalid release year
                            }
                        } else {
                            matches = false; // Filter out if release year is not provided
                        }
                    }

                    // If the item matches all active filters, add it to the filtered list
                    if (matches) {
                        filteredList.add(animeBox);
                    }
                }
            }
        }


        // Update the filtered grids in a single Platform.runLater call
        Platform.runLater(() -> {

            int totalNodes = 0;

            for (int i = 0; i < filteredGrids.size(); i++) {

                FlowGapPane filteredGrid = filteredGrids.get(i);
                ObservableList<VBox> filteredList = filteredAnimeLists.get(i);

                filteredGrid.getChildren().clear();
                filteredGrid.getChildren().addAll(filteredList);

                totalNodes += filteredList.size();

                if (filteredList.isEmpty()) {
                    filteredGrid.setMaxHeight(0);
                    filteredGrid.setPadding(new Insets(0, 0, 0, 0));
                } else {
                    filteredGrid.setMaxHeight(Double.MAX_VALUE);
                    filteredGrid.setPadding(new Insets(0, 0, 30, 0));
                }

            }


            if (scrollPane != null) {
                // scrollPane.setVvalue(0);
                smoothScroll.adjustAccumulatedVValue();
            }


            if (totalNodes == 0) {
                return;
            }


            // For the startup call
            if (lazyLoader == null) {
                lazyLoader = new LazyLoader(scrollPane, filteredGrids);
            }


            lazyLoader.setFirstVisibleIndex(0);
            lazyLoader.setLastVisibleIndex(0);

            lazyLoader.updateVisibilityFull();
        });
    }


    /**
     * Changes the border of an anime in a FlowPane based on its rating.
     * <ul>
     *     <li>Grey: Default</li>
     *     <li>Gold: Rated with Heart</li>
     *     <li>Green: Rated with Liked</li>
     *     <li>Red: Rated with Disliked</li>
     * </ul>
     * This is done by altering CSS classes of the anime's VBox component in the FlowPane.
     * @param animeBox The box to change the border for
     */
    private void setRatingBorder(VBox animeBox) {
        AnimeInfo anime = (AnimeInfo) animeBox.getUserData();

        animeBox.getStyleClass().removeAll(
                "grid-media-box-gold",
                "grid-media-box-green",
                "grid-media-box-red",
                "grid-media-box-grey"
        );

        switch (anime.getOwnRating()) {
            case "Heart" -> animeBox.getStyleClass().add("grid-media-box-gold");
            case "Liked" -> animeBox.getStyleClass().add("grid-media-box-green");
            case "Disliked" -> animeBox.getStyleClass().add("grid-media-box-red");
            default -> animeBox.getStyleClass().add("grid-media-box-grey");
        }
    }


    /**
     * Creates the wrapping scrollPane for the different grids used (so they all become one long scrollable content view),
     * together with the headers and empty grids it is filled with.
     * @return The finished component
     */
    private ScrollPane createScrollPane() {

        // Grid headers
        List<HBox> headers = List.of(
                createGridHeader("In progress", filteredAnimeLists.get(0)),
                createGridHeader("Backlog", filteredAnimeLists.get(1)),
                createGridHeader("Completed", filteredAnimeLists.get(2)),
                createGridHeader("Paused", filteredAnimeLists.get(3)),
                createGridHeader("Dropped", filteredAnimeLists.get(4))
        );


        // Grids
        this.filteredGrids = new ArrayList<>(List.of(
                createGrid(), createGrid(), createGrid(), createGrid(), createGrid()
        ));


        // Content wrapper
        VBox wrapper = new VBox(0);
        // wrapper.setMaxWidth(Double.MAX_VALUE);

        for (int i = 0; i < headers.size(); i++) {
            wrapper.getChildren().add(headers.get(i));
            wrapper.getChildren().add(filteredGrids.get(i));
        }

        ScrollPane scrollPane = new ScrollPane(wrapper);
        scrollPane.getStyleClass().add("grid-scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);


        for (FlowGapPane filteredGrid : filteredGrids) {
            filteredGrid.setWrapperPane(scrollPane);
        }

        // Call LazyLoader when window is shrunk or expanded
        scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> {
            if (lazyLoader != null) {
                lazyLoader.updateVisibilityFull();
            }
        });


        scrollPane.heightProperty().addListener((obs, oldValue, newValue) -> {
            if (lazyLoader != null) {
                Platform.runLater(() -> {
                    lazyLoader.updateVisibilityFull();
                });

            }
        });

        this.smoothScroll = new SmoothScroll(scrollPane, wrapper);


        // Another instance of having to reset the SmoothScroll accumulator on pane changes
        // Needs to be on a small delay, else it doesn't always work.
        // Not quite sure why, probably due to _when_ exactly rendering of pane size happens in the render pipeline etc.
        PauseTransition pause = new PauseTransition(Duration.seconds(0.3));
        scrollPane.widthProperty().addListener((obs, oldValue, newValue) -> {
            pause.setOnFinished(e -> {
                smoothScroll.adjustAccumulatedVValue();
            });
            pause.playFromStart();
        });

        return scrollPane;
    }


    public void shutdownLazyLoader() {
        if (lazyLoader != null) {
            lazyLoader.shutdownImageLoaderExecutor();
        }
    }
}
