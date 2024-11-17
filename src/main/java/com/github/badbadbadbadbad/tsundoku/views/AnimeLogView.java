package com.github.badbadbadbadbad.tsundoku.views;

import com.github.badbadbadbadbad.tsundoku.controllers.DatabaseRequestListener;
import com.github.badbadbadbadbad.tsundoku.external.FlowGridPane;
import com.github.badbadbadbadbad.tsundoku.external.SmoothScroll;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import com.github.badbadbadbadbad.tsundoku.models.AnimeListInfo;
import com.github.badbadbadbadbad.tsundoku.util.LazyLoader;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AnimeLogView implements LazyLoaderView {

    private final double RATIO = 318.0 / 225.0; // The aspect ratio to use for anime images. This doesn't match all exactly, but is close enough.

    private final Stage stage;
    private final DatabaseRequestListener databaseRequestListener;

    private ScrollPane scrollPane;
    private StackPane stackPane;
    private LazyLoader lazyLoader;
    private SmoothScroll smoothScroll;

    private String displayMode = "Any";
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
        Comparator<AnimeInfo> byTitle = Comparator.comparing(AnimeInfo::getTitle);

        ObservableList<AnimeInfo> inProgressAnimeList = FXCollections.observableArrayList(
                fullDatabase.getAnimeList().stream()
                        .filter(anime -> "In progress".equals(anime.getOwnStatus()))
                        .sorted(byTitle)
                        .toList()
        );

        ObservableList<AnimeInfo> backlogAnimeList = FXCollections.observableArrayList(
                fullDatabase.getAnimeList().stream()
                        .filter(anime -> "Backlog".equals(anime.getOwnStatus()))
                        .sorted(byTitle)
                        .toList()
        );

        ObservableList<AnimeInfo> completedAnimeList = FXCollections.observableArrayList(
                fullDatabase.getAnimeList().stream()
                        .filter(anime -> "Completed".equals(anime.getOwnStatus()))
                        .sorted(byTitle)
                        .toList()
        );

        ObservableList<AnimeInfo> pausedAnimeList = FXCollections.observableArrayList(
                fullDatabase.getAnimeList().stream()
                        .filter(anime -> "Paused".equals(anime.getOwnStatus()))
                        .sorted(byTitle)
                        .toList()
        );

        ObservableList<AnimeInfo> droppedAnimeList = FXCollections.observableArrayList(
                fullDatabase.getAnimeList().stream()
                        .filter(anime -> "Dropped".equals(anime.getOwnStatus()))
                        .sorted(byTitle)
                        .toList()
        );


        // Grid section headers
        HBox inProgressHeader = createGridHeader("In progress", inProgressAnimeList);
        HBox backlogHeader = createGridHeader("Backlog", backlogAnimeList);
        HBox completedHeader = createGridHeader("Completed", completedAnimeList);
        HBox pausedHeader = createGridHeader("Paused", pausedAnimeList);
        HBox droppedHeader = createGridHeader("Dropped", droppedAnimeList);

        List<HBox> headers = new ArrayList<>();
        Collections.addAll(headers, inProgressHeader, backlogHeader, completedHeader, pausedHeader, droppedHeader);

        // Grids
        FlowGridPane inProgressGrid = createGrid("In progress", inProgressAnimeList);
        FlowGridPane backlogGrid = createGrid("Backlog", backlogAnimeList);
        FlowGridPane completedGrid = createGrid("Completed", completedAnimeList);
        FlowGridPane pausedGrid = createGrid("Paused", pausedAnimeList);
        FlowGridPane droppedGrid = createGrid("Dropped", droppedAnimeList);

        List<FlowGridPane> grids = new ArrayList<>();
        Collections.addAll(grids, inProgressGrid, backlogGrid, completedGrid, pausedGrid, droppedGrid);

        // Wrapping scrollPane
        ScrollPane scrollPane = createScrollPane(headers, grids);


        // Collect grid loading futures
        List<CompletableFuture<Void>> gridFutures = new ArrayList<>();
        gridFutures.add(reloadAnimeGridAsync(inProgressGrid, inProgressAnimeList));
        gridFutures.add(reloadAnimeGridAsync(backlogGrid, backlogAnimeList));
        gridFutures.add(reloadAnimeGridAsync(completedGrid, completedAnimeList));
        gridFutures.add(reloadAnimeGridAsync(pausedGrid, pausedAnimeList));
        gridFutures.add(reloadAnimeGridAsync(droppedGrid, droppedAnimeList));
        CompletableFuture<Void> allGridsLoaded = CompletableFuture.allOf(
                gridFutures.toArray(new CompletableFuture[0])
        );

        allGridsLoaded.thenRun(() -> {
            Platform.runLater(() -> {
                lazyLoader = new LazyLoader(scrollPane, grids);
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




        // root.getChildren().addAll(controls, separator, scrollPane);
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
                // searchString = newValue;
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
                displayMode = newVal;
            });
        } else if (labelText.equals("Status")) {
            comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {

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
            if (!newValue.matches("\\d{0,4}")) {
                textField.setText(oldValue);
            }
        });

        // Filter change listeners
        if (labelText.equals("Start year ≥")) {
            textField.textProperty().addListener((obs, oldVal, newVal) -> {

            });
        } else if (labelText.equals("End year ≤")) {
            textField.textProperty().addListener((obs, oldVal, newVal) -> {

            });
        }

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


    private HBox createGridHeader(String labelText, ObservableList<AnimeInfo> animeList) {
        HBox headerBox = new HBox(5);
        headerBox.setPrefHeight(40);
        // headerBox.setStyle("-fx-border-width: 1 0 1 0; -fx-border-color: white;");
        HBox.setHgrow(headerBox, Priority.ALWAYS);
        headerBox.setAlignment(Pos.CENTER);

        if (animeList.isEmpty()) {
            headerBox.setManaged(false);
            headerBox.setVisible(false);
        }


        // Left region
        Region leftRegion = new Region();
        leftRegion.getStyleClass().add("log-grid-header-separator");
        leftRegion.prefWidthProperty().bind(headerBox.widthProperty().multiply(0.15));

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


        animeList.addListener((ListChangeListener<AnimeInfo>) change -> {
            boolean hasItems = animeList.size() > 0;
            boolean shouldDisplay = displayMode.equals("Any") || displayMode.equals(labelText);

            headerBox.setManaged(hasItems && shouldDisplay);
            headerBox.setVisible(hasItems && shouldDisplay);
        });

        return headerBox;
    }


    private FlowGridPane createGrid(String labelText, ObservableList<AnimeInfo> animeList) {
        FlowGridPane animeGrid = new FlowGridPane(3, 1);  // Default values here shouldn't matter but are needed, so..
        animeGrid.setHgap(20);
        animeGrid.setVgap(20);
        animeGrid.setMaxWidth(Double.MAX_VALUE);
        animeGrid.setStyle("-fx-padding: 0 0 20 0;");

        /*
        reloadAnimeGridAsync(animeGrid, animeList).join();

        int animesAmount = animeGrid.getChildren().size();
        int cols = 3;
        int rows = (int) Math.ceil((double) animesAmount / cols);
        animeGrid.setRowsCount(rows);

         */

        return animeGrid;
    }


    private CompletableFuture<Void> reloadAnimeGridAsync(FlowGridPane animeGrid, List<AnimeInfo> animeList) {
        return CompletableFuture.supplyAsync(() -> createAnimeGridItems(animeList))
                .thenAccept(animeBoxes -> {
                    Platform.runLater(() -> {
                        animeGrid.getChildren().clear();
                        animeGrid.getChildren().addAll(animeBoxes);

                        // Testing for speedy image caching?
                        /*
                        for (Node child: animeGrid.getChildren()) {
                            child.setCache(true);
                            child.setCacheHint(CacheHint.SPEED);
                        }

                         */

                        // Update the internal rows count of grid after children were updated
                        animeGrid.setRowsCount((int) Math.ceil((double) animeGrid.getChildren().size() / animeGrid.getColsCount()));
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
            // createPopupScreen(anime, stackPane);
        });


        // Initialize as non-visible so the scrollpane image loading listener updates it correctly
        animeBox.setVisible(false);

        return animeBox;
    }


    private void setRatingBorder(VBox animeBox) {
        AnimeInfo anime = (AnimeInfo) animeBox.getUserData();

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
        VBox wrapper = new VBox(10);
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
