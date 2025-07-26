package com.github.badbadbadbadbad.tsundoku.views;


import com.github.badbadbadbadbad.tsundoku.controllers.APIRequestListener;
import com.github.badbadbadbadbad.tsundoku.controllers.GridFilterListener;
import com.github.badbadbadbadbad.tsundoku.controllers.LoadingBarListener;
import com.github.badbadbadbadbad.tsundoku.controllers.DatabaseRequestListener;
import com.github.badbadbadbadbad.tsundoku.external.FlowGapPane;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import com.github.badbadbadbadbad.tsundoku.models.AnimeListInfo;
import com.github.badbadbadbadbad.tsundoku.external.FlowGridPane;
import com.github.badbadbadbadbad.tsundoku.external.SmoothScroll;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;


/**
 * The full view component displayed in the main content pane for browse mode "Browse" and media mode "Anime".
 *
 * <p>It would be far cleaner to have some BrowseView superclass with this inheriting, but I don't want to overcomplicate things
 * before I know what quirks the main content views for other media modes may involve (due to relying on data from external APIs).</p>
 */
public class AnimeBrowseView implements PopupMakerView {

    /**
     * Aspect ratio we use for anime images.
     * This matches most of them well enough (there's no perfect standard used for anime covers).
     */
    private final double RATIO = 318.0 / 225.0;

    private final Stage stage;
    private final GridFilterListener gridFilterListener;
    private final APIRequestListener apiRequestListener;
    private final LoadingBarListener loadingBarListener;
    private final DatabaseRequestListener databaseRequestListener;

    private ScrollPane scrollPane;
    private FlowGapPane animeGrid;
    private Pagination pagination;
    private StackPane stackPane;
    private SmoothScroll smoothScroll;


    private final Map<String, Consumer<String>> filterUpdaters = new HashMap<>();


    private String searchMode = "SEASON";  // Changes between SEASON, TOP, and SEARCH depending on last mode selected (so pagination calls "current mode")
    private String searchString = "";

    private ChangeListener<Number> filtersWidthListener;
    private final BooleanProperty filtersHidden = new SimpleBooleanProperty(true);
    private boolean apiLock = false;
    private String languagePreference;


    public AnimeBrowseView(Stage stage, LoadingBarListener loadingBarListener, APIRequestListener apiRequestListener,
                           GridFilterListener gridFilterListener, DatabaseRequestListener databaseRequestListener, String languagePreference) {
        this.stage = stage;
        this.loadingBarListener = loadingBarListener;
        this.apiRequestListener = apiRequestListener;
        this.gridFilterListener = gridFilterListener;
        this.databaseRequestListener = databaseRequestListener;
        this.languagePreference = languagePreference;

        // Filters must know which internal variable to update with the chosen setting
        this.filterUpdaters.put("Order by", gridFilterListener::onAnimeOrderByChanged);
        this.filterUpdaters.put("Release status", gridFilterListener::onAnimeStatusChanged);
        this.filterUpdaters.put("Year ≥", gridFilterListener::onAnimeStartYearChanged);
        this.filterUpdaters.put("Year ≤", gridFilterListener::onAnimeEndYearChanged);
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
        HBox buttonBox = createButtons();
        HBox searchAndFilterToggleBox = createSearchAndFilterToggle(filters);

        VBox controls = new VBox();
        controls.getStyleClass().add("content-pane-controls");
        controls.setMinHeight(Control.USE_PREF_SIZE);

        controls.getChildren().addAll(searchAndFilterToggleBox, filters, buttonBox);


        // Blocking call on CompletableFuture for first setup to ensure data is there for display
        // Together with loading bar animation (not working as well here, could expand later)
        loadingBarListener.animateLoadingBar(50, 0.1);
        AnimeListInfo animeListInfo = apiRequestListener.getCurrentAnimeSeason(1).join();
        this.scrollPane = createBrowseGrid(animeListInfo);
        apiLock = true;

        PauseTransition pause = new PauseTransition(Duration.seconds(0.1));
        pause.setOnFinished(ev -> {
            loadingBarListener.animateLoadingBar(100, 0.1);
            loadingBarListener.fadeOutLoadingBar(0.3);
            PauseTransition loadingBarFadeOutTimer = new PauseTransition(Duration.seconds(0.3));
            loadingBarFadeOutTimer.setOnFinished(e -> apiLock = false);
            loadingBarFadeOutTimer.play();
        });
        pause.play();


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

        // Regex to allow only english alphanumeric, JP / CN / KR characters, and spaces
        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {

            searchString = newValue;
        });

        // Search should trigger on enter press (but not when search empty to avoid unnecessary API calls)
        searchBar.setOnAction(event -> {
            if(!apiLock && !searchString.isEmpty()) {
                searchMode = "SEARCH";
                apiLock = true;

                invokeAnimatedAPICall(1);
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
        FlowGridPane filtersGrid = new FlowGridPane(3, 1);
        filtersGrid.setHgap(10);
        filtersGrid.setVgap(10);
        filtersGrid.setMaxWidth(Double.MAX_VALUE);
        filtersGrid.setMinHeight(0); // Necessary for fade animation

        // We initialize with filters hidden
        filtersGrid.setMaxHeight(0);
        filtersGrid.setOpacity(0.0);

        // Filters
        VBox orderByFilter = createDropdownFilter("Order by", new String[]{
                "Default",
                "Title: Ascending", "Title: Descending",
                "Rating: Highest", "Rating: Lowest",
                "Popular: Most", "Popular: Least"}, "Default");

        VBox statusFilter = createDropdownFilter("Release status",
                new String[]{"Any", "Complete", "Airing", "Upcoming"}, "Any");

        HBox yearFilter = createDoubleNumberFilter("Year ≥", "Year ≤");

        filtersGrid.getChildren().addAll(orderByFilter, statusFilter, yearFilter);


        // Dynamic resizing not needed with current filter amount
        /*
        // Dynamically adjust column amount based on window size
        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();
        int filtersAmount = filtersGrid.getChildren().size();

        this.filtersWidthListener = (obs, oldWidth, newWidth) -> {
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
        };
        stage.widthProperty().addListener(filtersWidthListener);
        filtersWidthListener.changed(stage.widthProperty(), stage.getWidth(), stage.getWidth()); // Activate once immediately

         */


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
        if (this.filterUpdaters.containsKey(labelText)) {
            comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                filterUpdaters.get(labelText).accept(newVal);
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


        // Filter change listeners
        Consumer<String> updater = filterUpdaters.get(labelText);
        if (updater != null) {
            textField.textProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue.matches("\\d{0,4}")) {
                    updater.accept(newValue);

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


        // Listener to fire searches on enter press.
        // Feels more natural when it fires not only when in the search bar, but also these year fields.
        textField.setOnAction(event -> {
            if(!apiLock && !searchString.isEmpty()) {
                searchMode = "SEARCH";
                apiLock = true;

                invokeAnimatedAPICall(1);
            }
        });


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
            updateVisibleGridItems(scrollPane);
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
            updateVisibleGridItems(scrollPane);
        });

        // Filters are a bit squished after clicking "show filters" button until a resize if this is not done
        test.setOnFinished(e -> {
            filters.setMaxHeight(filters.prefHeight(filters.getWidth()));
        });

        pt.play();
    }


    /**
     * First part of the controls component above the grid, the buttons invoking API calls.
     * @return The finished component
     */
    private HBox createButtons() {
        Button seasonButton = new Button("Season");
        Button upcomingButton = new Button("Upcoming");
        Button topButton = new Button("Top");
        Button searchButton = new Button("Search");

        seasonButton.getStyleClass().add("controls-button");
        upcomingButton.getStyleClass().add("controls-button");
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

        upcomingButton.setOnAction(event -> {
            if(!apiLock) {
                searchMode = "UPCOMING";
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
            if(!apiLock && !searchString.isEmpty()) {
                searchMode = "SEARCH";
                apiLock = true;

                invokeAnimatedAPICall(1);
            }
        });


        HBox leftButtons = new HBox(10, seasonButton, upcomingButton, topButton);
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


    /**
     * Creates the FlowPane of anime, wrapped by a scrollPane.
     * Does not actually create the child elements themselves, that is done in an async sub-function.
     * @param animeListInfo A List of AnimeInfo (to be passed to the async function filling the FlowPane on creation of this full View)
     * @return The finished component
     */
    private ScrollPane createBrowseGrid(AnimeListInfo animeListInfo) {

        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();
        animeGrid = new FlowGapPane(screenWidth / 9, screenWidth / 9 * RATIO, 20);

        // Load anime grid with grid items
        reloadAnimeGridAsync(animeListInfo.getAnimeList()).join();

        // Pagination element
        this.pagination = new Pagination(animeListInfo.getLastPage(), this::handlePageSelection);

        // Wrapper around anime grid and pagination
        VBox wrapper = new VBox(10, animeGrid, pagination);
        wrapper.setMaxWidth(Double.MAX_VALUE);


        // ScrollPane since grid will usually be too large
        ScrollPane scrollPane = new ScrollPane(wrapper);
        scrollPane.getStyleClass().add("grid-scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Lets the FlowGapPane know who to listen to for size changes
        animeGrid.setWrapperPane(scrollPane);


        // Smooth scroll listener because JavaFX does not hav smooth scrolling..
        // in /util/, SmoothScroll
        this.smoothScroll = new SmoothScroll(scrollPane, wrapper);


        // New: Try to set non-visible grid items to not render to improve performance on large grids
        scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> {
            updateVisibleGridItems(scrollPane);
        });


        scrollPane.widthProperty().addListener(e -> updateVisibleGridItems(scrollPane));
        scrollPane.heightProperty().addListener(e -> updateVisibleGridItems(scrollPane));


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


    /**
     * Even though it's not strictly necessary for the Browse views due to small page size, we
     * employ a LazyLoader-like approach on them.
     * <p>This is a far less efficient approach than the LazyLoader implementation the Log views use,
     * but it still works fine exactly because of the small page size.</p>
     * <p><a href="https://stackoverflow.com/a/30780960">Idea from StackOverflow</a></p>
     * @param scrollPane The scrollPane wrapping the FlowPane of anime (where the lazy loading is run on)
     */
    private void updateVisibleGridItems(ScrollPane scrollPane) {

        // Wrap in runLater for scrollPane resize update, make sure scrollPane size is set correctly.
        Platform.runLater(() -> {

            Bounds paneBounds = scrollPane.localToScene(scrollPane.getBoundsInLocal());

            if (scrollPane.getContent() instanceof Parent) {
                for (Node n : animeGrid.getChildren()) {
                    Bounds nodeBounds = n.localToScene(n.getBoundsInLocal());



                    boolean inViewport = paneBounds.intersects(nodeBounds);
                    AnimeInfo anime = (AnimeInfo) n.getUserData();

                    if (inViewport && !n.isVisible()) {
                        n.setStyle("-fx-background-image: url('" + anime.getImageUrl() + "');");
                        // n.setStyle("-fx-background-image: url('" + anime.getSmallImageUrl() + "');");
                        n.setVisible(true);

                        // TODO This fade-animation can be removed later, it's for testing right now. Probably expensive. Unsure.
                        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), n);
                        fadeIn.setFromValue(0.0);
                        fadeIn.setToValue(1.0);
                        fadeIn.play();

                    } else if (!inViewport && n.isVisible()) {
                        n.setVisible(false);
                        n.setStyle("-fx-background-image: none;");
                    }
                }
            }
        });
    }

    private void handlePageSelection(int page) {
        if (!apiLock) {
            apiLock = true;
            invokeAnimatedAPICall(page);
        }
    }

    /**
     * Called on API button ("Season", "Top"..) or pagination button click.
     * Send the correct async API call to the API call listener, refreshes the grid, and invokes the loading bar animation.
     * @param page The called page of the API call (set to 1 on component creation and on API call of a new type
     *             e.g. switching from Browse - Season to Browse - Top)
     */
    private void invokeAnimatedAPICall(int page) {
        // Begin load animation
        VBox darkBackground = createSearchBackground();
        FadeTransition fadeInBackground = createFadeInTransition(darkBackground, 0.2, 0.8);
        fadeInBackground.play();

        loadingBarListener.animateLoadingBar(50, 0.2);

        // Async API call
        getPageForCurrentQuery(page).thenAccept(info -> {

            // Middle load animation
            loadingBarListener.animateLoadingBar(80, 0.2);

            // Update grid in the background
            reloadAnimeGridAsync(info.getAnimeList());

            Platform.runLater(() -> {
                pagination.updatePaginationButtons(page, info.getLastPage());

                // Inner runLater for animation end after everything is loaded
                Platform.runLater(() -> {
                    PauseTransition pause = new PauseTransition(Duration.seconds(0.1));
                    pause.setOnFinished(ev -> {


                        loadingBarListener.animateLoadingBar(100, 0.1);

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
        return switch (searchMode) {
            case "SEASON" -> apiRequestListener.getCurrentAnimeSeason(page);
            case "UPCOMING" -> apiRequestListener.getUpcomingAnime(page);
            case "TOP" -> apiRequestListener.getTopAnime(page);
            default ->  // Default mode: SEARCH
                    apiRequestListener.getAnimeSearch(searchString, page);
        };
    }


    /**
     * Reloads the FlowPane of anime with new information when the answer of an API call is received.
     * @param animeList The List of anime to load into the FlowPane.
     * @return A CompletableFuture so this can be used as a blocking function for the first API call on creation of the full View
     */
    private CompletableFuture<Void> reloadAnimeGridAsync(List<AnimeInfo> animeList) {
        return CompletableFuture.supplyAsync(() -> createAnimeGridItems(animeList))
                .thenAccept(animeBoxes -> {
                    Platform.runLater(() -> {
                        animeGrid.getChildren().clear();
                        pagination.setPaginationButtonVisibility(false);
                        animeGrid.getChildren().addAll(animeBoxes);

                        new AnimationTimer() {
                            @Override
                            public void handle(long now) {
                                Bounds paneBounds = scrollPane.localToScene(scrollPane.getBoundsInLocal());
                                if (paneBounds.getWidth() > 0 && paneBounds.getHeight() > 0) {

                                    adjustGridItemHeights();

                                    // Wait one frame so JavaFX can actually set the real heights
                                    // (For visibility intersection tests)
                                    PauseTransition pause = new PauseTransition(Duration.millis(16));
                                    pause.setOnFinished(e -> {
                                        if (smoothScroll != null) {
                                            scrollPane.setVvalue(0);
                                            smoothScroll.resetAccumulatedVValue();
                                        }
                                        updateVisibleGridItems(scrollPane);
                                        pagination.setPaginationButtonVisibility(true);
                                    });
                                    pause.play();


                                    stop();
                                }
                            }
                        }.start();
                    });
                });
    }


    private List<VBox> createAnimeGridItems(List<AnimeInfo> animeList) {
        List<VBox> animeBoxes = new ArrayList<>();
        for (AnimeInfo anime : animeList) {
            AnimeInfo databaseAnime = databaseRequestListener.requestAnimeFromDatabase(anime.getId());

            AnimeBox animeBox = new AnimeBox(anime, languagePreference);
            animeBox.setOnMouseClick(this::createPopupScreen);
            animeBox.setRatingBorder(databaseAnime, true);

            animeBoxes.add(animeBox);
        }
        return animeBoxes;
    }


    /**
     * Runs when the FlowPane of anime is refreshed.
     * JavaFX and item heights are janky, hence we enforce them according to our wanted aspect ratio.
     */
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


    @Override
    public void onPopupClosed(AnimeBox popupParent) {
        AnimeInfo anime = (AnimeInfo) popupParent.getUserData();
        AnimeInfo databaseAnime = databaseRequestListener.requestAnimeFromDatabase(anime.getId());
        popupParent.setRatingBorder(databaseAnime, true);
    }


    /**
     * Creates a darkener full screen effect.
     * @return The finished background.
     */
    private VBox createSearchBackground() {
        VBox darkBackground = new VBox();
        darkBackground.getStyleClass().add("grid-media-popup-background");
        VBox.setVgrow(darkBackground, Priority.ALWAYS);
        HBox.setHgrow(darkBackground, Priority.ALWAYS);
        darkBackground.setOpacity(0);
        stackPane.getChildren().add(darkBackground);
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

}
