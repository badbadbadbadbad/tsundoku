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
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


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
    private HBox paginationButtons;
    private StackPane stackPane;
    private SmoothScroll smoothScroll;


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

            /*
            if (newValue.matches("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsHangul}\\p{Alnum} ]*")) {
                searchString = newValue;
            } else {
                searchBar.setText(oldValue);
            }

             */
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
        FlowGridPane filtersGrid = new FlowGridPane(2, 3);
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

        VBox statusFilter = createDropdownFilter("Status",
                new String[]{"Any", "Complete", "Airing", "Upcoming"}, "Any");

        VBox startYearFilter = createNumberFilter("Start year ≥");
        VBox endYearFilter = createNumberFilter("End year ≤");


        filtersGrid.getChildren().addAll(orderByFilter, statusFilter, startYearFilter, endYearFilter);


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
        if (labelText.equals("Order by")) {
            comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                gridFilterListener.onAnimeOrderByChanged(newVal);
            });
        } else if (labelText.equals("Status")) {
            comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                gridFilterListener.onAnimeStatusChanged(newVal);
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
            if (!newValue.matches("\\d{0,4}")) {
                textField.setText(oldValue);
            }
        });

        // Filter change listeners
        if (labelText.equals("Start year ≥")) {
            textField.textProperty().addListener((obs, oldVal, newVal) -> {
                gridFilterListener.onAnimeStartYearChanged(newVal);
            });
        } else if (labelText.equals("End year ≤")) {
            textField.textProperty().addListener((obs, oldVal, newVal) -> {
                gridFilterListener.onAnimeEndYearChanged(newVal);
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

        /*
        animeGrid = new FlowGridPane(2, 3);  // Default values here shouldn't matter but are needed, so..
        animeGrid.setHgap(20);
        animeGrid.setVgap(20);
        animeGrid.setMaxWidth(Double.MAX_VALUE);
         */


        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();
        animeGrid = new FlowGapPane(screenWidth / 9, screenWidth / 9 * RATIO, 20);
        // VBox.setVgrow(animeGrid, Priority.ALWAYS);
        // animeGrid.setMaxWidth(Double.MAX_VALUE);

        // Load anime grid with grid items
        reloadAnimeGridAsync(animeListInfo.getAnimeList()).join();

        /*
        ChangeListener<Number> widthListener = (obs, oldWidth, newWidth) -> {
            double windowWidth = newWidth.doubleValue();
            int animesAmount = animeGrid.getChildren().size();

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

        };
        stage.widthProperty().addListener(widthListener);
        widthListener.changed(stage.widthProperty(), stage.getWidth(), stage.getWidth());
         */


        // Pagination element
        HBox pagination = createPagination(animeListInfo.getLastPage());


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


    /**
     * Pagination component below the FlowPane of anime.
     * @param pages Total amount of pages
     * @return The finished component
     */
    private HBox createPagination(int pages) {

        // Wrapper (full width HBox)
        HBox pagination = new HBox();
        pagination.getStyleClass().add("pagination-wrapper");
        pagination.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pagination, Priority.ALWAYS);

        // Wrapper (only the button box)
        paginationButtons = new HBox(10);
        paginationButtons.getStyleClass().add("pagination-buttons");
        pagination.getChildren().add(paginationButtons);

        // Make the actual buttons
        updatePaginationButtons(1, pages);

        return pagination;
    }


    /**
     * Refreshes the pagination buttons of the pagination component.
     * Runs on pagination component creating and after any API call.
     * @param selectedPage The clicked page of the API call (set to 1 on component creation and on API call of a new type
     *                     e.g. switching from Browse - Season to Browse - Top)
     * @param pages Total amount of pages
     */
    private void updatePaginationButtons(int selectedPage, int pages) {
        paginationButtons.getChildren().clear();

        // Only need "first page" button if it's not already the selected one
        if (selectedPage > 2) {
            paginationButtons.getChildren().add(createPageButton(1, selectedPage));
        }

        // Low numbers ellipsis button
        if (selectedPage > 3) {
            paginationButtons.getChildren().add(createEllipsisButton(paginationButtons, pages));
        }

        // Selected page as well as its prev and next
        for (int i = Math.max(1, selectedPage - 1); i <= Math.min(pages, selectedPage + 1); i++) {
            Button butt = createPageButton(i, selectedPage);
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
            paginationButtons.getChildren().add(createPageButton(pages, selectedPage));
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
                updatePaginationButtons(page, info.getLastPage());

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
        if (searchMode.equals("SEASON")) {
            return apiRequestListener.getCurrentAnimeSeason(page);
        } else if (searchMode.equals("UPCOMING")) {
            return apiRequestListener.getUpcomingAnime(page);
        } else if (searchMode.equals("TOP")) {
            return apiRequestListener.getTopAnime(page);
        } else { // Default mode: SEARCH
            return apiRequestListener.getAnimeSearch(searchString, page);
        }
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
                        paginationButtons.setVisible(false);
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
                                        paginationButtons.setVisible(true);
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
            VBox animeBox = createAnimeBox(anime);
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
     * Creates a numbered page button for the pagination component.
     * @param ownPage The number set as the String for this button's text
     * @param selectedPage The selected page number that triggered the pagination component's refresh
     *                     (so this button knows if it's already the clicked button or not)
     * @return The finished Button
     */
    private Button createPageButton(int ownPage, int selectedPage) {
        Button pageButton =  new Button(String.valueOf(ownPage));
        pageButton.getStyleClass().add("pagination-button");

        if (!(ownPage == selectedPage)) {
            pageButton.setOnAction(event -> {
                if(!apiLock) {
                    apiLock = true;
                    invokeAnimatedAPICall(ownPage);
                }
            });
        }

        return pageButton;
    }


    /**
     * Creates an ellipsis page button for the pagination component (that's a "...").
     * Ellipsis buttons bridge gaps between large amount of pages (so the pagination only shows the first, current, and last pages).
     * They can also be clicked to change into a "page search" number input field.
     * @param paginationButtons The pagination component
     * @param pages Amount of max pages of the pagination component
     * @return The finished Button
     */
    private Button createEllipsisButton(HBox paginationButtons, int pages) {
        Button ellipsisButton =  new Button("...");
        ellipsisButton.getStyleClass().add("pagination-button");

        // On click, turn the ellipsis button into a number input to get to specific pages
        ellipsisButton.setOnAction(event -> {
            TextField pageInputField = createPageInputField(paginationButtons, pages);
            int index = paginationButtons.getChildren().indexOf(ellipsisButton);
            paginationButtons.getChildren().set(index, pageInputField);
            pageInputField.requestFocus();
        });

        return ellipsisButton;
    }


    /**
     * Creates a number input text field, used to replace an ellipsis button of the pagination component when it's clicked.
     * On focus loss, replaces itself with an ellipsis button again.
     * @param paginationButtons The pagination component
     * @param pages Amount of max pages of the pagination component
     * @return The finished number input field
     */
    private TextField createPageInputField(HBox paginationButtons, int pages) {
        TextField pageInputField = new TextField();
        pageInputField.getStyleClass().add("pagination-input-field");

        // Numbers only regex for page input (as pages are always numbers..)
        pageInputField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                pageInputField.setText(newValue.replaceAll("\\D", ""));
            }
        });

        // Handle focus loss just like enter press, invoke the page being called
        pageInputField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                handlePageInput(paginationButtons, pageInputField, pages);
            }
        });

        // Enter pressed (default value for setOnAction for textFields)
        pageInputField.setOnAction(event -> handlePageInput(paginationButtons, pageInputField, pages));

        return pageInputField;
    }


    /**
     * Triggers when a number input text field (the replacement for an activated ellipsis pagination button) is activated.
     * Invokes an API call for reasonable input.
     * @param paginationButtons The pagination component
     * @param pageInputField The number input field whose input is considered
     * @param pages Amount of max pages of the pagination component
     */
    private void handlePageInput(HBox paginationButtons, TextField pageInputField, int pages) {
        String input = pageInputField.getText();
        int index = paginationButtons.getChildren().indexOf(pageInputField);


        if (input.isEmpty()) { // If no input, just turn input field back to ellipsis button
            paginationButtons.getChildren().set(index, createEllipsisButton(paginationButtons, pages));
        } else {
            try {
                // Clamp input to [first page, last page] and handle it instead of throwing the input away
                int clampedPage = Math.clamp(Integer.parseInt(input), 1, pages);

                if(!apiLock) {
                    apiLock = true;
                    invokeAnimatedAPICall(clampedPage);
                }

            } catch (NumberFormatException e) {
                // Number formatting issues _shouldn't_ exist to my knowledge, but provide failsafe anyway
                paginationButtons.getChildren().set(index, createEllipsisButton(paginationButtons, pages));
            }

        }
    }


    /**
     * Makes a VBox component to be used as a child for the full FlowPane of anime.
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

        // Label testLabel = new Label(title);
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
     * Changes the border of an anime in the FlowPane based on its status in the Log and its rating.
     * <ul>
     *     <li>Grey: Default</li>
     *     <li>Blue: In Log, unrated</li>
     *     <li>Gold: In Log, rated with Heart</li>
     *     <li>Green: In Log, rated with Liked</li>
     *     <li>Red: In Log, rated with Disliked</li>
     * </ul>
     * This is done by altering CSS classes of the anime's VBox component in the FlowPane.
     * @param animeBox The box to change the border for
     */
    private void setRatingBorder(VBox animeBox) {
        AnimeInfo anime = (AnimeInfo) animeBox.getUserData();
        AnimeInfo databaseAnime = databaseRequestListener.requestAnimeFromDatabase(anime.getId());

        animeBox.getStyleClass().removeAll(
                "grid-media-box-gold",
                "grid-media-box-green",
                "grid-media-box-red",
                "grid-media-box-blue",
                "grid-media-box-grey"
        );

        if (databaseAnime == null) {
            animeBox.getStyleClass().add("grid-media-box-grey");
            return;
        }

        String rating =  databaseAnime.getOwnRating();
        String ownStatus = databaseAnime.getOwnStatus();

        if (rating.equals("Heart")) {
            animeBox.getStyleClass().add("grid-media-box-gold");
        } else if (rating.equals("Liked")) {
            animeBox.getStyleClass().add("grid-media-box-green");
        } else if (rating.equals("Disliked")) {
            animeBox.getStyleClass().add("grid-media-box-red");
        }

        else if (!ownStatus.equals("Untracked")) {
            animeBox.getStyleClass().add("grid-media-box-blue");
        }

        else {
            animeBox.getStyleClass().add("grid-media-box-blue");
            // animeBox.getStyleClass().add("grid-media-box-grey");
        }
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


    @Override
    public void onPopupClosed(VBox popupParent) {
        // Just need to update the border for the BrowseView. No deleting.
        setRatingBorder(popupParent);
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
