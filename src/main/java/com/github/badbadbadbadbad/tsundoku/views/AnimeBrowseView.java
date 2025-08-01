package com.github.badbadbadbadbad.tsundoku.views;

import com.github.badbadbadbadbad.tsundoku.controllers.APIRequestListener;
import com.github.badbadbadbadbad.tsundoku.controllers.GridFilterListener;
import com.github.badbadbadbadbad.tsundoku.controllers.LoadingBarListener;
import com.github.badbadbadbadbad.tsundoku.controllers.DatabaseRequestListener;
import com.github.badbadbadbadbad.tsundoku.external.FlowGapPane;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import com.github.badbadbadbadbad.tsundoku.models.AnimeListInfo;
import com.github.badbadbadbadbad.tsundoku.external.SmoothScroll;
import com.github.badbadbadbadbad.tsundoku.util.AspectRatio;
import com.github.badbadbadbadbad.tsundoku.views.ControlsPane.ButtonConfig;
import com.github.badbadbadbadbad.tsundoku.views.ControlsPane.ControlsPane;
import com.github.badbadbadbadbad.tsundoku.views.ControlsPane.FilterConfig;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;


/**
 * The full view component displayed in the main content pane for browse mode "Browse" and media mode "Anime".
 *
 * <p>It would be far cleaner to have some BrowseView superclass with this inheriting, but I don't want to overcomplicate things
 * before I know what quirks the main content views for other media modes may involve (due to relying on data from external APIs).</p>
 */
public class AnimeBrowseView implements PopupMakerView {

    private final Stage stage;
    private final APIRequestListener apiRequestListener;
    private final LoadingBarListener loadingBarListener;
    private final DatabaseRequestListener databaseRequestListener;

    private ScrollPane scrollPane;
    private FlowGapPane animeGrid;
    private Pagination pagination;
    private StackPane stackPane;
    private SmoothScroll smoothScroll;

    private final Map<String, Consumer<String>> filterUpdaters = new HashMap<>();
    private final Map<String, String> filterDefaults = new HashMap<>();

    private final StringProperty searchStringProperty = new SimpleStringProperty("");
    private String searchMode = "SEASON";  // Changes between SEASON, TOP, and SEARCH depending on last mode selected (so pagination calls "current mode")
    private boolean apiLock = false;
    private final String languagePreference;


    public AnimeBrowseView(Stage stage, LoadingBarListener loadingBarListener, APIRequestListener apiRequestListener,
                           GridFilterListener gridFilterListener, DatabaseRequestListener databaseRequestListener, String languagePreference) {

        this.stage = stage;
        this.loadingBarListener = loadingBarListener;
        this.apiRequestListener = apiRequestListener;
        this.databaseRequestListener = databaseRequestListener;
        this.languagePreference = languagePreference;

        // Filters must know which internal variable to update with the chosen setting
        this.filterUpdaters.put("Order by", gridFilterListener::onAnimeOrderByChanged);
        this.filterUpdaters.put("Release status", gridFilterListener::onAnimeStatusChanged);
        this.filterUpdaters.put("Year ≥", gridFilterListener::onAnimeStartYearChanged);
        this.filterUpdaters.put("Year ≤", gridFilterListener::onAnimeEndYearChanged);

        this.filterDefaults.put("Order by", gridFilterListener.getAnimeOrderByDefault());
        this.filterDefaults.put("Release status", gridFilterListener.getAnimeStatusDefault());
        this.filterDefaults.put("Year ≥", gridFilterListener.getAnimeStartYearDefault());
        this.filterDefaults.put("Year ≤", gridFilterListener.getAnimeEndYearDefault());
    }


    /**
     * Called once by ViewsController, creates the whole View component
     * @return The finished view
     */
    public Region createGridView() { // TODO Instead of this being Region, just make the class extend Region?

        VBox root = new VBox();
        VBox.setVgrow(root, Priority.ALWAYS);
        HBox.setHgrow(root, Priority.ALWAYS);


        // StackPane wrapper to allow for popup functionality when grid element is clicked
        stackPane = new StackPane();
        VBox.setVgrow(stackPane, Priority.ALWAYS);
        HBox.setHgrow(stackPane, Priority.ALWAYS);
        stackPane.getChildren().add(root);

        // TODO Put these naked strings into an enum?

        List<String> orderByOptions = List.of(
                "Default",
                "Title: Ascending", "Title: Descending",
                "Rating: Highest", "Rating: Lowest",
                "Popular: Most", "Popular: Least"
        );
        FilterConfig orderByFilter = FilterConfig.dropdown(
                "Order by",
                orderByOptions,
                List.of(filterUpdaters.get("Order by")),
                filterDefaults.get("Order by")
        );

        List<String> releaseStatusOptions= List.of("Any", "Complete", "Airing", "Upcoming");
        FilterConfig releaseStatusFilter = FilterConfig.dropdown(
                "Release status",
                releaseStatusOptions,
                List.of(filterUpdaters.get("Release status")),
                filterDefaults.get("Release status")
        );

        FilterConfig yearFilter = FilterConfig.doubleNumber(
                "Year ≥", "Year ≤",
                List.of(filterUpdaters.get("Year ≥")), List.of(filterUpdaters.get("Year ≤")),
                fireApiCall("SEARCH"),
                filterDefaults.get("Year ≥"), filterDefaults.get("Year ≤")
        );

        List<FilterConfig> animeFilters = List.of(orderByFilter, releaseStatusFilter, yearFilter);


        List<ButtonConfig> animeButtons = List.of(
                new ButtonConfig("Season", fireApiCall("SEASON")),
                new ButtonConfig("Upcoming", fireApiCall("UPCOMING")),
                new ButtonConfig("Top", fireApiCall("TOP")),
                new ButtonConfig("Search", fireApiCall("SEARCH")
                )
        );

        BiFunction<Double, Integer, Pair<Integer, Integer>> layoutStrategy = (windowWidth, filterCount) -> {
            int cols = 3;
            int rows = (int) Math.ceil((double) filterCount / cols);
            return new Pair<>(cols, rows);
        };

        ControlsPane controls = new ControlsPane(
                animeFilters,
                animeButtons,
                fireApiCall("SEARCH"),
                () -> updateVisibleGridItems(scrollPane),
                stage.widthProperty(),
                layoutStrategy
        );

        searchStringProperty.bind(controls.getSearchStringProperty());


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
     * Creates the FlowPane of anime, wrapped by a scrollPane.
     * Does not actually create the child elements themselves, that is done in an async sub-function.
     * @param animeListInfo A List of AnimeInfo (to be passed to the async function filling the FlowPane on creation of this full View)
     * @return The finished component
     */
    private ScrollPane createBrowseGrid(AnimeListInfo animeListInfo) {

        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();
        animeGrid = new FlowGapPane(screenWidth / 9, screenWidth / 9 * AspectRatio.ANIME.getRatio(), 20);

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
                    apiRequestListener.getAnimeSearch(searchStringProperty.get(), page);
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
                double newHeight = width * AspectRatio.ANIME.getRatio();
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

    private Runnable fireApiCall(String mode) {
        return () -> {
            if (!apiLock && (!mode.equals("SEARCH") || !searchStringProperty.get().isEmpty())) {
                searchMode = mode;
                apiLock = true;
                invokeAnimatedAPICall(1);
            }
        };
    }
}
