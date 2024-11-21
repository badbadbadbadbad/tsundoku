package com.github.badbadbadbadbad.tsundoku.views;

import com.github.badbadbadbadbad.tsundoku.controllers.DatabaseRequestListener;
import com.github.badbadbadbadbad.tsundoku.external.FlowGridPane;
import com.github.badbadbadbadbad.tsundoku.external.SmoothScroll;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import com.github.badbadbadbadbad.tsundoku.models.AnimeListInfo;
import com.github.badbadbadbadbad.tsundoku.util.LazyLoader;
import com.github.badbadbadbadbad.tsundoku.util.ListFinder;
import com.github.badbadbadbadbad.tsundoku.util.PaneFinder;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AnimeLogView implements LazyLoaderView, PopupMakerView {

    private final double RATIO = 318.0 / 225.0; // The aspect ratio to use for anime images. This doesn't match all exactly, but is close enough.

    private final Stage stage;
    private final DatabaseRequestListener databaseRequestListener;

    private List<ObservableList<AnimeInfo>> animeLists;
    // private List<FlowGridPane> grids;
    private List<List<VBox>> grids;

    private List<ObservableList<VBox>> filteredAnimeLists;
    private List<FlowGridPane> filteredGrids;


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

    public AnimeLogView(Stage stage, DatabaseRequestListener databaseRequestListener) {
        this.stage = stage;
        this.databaseRequestListener = databaseRequestListener;
    }

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


        // Database information for grids
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
        Comparator<AnimeInfo> byTitle = Comparator.comparing(AnimeInfo::getTitle);

        // Combine rating and alphanumerical title sort
        Comparator<AnimeInfo> combinedComparator = byRating.thenComparing(byTitle);

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

        this.animeLists = new ArrayList<>();
        Collections.addAll(animeLists, inProgressAnimeList, backlogAnimeList, completedAnimeList, pausedAnimeList, droppedAnimeList);



        // Full Grids
        List<VBox> inProgressList = new ArrayList<>();
        List<VBox> backlogList = new ArrayList<>();
        List<VBox> completedList = new ArrayList<>();
        List<VBox> pausedList = new ArrayList<>();
        List<VBox> droppedList = new ArrayList<>();

        this.grids = new ArrayList<>();
        Collections.addAll(grids, inProgressList, backlogList, completedList, pausedList, droppedList);



        // Filtered ObservableLists of VBoxes. The headers listen to these so they can disappear when these are empty.
        ObservableList<VBox> filteredInProgressAnimeList = FXCollections.observableArrayList();
        ObservableList<VBox> filteredBacklogAnimeList = FXCollections.observableArrayList();
        ObservableList<VBox> filteredCompletedAnimeList = FXCollections.observableArrayList();
        ObservableList<VBox> filteredPausedAnimeList = FXCollections.observableArrayList();
        ObservableList<VBox> filteredDroppedAnimeList = FXCollections.observableArrayList();

        this.filteredAnimeLists = new ArrayList<>();
        Collections.addAll(filteredAnimeLists, filteredInProgressAnimeList, filteredBacklogAnimeList, filteredCompletedAnimeList, filteredPausedAnimeList, filteredDroppedAnimeList);



        // Grid section headers
        HBox inProgressHeader = createGridHeader("In progress", filteredInProgressAnimeList);
        HBox backlogHeader = createGridHeader("Backlog", filteredBacklogAnimeList);
        HBox completedHeader = createGridHeader("Completed", filteredCompletedAnimeList);
        HBox pausedHeader = createGridHeader("Paused", filteredPausedAnimeList);
        HBox droppedHeader = createGridHeader("Dropped", filteredDroppedAnimeList);

        List<HBox> headers = new ArrayList<>();
        Collections.addAll(headers, inProgressHeader, backlogHeader, completedHeader, pausedHeader, droppedHeader);



        // Filtered FlowGridPanes that will be displayed
        FlowGridPane filteredInProgressGrid = createGrid("In progress", inProgressAnimeList);
        FlowGridPane filteredBacklogGrid = createGrid("Backlog", backlogAnimeList);
        FlowGridPane filteredCompletedGrid = createGrid("Completed", completedAnimeList);
        FlowGridPane filteredPausedGrid = createGrid("Paused", pausedAnimeList);
        FlowGridPane filteredDroppedGrid = createGrid("Dropped", droppedAnimeList);

        this.filteredGrids = new ArrayList<>();
        Collections.addAll(filteredGrids, filteredInProgressGrid, filteredBacklogGrid, filteredCompletedGrid, filteredPausedGrid, filteredDroppedGrid);

        // Wrapping scrollPane
        this.scrollPane = createScrollPane(headers, filteredGrids);


        // Collect grid loading futures
        List<CompletableFuture<Void>> gridFutures = new ArrayList<>();
        gridFutures.add(reloadAnimeGridAsync(grids.get(0), inProgressAnimeList));
        gridFutures.add(reloadAnimeGridAsync(grids.get(1), backlogAnimeList));
        gridFutures.add(reloadAnimeGridAsync(grids.get(2), completedAnimeList));
        gridFutures.add(reloadAnimeGridAsync(grids.get(3), pausedAnimeList));
        gridFutures.add(reloadAnimeGridAsync(grids.get(4), droppedAnimeList));
        CompletableFuture<Void> allGridsLoaded = CompletableFuture.allOf(
                gridFutures.toArray(new CompletableFuture[0])
        );

        allGridsLoaded.thenRun(() -> {
            Platform.runLater(() -> {
                // First call here also initializes the lazyLoader
                onFiltersChanged();
                this.listFinder = new ListFinder(grids);
            });
        });



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

            // Platform.runLater else the border starts as shown on scrollPane default position on full grid reload

            Platform.runLater(() -> {
                // This is so the controls-bottom-border can't start showing if the pane scroll bar is fully vertical (no scrolling possible)
                boolean canScroll = scrollPane.getContent().getBoundsInLocal().getHeight() > scrollPane.getViewportBounds().getHeight();

                if (newValue.doubleValue() > 0.00001 && canScroll) {
                    if (separator.getOpacity() == 0.0) {
                        fadeIn.playFromStart();
                    }
                } else {
                    if (separator.getOpacity() == 1.0) {
                        fadeOut.playFromStart();
                    }
                }
            });
        });


        root.getChildren().addAll(controls, separator, scrollPane);
        return stackPane;
    }


    private HBox createSearchAndFilterToggle(FlowGridPane filters) {
        // Wrapper
        HBox searchAndModeBox = new HBox();
        // searchAndModeBox.setStyle("-fx-spacing: 10;");
        searchAndModeBox.getStyleClass().add("search-bar-and-filter-toggle");

        // Search bar
        TextField searchBar = new TextField();
        searchBar.setId("search-bar");
        searchBar.setPromptText("Enter anime name..");
        HBox.setHgrow(searchBar, Priority.ALWAYS);

        // Regex to allow only english alphanumeric, JP / CN / KR characters, and spaces
        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.matches("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{Alnum} ]*")) {
                this.searchString = newValue;
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
                onFiltersChanged();
            });
        } else if (labelText.equals("Status")) {
            comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                releaseStatusFilter = newVal;
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
                onFiltersChanged();

            } else {
                textField.setText(oldValue);
            }
        });

        /*
        // Filter change listeners
        if (labelText.equals("Start year ≥")) {
            textField.textProperty().addListener((obs, oldVal, newVal) -> {
                startYearFilter
            });
        } else if (labelText.equals("End year ≤")) {
            textField.textProperty().addListener((obs, oldVal, newVal) -> {

            });
        }

         */

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


    private HBox createGridHeader(String labelText, ObservableList<VBox> animeList) {
        HBox headerBox = new HBox(5);
        headerBox.setPrefHeight(40);
        headerBox.setPadding(new Insets(0, 0, 10, 0));
        // headerBox.setMinHeight(0);
        // headerBox.setStyle("-fx-border-width: 1 0 1 0; -fx-border-color: red;");
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
        // leftRegion.prefWidthProperty().bind(headerBox.widthProperty().multiply(0.15));

        // TODO: Fix font weight. https://stackoverflow.com/a/77339072
        // Middle label
        Label label = new Label(labelText);
        label.getStyleClass().add("log-grid-header-text");

        // Right region
        Region rightRegion = new Region();
        rightRegion.getStyleClass().add("log-grid-header-separator");
        HBox.setHgrow(rightRegion, Priority.ALWAYS);

        // Add components to the HBox
        headerBox.getChildren().addAll(leftRegion, label, rightRegion);


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


    // TODO: Remove parameters
    private FlowGridPane createGrid(String labelText, ObservableList<AnimeInfo> animeList) {
        FlowGridPane animeGrid = new FlowGridPane(3, 1);  // Default values here shouldn't matter but are needed, so..
        animeGrid.setHgap(20);
        animeGrid.setVgap(20);
        animeGrid.setMaxWidth(Double.MAX_VALUE);
        animeGrid.setPadding(new Insets(0, 0, 30, 0));
        // animeGrid.setStyle("-fx-border-color: white; -fx-border-width: 1 0 1 0;");


        /*
        reloadAnimeGridAsync(animeGrid, animeList).join();

        int animesAmount = animeGrid.getChildren().size();
        int cols = 3;
        int rows = (int) Math.ceil((double) animesAmount / cols);
        animeGrid.setRowsCount(rows);

         */

        return animeGrid;
    }


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
            VBox animeBox = createAnimeBox(anime, stackPane);
            animeBoxes.add(animeBox);
        }
        return animeBoxes;
    }


    private VBox createAnimeBox(AnimeInfo anime, StackPane stackPane) {

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


        // Label with anime name to be shown on animeBox hover
        Label testLabel = new Label(anime.getTitle());
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
            createPopupScreen(animeBox, anime, stackPane);
        });


        // Initialize as non-visible so the scrollpane image loading listener updates it correctly
        animeBox.setVisible(false);

        return animeBox;
    }


    private void createPopupScreen(VBox parentBox, AnimeInfo anime, StackPane stackPane) {
        // Fake darkener effect
        VBox darkBackground = new VBox();
        darkBackground.getStyleClass().add("grid-media-popup-background");
        VBox.setVgrow(darkBackground, Priority.ALWAYS);
        HBox.setHgrow(darkBackground, Priority.ALWAYS);

        // The actual popup
        AnimePopupView animePopupView = new AnimePopupView(parentBox, this, anime, databaseRequestListener, darkBackground);
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

            VBox newAnimeBox = createAnimeBox(animeNew, stackPane);
            AnimeInfo newAnimeInfo = (AnimeInfo) newAnimeBox.getUserData();

            // Get correct grid to insert in
            List<VBox> targetGrid = switch (newAnimeInfo.getOwnStatus()) {
                case "In progress" -> grids.get(0);
                case "Backlog" -> grids.get(1);
                case "Completed" -> grids.get(2);
                case "Paused" -> grids.get(3);
                case "Dropped" -> grids.get(4);
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
            while (insertIndex < targetGrid.size()) {

                // Special case: New item passes itself still in the list
                if (insertIndex == oldIndex) {
                    insertIndex++;
                    hasPassedOldIndex = true;
                    continue;
                }

                VBox box = (VBox) targetGrid.get(insertIndex);
                AnimeInfo existingAnimeInfo = (AnimeInfo) box.getUserData();

                // Find correct position by rating first
                if (!sortedByRating) {
                    int comparison = byRating.compare(newAnimeInfo, existingAnimeInfo);
                    if (comparison <= 0) {
                        sortedByRating = true;
                        continue;
                    }
                    insertIndex++;
                }

                // Find correct position by title once sorted by rating already
                else {
                    int comparison = byTitle.compare(newAnimeInfo, existingAnimeInfo);
                    if (comparison <= 0) {
                        break;
                    }
                    insertIndex++;
                }

            }


            // If anime passed itself during sorting process, we need to decrement once because its old entry is about to be deleted
            if (hasPassedOldIndex) {
                insertIndex--;
            }

            // final int finalInsertIndex = insertIndex;


            // Remove old entry
            int idx = pair.getValue();
            pair.getKey().remove(idx);

            // Insert at new position
            targetGrid.add(insertIndex, newAnimeBox);
        }


        onFiltersChanged();
    }


    private void onFiltersChanged() {
        // Clear and filter items for all categories
        for (int i = 0; i < grids.size(); i++) {
            List<VBox> currentGrid = grids.get(i);
            ObservableList<VBox> filteredList = filteredAnimeLists.get(i);

            // Clear the filtered list for the current category
            filteredList.clear();

            // for (VBox animeBox : currentGrid.getChildren()) {
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
            for (int i = 0; i < filteredGrids.size(); i++) {

                FlowGridPane filteredGrid = filteredGrids.get(i);
                ObservableList<VBox> filteredList = filteredAnimeLists.get(i);

                filteredGrid.getChildren().clear();
                filteredGrid.getChildren().addAll(filteredList);

                if (filteredList.isEmpty()) {
                    filteredGrid.setMaxHeight(0);
                    filteredGrid.setPadding(new Insets(0, 0, 0, 0));
                } else {
                    filteredGrid.setMaxHeight(Double.MAX_VALUE);
                    filteredGrid.setPadding(new Insets(0, 0, 30, 0));
                }

                filteredGrid.setRowsCount((int) Math.ceil((double) filteredGrid.getChildren().size() / filteredGrid.getColsCount()));
            }



            if (scrollPane != null) {
                scrollPane.setVvalue(0);
                smoothScroll.resetAccumulatedVValue();
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


    private void setRatingBorder(VBox animeBox) {
        AnimeInfo anime = (AnimeInfo) animeBox.getUserData();

        animeBox.getStyleClass().removeAll(
                "grid-media-box-gold",
                "grid-media-box-green",
                "grid-media-box-red",
                "grid-media-box-grey"
        );

        if (anime.getOwnRating().equals("Heart")) {
            animeBox.getStyleClass().add("grid-media-box-gold");
        } else if (anime.getOwnRating().equals("Liked")) {
            animeBox.getStyleClass().add("grid-media-box-green");
        } else if (anime.getOwnRating().equals("Disliked")) {
            animeBox.getStyleClass().add("grid-media-box-red");
        } else {
            animeBox.getStyleClass().add("grid-media-box-grey");
        }
    }


    private ScrollPane createScrollPane(List<HBox> headers, List<FlowGridPane> grids) {
        VBox wrapper = new VBox(0);
        wrapper.setMaxWidth(Double.MAX_VALUE);

        for (int i = 0; i < headers.size(); i++) {
            wrapper.getChildren().add(headers.get(i));
            wrapper.getChildren().add(grids.get(i));
        }

        ScrollPane scrollPane = new ScrollPane(wrapper);
        scrollPane.getStyleClass().add("grid-scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

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

        return scrollPane;
    }


    public void shutdownLazyLoader() {
        lazyLoader.shutdownImageLoaderExecutor();
    }
}
