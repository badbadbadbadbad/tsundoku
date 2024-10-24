package com.github.badbadbadbadbad.tsundoku.views;


import com.github.badbadbadbadbad.tsundoku.controllers.APIController;
import com.github.badbadbadbadbad.tsundoku.controllers.APIRequestListener;
import com.github.badbadbadbadbad.tsundoku.controllers.GridFilterListener;
import com.github.badbadbadbadbad.tsundoku.controllers.LoadingBarListener;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import com.github.badbadbadbadbad.tsundoku.external.FlowGridPane;
import com.github.badbadbadbadbad.tsundoku.external.SmoothScroll;
import com.github.badbadbadbadbad.tsundoku.models.AnimeListInfo;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public class AnimeGridView {

    private final double RATIO = 318.0 / 225.0; // The aspect ratio to use for anime images. Close to most cover images.
    private final List<GridFilterListener> gridFilterListeners = new ArrayList<>();
    private APIRequestListener apiRequestListener = null; // TODO Change when ViewsController is implemented
    private LoadingBarListener loadingBarListener = null;
    private final APIController apiController;

    AnimeListInfo animeListInfo;
    private FlowGridPane animeGrid;
    private HBox pagination;
    private StackPane stackPane;

    private String searchMode = "SEASON";  // Changes between SEASON, TOP, and SEARCH depending on last mode selected (so pagination calls "current mode")
    private String searchString = "";
    private static boolean filtersHidden = false;
    private boolean apiLock = false;

    public AnimeGridView(APIController apiController) {
        this.apiController = apiController;
    }

    public Region createGridView(Stage stage) {
        VBox root = new VBox();
        VBox.setVgrow(root, Priority.ALWAYS);
        HBox.setHgrow(root, Priority.ALWAYS);


        // StackPane wrapper to allow for popup functionality when grid element is clicked
        stackPane = new StackPane();
        VBox.setVgrow(stackPane, Priority.ALWAYS);
        HBox.setHgrow(stackPane, Priority.ALWAYS);
        stackPane.getChildren().add(root);


        FlowGridPane filters = createFilters(stage);
        HBox buttonBox = createButtons();
        HBox searchAndFilterToggleBox = createSearchAndFilterToggle(filters);

        // TODO Change when viewsController is implemented
        setAPIRequestListener(apiController);

        // Blocking call on CompletableFuture for first setup
        // Probably change later when functionality for media type tab switching is in
        animeListInfo = apiRequestListener.getCurrentAnimeSeason(1).join();
        // animeListInfo = apiController.getCurrentAnimeSeason(1).join();
        ScrollPane animeGrid = createBrowseGrid(stage, animeListInfo);


        VBox controls = new VBox();
        controls.getStyleClass().add("content-pane-controls");
        controls.setMinHeight(Control.USE_PREF_SIZE);


        controls.getChildren().addAll(searchAndFilterToggleBox, filters, buttonBox);
        root.getChildren().addAll(controls, animeGrid);


        return stackPane;
    }


    private HBox createSearchAndFilterToggle(FlowGridPane filters) {
        HBox searchAndModeBox = new HBox();
        searchAndModeBox.setSpacing(10);

        // Search bar
        TextField searchBar = new TextField();
        searchBar.setId("search-bar");
        searchBar.setPromptText("Enter query..");
        HBox.setHgrow(searchBar, Priority.ALWAYS);

        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            searchString = newValue;
        });

        // Search should trigger on enter press
        searchBar.setOnAction(event -> {
            if(!apiLock) {
                searchMode = "SEARCH";
                apiLock = true;

                invokeAnimatedAPICall(1);
            }
        });

        // Filter toggle button
        ToggleButton toggleFiltersButton = new ToggleButton("Hide filters");
        toggleFiltersButton.getStyleClass().add("controls-button");

        // Filter toggle logic
        toggleFiltersButton.setOnAction(e -> {
            toggleFiltersButton.setDisable(true);

            if (toggleFiltersButton.isSelected()) {
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


    private FlowGridPane createFilters(Stage stage) {
        FlowGridPane filtersGrid = new FlowGridPane(2, 3);
        filtersGrid.setHgap(10);
        filtersGrid.setVgap(10);
        filtersGrid.setMaxWidth(Double.MAX_VALUE);
        filtersGrid.setMinHeight(0); // Necessary for fade animation


        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();

        // Filters
        VBox orderByFilter = createDropdownFilter("Order by", new String[]{
                "Title: Ascending", "Title: Descending",
                "Rating: Highest", "Rating: Lowest",
                "Popular: Most", "Popular: Least"}, "Popular: Most");

        VBox startYearFilter = createNumberFilter("Start year");
        VBox endYearFilter = createNumberFilter("End year");

        VBox statusFilter = createDropdownFilter("Status",
                new String[]{"Any", "Complete", "Airing", "Upcoming"}, "Any");



        filtersGrid.getChildren().addAll(orderByFilter, statusFilter, startYearFilter, endYearFilter);



        // Dynamically adjust column amount based on window size
        int filtersAmount = filtersGrid.getChildren().size();
        stage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double windowWidth = newWidth.doubleValue();
            int cols, rows;

            if (windowWidth < screenWidth * 0.625) {
                cols = 2;  // Minimum 2 columns
            } else if (windowWidth < screenWidth * 0.75) {
                cols = 3;
            } else {
                cols = 4;
            }

            rows = (int) Math.ceil((double) filtersAmount / cols); // Need an int value, but need float division, hence ugly casting..

            filtersGrid.setColsCount(cols);
            filtersGrid.setRowsCount(rows);

            // Necessary for the fade animation to work
            if (!filtersHidden) {
                filtersGrid.setMaxHeight(filtersGrid.prefHeight(filtersGrid.getWidth()));
            }
        });

        // Necessary else Windows doesn't do the height on startup properly
        // Thanks, Microsoft
        Platform.runLater(() -> {
            filtersGrid.setMaxHeight(filtersGrid.prefHeight(filtersGrid.getWidth()));
        });

        return filtersGrid;
    }


    private VBox createDropdownFilter(String labelText, String[] options, String defaultValue) {
        Label label = new Label(labelText);
        label.getStyleClass().add("filter-label");

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(options);
        comboBox.setValue(defaultValue);
        comboBox.getStyleClass().add("filter-box");
        comboBox.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(comboBox, Priority.ALWAYS);

        // Filter change listeners
        if (labelText.equals("Order by")) {
            comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                for (GridFilterListener listener: gridFilterListeners) {
                    listener.onAnimeOrderByChanged(newVal);
                }
            });
        } else if (labelText.equals("Status")) {
            comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                for (GridFilterListener listener: gridFilterListeners) {
                    listener.onAnimeStatusChanged(newVal);
                }
            });
        }

        return new VBox(5, label, comboBox);
    }


    private VBox createNumberFilter(String labelText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("filter-label");

        TextField textField = new TextField("");
        textField.getStyleClass().add("filter-box");
        textField.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(textField, Priority.ALWAYS);

        // Numeric input and at most four digits regex filter
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d{0,4}")) {
                textField.setText(oldValue);
            }
        });

        // Filter change listeners
        if (labelText.equals("Start year")) {
            textField.textProperty().addListener((obs, oldVal, newVal) -> {
                for (GridFilterListener listener: gridFilterListeners) {
                    listener.onAnimeStartYearChanged(newVal);
                }
            });
        } else if (labelText.equals("End year")) {
            textField.textProperty().addListener((obs, oldVal, newVal) -> {
                for (GridFilterListener listener: gridFilterListeners) {
                    listener.onAnimeEndYearChanged(newVal);
                }
            });
        }

        return new VBox(5, label, textField);
    }


    private void hideFilters(FlowGridPane filters, ToggleButton button) {

        filtersHidden = true;

        // Fade animation
        FadeTransition fade = new FadeTransition(Duration.millis(150), filters);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        // Move animation
        KeyFrame visible = new KeyFrame(Duration.ZERO, new KeyValue(filters.maxHeightProperty(), filters.getHeight()));
        KeyFrame hidden = new KeyFrame(Duration.millis(100), new KeyValue(filters.maxHeightProperty(), 0));
        Timeline move = new Timeline(visible, hidden);

        // Cooldown
        PauseTransition cooldown = new PauseTransition(Duration.millis(200));
        cooldown.setOnFinished(event -> button.setDisable(false));

        // Gather animations
        SequentialTransition st = new SequentialTransition(fade, move);
        ParallelTransition pt = new ParallelTransition(cooldown, st);

        pt.play();
    }


    private void showFilters(FlowGridPane filters, ToggleButton button) {

        filtersHidden = false;

        // Fade animation
        FadeTransition fade = new FadeTransition(Duration.millis(150), filters);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        // Move animation
        KeyFrame hidden = new KeyFrame(Duration.ZERO, new KeyValue(filters.maxHeightProperty(), 0));
        KeyFrame visible = new KeyFrame(Duration.millis(100), new KeyValue(filters.maxHeightProperty(), filters.prefHeight(filters.getWidth())));
        Timeline move = new Timeline(hidden, visible);

        // Cooldown
        PauseTransition cooldown = new PauseTransition(Duration.millis(200));
        cooldown.setOnFinished(event -> button.setDisable(false));

        // Gather animations
        SequentialTransition st = new SequentialTransition(move, fade);
        ParallelTransition pt = new ParallelTransition(cooldown, st);

        pt.play();
    }


    private HBox createButtons() {
        Button seasonButton = new Button("Season");
        Button topButton = new Button("Top");
        Button searchButton = new Button("Search");

        seasonButton.getStyleClass().add("controls-button");
        topButton.getStyleClass().add("controls-button");
        searchButton.getStyleClass().add("controls-button");

        // Listeners to call for content
        seasonButton.setOnAction(event -> {
            if(!apiLock) {
                searchMode = "SEASON";
                apiLock = true;

                invokeAnimatedAPICall(1);
            }
        });

        topButton.setOnAction(event -> {
            if(!apiLock) {
                searchMode = "TOP";
                apiLock = true;

                invokeAnimatedAPICall(1);
            }
        });

        searchButton.setOnAction(event -> {
            if(!apiLock) {
                searchMode = "SEARCH";
                apiLock = true;

                invokeAnimatedAPICall(1);
            }
        });


        HBox leftButtons = new HBox(10, seasonButton, topButton);
        HBox rightButtons = new HBox(searchButton);
        HBox.setHgrow(leftButtons, Priority.ALWAYS);
        HBox.setHgrow(rightButtons, Priority.ALWAYS);
        leftButtons.setAlignment(Pos.CENTER_LEFT);
        rightButtons.setAlignment(Pos.CENTER_RIGHT);

        HBox buttonBox = new HBox(10, leftButtons, rightButtons);
        HBox.setHgrow(buttonBox, Priority.ALWAYS);
        buttonBox.setAlignment(Pos.CENTER);

        return buttonBox;
    }


    private ScrollPane createBrowseGrid(Stage stage, AnimeListInfo animeListInfo) {

        animeGrid = new FlowGridPane(2, 3);
        animeGrid.setHgap(20);
        animeGrid.setVgap(20);
        animeGrid.setMaxWidth(Double.MAX_VALUE);

        reloadAnimeGrid(animeListInfo.getAnimeList());

        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();

        int animesAmount = animeGrid.getChildren().size();
        stage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double windowWidth = newWidth.doubleValue();

            int cols, rows;

            if (windowWidth < screenWidth * 0.6) {
                cols = 3;
            } else if (windowWidth < screenWidth * 0.7) {
                cols = 4;
            } else if (windowWidth < screenWidth * 0.8) {
                cols = 5;
            } else if (windowWidth < screenWidth * 0.9) {
                cols = 6;
            } else {
                cols = 7;
            }

            rows = (int) Math.ceil((double) animesAmount / cols);

            animeGrid.setColsCount(cols);
            animeGrid.setRowsCount(rows);
        });



        pagination = createPagination(animeListInfo.getLastPage());
        VBox wrapper = new VBox(10, animeGrid, pagination);
        wrapper.setMaxWidth(Double.MAX_VALUE);


        ScrollPane scrollPane = new ScrollPane(wrapper);
        scrollPane.getStyleClass().add("grid-scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        // VBox.setVgrow(scrollPane, Priority.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Smooth scroll listener
        // in /util/, SmoothScroll
        new SmoothScroll(scrollPane, wrapper);

        return scrollPane;
    }


    private HBox createPagination(int pages) {
        int selectedPage = 1;

        HBox pagination = new HBox();
        pagination.setMinHeight(50);
        pagination.setMaxHeight(50);
        pagination.setStyle("-fx-padding: 0 0 10px 0;");
        pagination.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pagination, Priority.ALWAYS);
        pagination.setAlignment(Pos.CENTER);

        HBox paginationButtons = new HBox(10);
        paginationButtons.setMinHeight(40);
        paginationButtons.setMaxHeight(40);

        pagination.getChildren().add(paginationButtons);
        updatePaginationButtons(paginationButtons, selectedPage, pages);
        return pagination;
    }


    private void updatePaginationButtons(HBox paginationButtons, int selectedPage, int pages) {
        paginationButtons.getChildren().clear();

        // Only need "first page" button if it's not already the selected one
        if (selectedPage > 2) {
            paginationButtons.getChildren().add(createPageButton(1, pages));
        }

        // Low numbers ellipsis button
        if (selectedPage > 3) {
            paginationButtons.getChildren().add(createEllipsisButton(paginationButtons, pages));
        }

        // Selected page as well as its prev and next
        for (int i = Math.max(1, selectedPage - 1); i <= Math.min(pages, selectedPage + 1); i++) {
            Button butt = createPageButton(i, pages);
            if (i == selectedPage) {
                butt.getStyleClass().add("pagination-button-active");
            }
            paginationButtons.getChildren().add(butt);

        }

        // High numbers ellipsis button
        if (selectedPage < pages - 2) {
            paginationButtons.getChildren().add(createEllipsisButton(paginationButtons, pages));
        }

        // Only need "last page" button if it's not already the selected one
        if (selectedPage < pages - 1) {
            paginationButtons.getChildren().add(createPageButton(pages, pages));
        }
    }


    private void invokeAnimatedAPICall(int page) {
        // Begin load animation
        VBox darkBackground = createSearchBackground(stackPane);
        FadeTransition fadeInBackground = createFadeInTransition(darkBackground, 0.2, 0.8);
        fadeInBackground.play();

        loadingBarListener.animateLoadingBar(0, 50, 0.2);

        // API call
        getPageForCurrentQuery(page).thenAccept(info -> {

            // Middle load animation
            loadingBarListener.animateLoadingBar(50, 80, 0.2);

            // Update grid in the background
            reloadAnimeGridAsync(info.getAnimeList());

            Platform.runLater(() -> {
                // Update pagination. Maybe move later? Needs testing
                updatePaginationButtons(pagination, page, info.getLastPage());

                // Inner runLater for animation end after everything is loaded. Needs testing if inner runLater is needed.
                Platform.runLater(() -> {
                    PauseTransition pause = new PauseTransition(Duration.seconds(0.1));
                    pause.setOnFinished(ev -> {


                        loadingBarListener.animateLoadingBar(80, 100, 0.1);

                        FadeTransition fadeOutBackground = createFadeOutTransition(darkBackground, 0.3, 0.8);
                        fadeOutBackground.play();

                        loadingBarListener.fadeOutLoadingBar(0.3);
                        PauseTransition loadingBarFadeOutTimer = new PauseTransition(Duration.seconds(0.3));
                        loadingBarFadeOutTimer.setOnFinished(e -> apiLock = false);
                        loadingBarFadeOutTimer.play();
                    });
                    pause.play();
                });
            });

        });
    }


    private CompletableFuture<AnimeListInfo> getPageForCurrentQuery(int page) {
        if (searchMode.equals("SEASON")) {
            return apiRequestListener.getCurrentAnimeSeason(page);
        } else if (searchMode.equals("TOP")) {
            return apiRequestListener.getTopAnime(page);
        } else { // Default mode: SEARCH
            return apiRequestListener.getAnimeSearch(searchString, page);
        }
    }

    // TODO OLD VERSION, REMOVE OR MAKE BLOCKING FOR INITIAL CALL
    private void reloadAnimeGrid(List<AnimeInfo> animeList) {

        CompletableFuture.supplyAsync(() -> {
            List<VBox> animeBoxes = new ArrayList<>();
            for (AnimeInfo anime : animeList) {
                animeBoxes.add(createAnimeBox(anime, stackPane));
            }
            return animeBoxes;
        }).thenAccept(animeBoxes -> {
            Platform.runLater(() -> {
                animeGrid.getChildren().clear();
                animeGrid.getChildren().addAll(animeBoxes);
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


    private void reloadAnimeGridAsync(List<AnimeInfo> animeList) {
        CompletableFuture.supplyAsync(() -> createAnimeGridItems(animeList))
                .thenAccept(animeBoxes -> {
                    Platform.runLater(() -> {
                        animeGrid.getChildren().clear();
                        animeGrid.getChildren().addAll(animeBoxes);
                        adjustGridItemHeights(); // Adjust the heights after adding to the scene graph
                    });
                });
    }


    private void adjustGridItemHeights() {
        for (Node node : animeGrid.getChildren()) {
            if (node instanceof VBox animeBox) {
                double width = animeBox.getWidth();
                double newHeight = width * RATIO;
                animeBox.setMinHeight(newHeight);
                animeBox.setPrefHeight(newHeight);
                animeBox.setMaxHeight(newHeight);
            }
        }
    }


    private Button createPageButton(int page, int pages) {
        Button pageButton =  new Button(String.valueOf(page));
        pageButton.getStyleClass().add("pagination-button");

        pageButton.setOnAction(event -> {
            if(!apiLock) {
                apiLock = true;
                invokeAnimatedAPICall(page);
            }
        });

        return pageButton;
    }


    private Button createEllipsisButton(HBox paginationButtons, int pages) {
        Button ellipsisButton =  new Button("...");
        ellipsisButton.getStyleClass().add("pagination-button");

        ellipsisButton.setOnAction(event -> {
            TextField pageInputField = createPageInputField(paginationButtons, pages);
            int index = paginationButtons.getChildren().indexOf(ellipsisButton);
            paginationButtons.getChildren().set(index, pageInputField);
            pageInputField.requestFocus();
        });

        return ellipsisButton;
    }


    private TextField createPageInputField(HBox paginationButtons, int pages) {
        TextField pageInputField = new TextField();
        pageInputField.setMinWidth(60);
        pageInputField.setMaxWidth(60);
        pageInputField.setMinHeight(40);
        pageInputField.setMaxHeight(40);
        pageInputField.setStyle("-fx-font-size: 16px;");

        // Numbers only regex
        pageInputField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                pageInputField.setText(newValue.replaceAll("\\D", ""));
            }
        });

        // Focus lost
        pageInputField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                handlePageInput(paginationButtons, pageInputField, pages);
            }
        });

        // Enter pressed (default value for setOnAction for textFields)
        pageInputField.setOnAction(event -> handlePageInput(paginationButtons, pageInputField, pages));

        return pageInputField;
    }


    private void handlePageInput(HBox paginationButtons, TextField pageInputField, int pages) {
        String input = pageInputField.getText();
        int index = paginationButtons.getChildren().indexOf(pageInputField);

        if (index == -1) {
            return;
        }

        if (input.isEmpty()) {
            paginationButtons.getChildren().set(index, createEllipsisButton(paginationButtons, pages));
        } else {
            try {
                int clampedPage = Math.clamp(Integer.parseInt(input), 1, pages);

                if(!apiLock) {
                    apiLock = true;
                    invokeAnimatedAPICall(clampedPage);
                }

            } catch (NumberFormatException e) {
                // Number formatting issues _shouldn't_ exist, but provide failsafe anyway
                paginationButtons.getChildren().set(index, createEllipsisButton(paginationButtons, pages));
            }

        }
    }


    private VBox createAnimeBox(AnimeInfo anime, StackPane stackPane) {

        // Make image into VBox background, CSS cover sizing to look okay
        VBox animeBox = new VBox();
        animeBox.setAlignment(Pos.CENTER);
        animeBox.setStyle("-fx-background-image: url('" + anime.getImageUrl() + "');");
        animeBox.getStyleClass().add("grid-media-box");

        // Clipping rectangle because JavaFX doesn't have any kind of background image clipping. WHY??
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(animeBox.widthProperty());
        clip.heightProperty().bind(animeBox.heightProperty());
        clip.setArcHeight(40);
        clip.setArcWidth(40);
        animeBox.setClip(clip);

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
            createPopupScreen(anime, stackPane);
        });

        return animeBox;
    }


    // TODO: Clean up code, reuse animations
    private void createPopupScreen(AnimeInfo anime, StackPane stackPane) {
        // Fake darkener effect
        VBox darkBackground = new VBox();
        darkBackground.getStyleClass().add("grid-media-popup-background");
        VBox.setVgrow(darkBackground, Priority.ALWAYS);
        HBox.setHgrow(darkBackground, Priority.ALWAYS);

        // The actual popup
        AnimePopupView animePopupView = new AnimePopupView();
        VBox popupBox = animePopupView.createPopup(anime);

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


    private VBox createSearchBackground(StackPane parent) {
        VBox darkBackground = new VBox();
        darkBackground.getStyleClass().add("grid-media-popup-background");
        VBox.setVgrow(darkBackground, Priority.ALWAYS);
        HBox.setHgrow(darkBackground, Priority.ALWAYS);
        darkBackground.setOpacity(0);
        parent.getChildren().add(darkBackground);
        return darkBackground;
    }


    private FadeTransition createFadeInTransition(Node node, double durationSeconds, double toValue) {
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(durationSeconds), node);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(toValue);
        return fadeIn;
    }


    private FadeTransition createFadeOutTransition(Node node, double durationSeconds, double fromValue) {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(durationSeconds), node);
        fadeOut.setFromValue(fromValue);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> stackPane.getChildren().remove(node));
        return fadeOut;
    }


    public void addGridFilterListener(GridFilterListener listener) {
        gridFilterListeners.add(listener);
    }

    public void removeGridFilterListener(GridFilterListener listener) {
        gridFilterListeners.remove(listener);
    }

    public void setAPIRequestListener(APIRequestListener listener) {
        // apiRequestListeners.add(listener);
        this.apiRequestListener = listener;
    }

    public void setLoadingBarListener(LoadingBarListener listener) {
        this.loadingBarListener = listener;
    }
}
