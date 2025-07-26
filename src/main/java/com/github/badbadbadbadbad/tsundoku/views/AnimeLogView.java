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
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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

    private final Stage stage;
    private final DatabaseRequestListener databaseRequestListener;

    private List<List<VBox>> unfilteredAnimeLists;
    private List<ObservableList<VBox>> filteredAnimeLists;    // ObservableList so grid headers can watch for these being empty
    private List<FlowGapPane> filteredGrids;                 // The actual grids used for UI


    private ScrollPane scrollPane;
    private StackPane stackPane;
    private LazyLoader lazyLoader;
    private ListFinder listFinder;
    private SmoothScroll smoothScroll;


    private final Map<String, Consumer<String>> filterUpdaters = new HashMap<>();
    private String searchString = "";
    private String personalStatusFilter = "Any";
    private String personalRatingFilter = "Any";
    private String releaseStatusFilter = "Any";
    private String ageRatingFilter = "Any";
    private String minEpisodeFilter = "";
    private String maxEpisodeFilter = "";
    private String startYearFilter = "";
    private String endYearFilter = "";
    private String seasonFilter = "Any";
    private String typeFilter = "Any";

    private final BooleanProperty filtersHidden = new SimpleBooleanProperty(true);
    private String languagePreference;

    public AnimeLogView(Stage stage, DatabaseRequestListener databaseRequestListener, String languagePreference) {
        this.stage = stage;
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

        // Filters must know which internal variable to update with the chosen setting
        this.filterUpdaters.put("Personal status", newValue -> this.personalStatusFilter = newValue);
        this.filterUpdaters.put("Personal rating", newValue -> this.personalRatingFilter = newValue);
        this.filterUpdaters.put("Release status", newValue -> this.releaseStatusFilter = newValue);
        this.filterUpdaters.put("Age rating", newValue -> this.ageRatingFilter = newValue);
        this.filterUpdaters.put("Episodes ≥", newValue -> this.minEpisodeFilter = newValue);
        this.filterUpdaters.put("Episodes ≤", newValue -> this.maxEpisodeFilter = newValue);
        this.filterUpdaters.put("Year ≥", newValue -> this.startYearFilter = newValue);
        this.filterUpdaters.put("Year ≤", newValue -> this.endYearFilter = newValue);
        this.filterUpdaters.put("Season", newValue -> this.seasonFilter = newValue);
        this.filterUpdaters.put("Type", newValue -> this.typeFilter = newValue);
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


        // Give controls a bottom border when scrolling around
        Region separator = new Region();
        separator.getStyleClass().add("separator");
        separator.setOpacity(0.0);

        FadeTransition fader = new FadeTransition(Duration.seconds(0.2), separator);
        this.smoothScroll.accumulatedTargetVValueProp.addListener((obs, oldValue, newValue) -> {

            boolean canScroll = scrollPane.getContent().getBoundsInLocal().getHeight() > scrollPane.getViewportBounds().getHeight();

            if (!canScroll) {
                fader.setToValue(0.0);
                fader.playFromStart();
            } else if (oldValue.doubleValue() == 0.0 && newValue.doubleValue() > 0.00) {
                fader.setToValue(1.0);
                fader.playFromStart();
            } else if (oldValue.doubleValue() > 0.00 && newValue.doubleValue() == 0.00) {
                fader.setToValue(0.0);
                fader.playFromStart();
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

            this.searchString = newValue;

            scrollPane.setVvalue(0);
            smoothScroll.resetAccumulatedVValue();

            if (lazyLoader != null) {
                lazyLoader.unloadVisible();
            }

            onFiltersChanged();

            /*
            // Regex to allow only english alphanumeric, JP / CN / KR characters, and spaces,
            // as well as some special characters for JP / CN / KR languages.
            if (newValue.matches("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{Alnum} \\u30FB\\u30FC\\u301C\\u3001\\u3002\\u2014\\u2013\\u300A\\u300B\\u00B7\\uFF01\\uFF1F]*")) {
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

             */
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
        // FlowGridPane filtersGrid = new FlowGridPane(2, 3);
        FlowGridPane filtersGrid = new FlowGridPane(4, 2);
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

        VBox personalRatingFilter = createDropdownFilter("Personal rating", new String[]{
                "Any", "Heart", "Liked", "Disliked", "Unscored"}, "Any");

        VBox releaseStatusFilter = createDropdownFilter("Release status",
                new String[]{"Any", "Complete", "Airing", "Upcoming"}, "Any");

        VBox ageRatingFilter = createDropdownFilter("Age rating",
                new String[]{"Any", "G", "PG", "PG13", "R17+", "R+", "Rx"}, "Any");

        VBox releaseSeasonFilter = createDropdownFilter("Season",
                new String[]{"Any", "Winter", "Spring", "Summer", "Fall"}, "Any");

        HBox yearFilter = createDoubleNumberFilter("Year ≥", "Year ≤");
        HBox episodeFilter = createDoubleNumberFilter("Episodes ≥", "Episodes ≤");

        VBox typeFilter = createDropdownFilter("Type",
                new String[]{"Any", "TV", "Movie", "OVA", "Special", "ONA", "Music", "CM", "PV", "TV Special"}, "Any");


        filtersGrid.getChildren().addAll(personalStatusFilter, personalRatingFilter, releaseStatusFilter, ageRatingFilter,
                releaseSeasonFilter, yearFilter, episodeFilter, typeFilter);



        if (!filtersHidden.get()) {
            filtersGrid.setMaxHeight(filtersGrid.prefHeight(filtersGrid.getWidth()));
        }


        // Scale filter columns to window width
        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();
        int filtersAmount = filtersGrid.getChildren().size();
        stage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double windowWidth = newWidth.doubleValue();
            int cols, rows;

            if (windowWidth < screenWidth * 0.66)
                cols = 2;
            else
                cols = 4;

            rows = (int) Math.ceil((double) filtersAmount / cols); // Need an int value, but need float division, hence ugly casting..

            filtersGrid.setColsCount(cols);
            filtersGrid.setRowsCount(rows);

            // Necessary for the fade animation to work
            if (!filtersHidden.get()) {
                filtersGrid.setMaxHeight(filtersGrid.prefHeight(filtersGrid.getWidth()));
            }
        });


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

        // Filter change listener
        if (this.filterUpdaters.containsKey(labelText)) {
            comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                filterUpdaters.get(labelText).accept(newVal);

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
     * @param labelText String to be used for label above the number filter
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
        Consumer<String> updater = filterUpdaters.get(labelText);
        if (updater != null) {
            textField.textProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue.matches("\\d{0,4}")) {
                    updater.accept(newValue);

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
        }


        // Even when filters are hidden, mouse cursor changes to text field cursor when hovering
        // where the number filters would be.
        filtersHidden.addListener((obs, oldValue, newValue) -> {
            textField.setDisable(newValue);
        });
        textField.setDisable(filtersHidden.get()); // Since filters are hidden on startup


        VBox container = new VBox(5, label, textField);
        HBox.setHgrow(container, Priority.ALWAYS);
        return container;
    }


    /**
     * Creates two number filters as one filter box of two half-widths as our number filters come in natural pairs.
     * @param labelText1 String to be used for label above first number filter
     * @param labelText2 String to be used for label above second number filter
     * @return The finished component
     */
    private HBox createDoubleNumberFilter(String labelText1, String labelText2) {
        VBox filter1 = createNumberFilter(labelText1);
        VBox filter2 = createNumberFilter(labelText2);

        HBox container = new HBox(10, filter1, filter2);
        container.setPrefWidth(200); // NECESSARY SO HBOX GETS SCALED BY FLOWGRIDPANE CORRECTLY
        return container;
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
        Label label = new Label(labelText + "(0)");
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
            boolean shouldDisplay = personalStatusFilter.equals("Any") ||
                    personalStatusFilter.substring(0, 2).equals(labelText.substring(0, 2));

            if (hasItems) {
                headerBox.setMinHeight(40);
                headerBox.setMaxHeight(40);
            } else {
                headerBox.setMinHeight(0);
                headerBox.setMaxHeight(0);
            }

            headerBox.setManaged(hasItems && shouldDisplay);
            headerBox.setVisible(hasItems && shouldDisplay);

            label.setText(labelText + " (" + animeList.size() +")");
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
            AnimeBox animeBox = new AnimeBox(anime, languagePreference);
            animeBox.setOnMouseClick(this::createPopupScreen);

            AnimeInfo databaseAnime = databaseRequestListener.requestAnimeFromDatabase(anime.getId());
            animeBox.setRatingBorder(databaseAnime, false);

            animeBoxes.add(animeBox);
        }
        return animeBoxes;
    }

    /**
     * Creates a PopupView for an anime (and a window darkener effect) when its VBox in the FlowPane is clicked.
     * @param parentBox The anime box that was clicked
     */
    private void createPopupScreen(AnimeBox parentBox) {
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
    public void onPopupClosed(AnimeBox popupParent) {
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

            AnimeBox newAnimeBox = new AnimeBox(animeNew, languagePreference);
            newAnimeBox.setOnMouseClick(this::createPopupScreen);
            newAnimeBox.setRatingBorder(animeNew, false);

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


                    // Search string filter
                    if (searchString != null && !searchString.isEmpty()) {
                        String lowerSearchString = searchString.toLowerCase();
                        String title = animeInfo.getTitle() != null ? animeInfo.getTitle().toLowerCase() : "";
                        String titleJapanese = animeInfo.getTitleJapanese() != null ? animeInfo.getTitleJapanese().toLowerCase() : "";
                        String titleEnglish = animeInfo.getTitleEnglish() != null ? animeInfo.getTitleEnglish().toLowerCase() : "";

                        if (!(title.contains(lowerSearchString) ||
                                titleJapanese.contains(lowerSearchString) ||
                                titleEnglish.contains(lowerSearchString))) {
                            continue;
                        }
                    }

                    // Personal status filter
                    if (!"Any".equals(personalStatusFilter) && !personalStatusFilter.equals(animeInfo.getOwnStatus())) {
                        continue;
                    }

                    // Personal rating filter
                    if (!"Any".equals(personalRatingFilter) && !personalRatingFilter.equals(animeInfo.getOwnRating())) {
                        continue;
                    }

                    // Release status filter
                    if (!"Any".equals(releaseStatusFilter) && !releaseStatusFilter.equals(animeInfo.getPublicationStatus())) {
                        continue;
                    }

                    // Age rating filter
                    if (!"Any".equals(ageRatingFilter) && !ageRatingFilter.equals(animeInfo.getAgeRating())) {
                        continue;
                    }

                    // Episode filters
                    if (minEpisodeFilter != null && !minEpisodeFilter.isEmpty()) {
                        try {
                            int minEpisodes = Integer.parseInt(minEpisodeFilter);
                            if (animeInfo.getEpisodesTotal() < minEpisodes) {
                                continue;
                            }
                        } catch (NumberFormatException e) {
                        }
                    }

                    if (maxEpisodeFilter != null && !maxEpisodeFilter.isEmpty()) {
                        try {
                            int maxEpisodes = Integer.parseInt(maxEpisodeFilter);
                            if (animeInfo.getEpisodesTotal() > maxEpisodes) {
                                continue;
                            }
                        } catch (NumberFormatException e) {
                        }
                    }

                    // Release year filters
                    if (startYearFilter != null && !startYearFilter.isEmpty()) {
                        String release = animeInfo.getRelease();
                        if (!"Not yet provided".equals(release)) {
                            try {
                                int startYear = Integer.parseInt(startYearFilter);
                                int releaseYear = Integer.parseInt(release.substring(release.length() - 4));
                                if (releaseYear < startYear) continue;
                            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    }

                    if (endYearFilter != null && !endYearFilter.isEmpty()) {
                        String release = animeInfo.getRelease();
                        if (!"Not yet provided".equals(release)) {
                            try {
                                int endYear = Integer.parseInt(endYearFilter);
                                int releaseYear = Integer.parseInt(release.substring(release.length() - 4));
                                if (releaseYear > endYear) continue;
                            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    }

                    // Release season filter
                    if (!"Any".equals(seasonFilter)) {
                        String release = animeInfo.getRelease();
                        if (!"Not yet provided".equals(release)) {
                            String season = release.substring(0, release.length() - 5); // Trim spacebar and four-digit release year
                            if (!seasonFilter.equals(season)) {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    }

                    // Type filter
                    if (!"Any".equals(typeFilter) && !typeFilter.equals(animeInfo.getType())) {
                        continue;
                    }


                    // If all filters passed, add to filtered list
                    filteredList.add(animeBox);
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
