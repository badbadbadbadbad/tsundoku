package com.github.badbadbadbadbad.tsundoku.util;

import com.github.badbadbadbadbad.tsundoku.external.FlowGapPane;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.control.ScrollPane;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.Pair;
import java.util.*;
import java.util.concurrent.*;

import javafx.animation.AnimationTimer;


/**
 * While the Browse views are restricted to pagination due to relying on APIs,
 * the Log views are an infinite scroll. The bottleneck here isn't necessarily the amount of items in the log
 * (as there just aren't that many anime / manga..), but rather each item loading an image in the log.
 * Hence, we use a lazy loader background service to only keep images in view loaded.
 */
public class LazyLoader {

    // Threads dedicated to image loading
    private final ExecutorService imageLoaderExecutor = Executors.newFixedThreadPool(3);

    // Simple background timer taking loaded images and setting them as backgrounds, once per frame
    // (Somewhat hacky JavaFX way to have a timed background service in the JavaFX thread without Platform.runLater)
    private AnimationTimer batchImageUpdaterTimer;
    private final ConcurrentLinkedQueue<Pair<Node, String>> pendingImageUpdates = new ConcurrentLinkedQueue<>();

    private final AspectRatio aspectRatio;
    private final PaneFinder paneFinder;
    private final ScrollPane scrollPane;
    private final List<FlowGapPane> flowPanes;
    private int firstVisibleIndex;
    private int lastVisibleIndex;

    // There's some slight issues if the image loader / image setter services run all the time;
    // it's possible for actions to "overwrite" each other in a way.
    // Hence, we pause the services on scroll events, and resume them when the scrolling ends.
    // May have to tinker with the timers still.
    private final PauseTransition loaderPause = new PauseTransition(Duration.seconds(0.1));
    private final PauseTransition imagePause = new PauseTransition(Duration.seconds(0.1));

    public LazyLoader(ScrollPane scrollPane, List<FlowGapPane> flowPanes, AspectRatio aspectRatio) {
        this.aspectRatio = aspectRatio;
        this.scrollPane = scrollPane;
        this.flowPanes = flowPanes;
        this.paneFinder = new PaneFinder(flowPanes);

        loaderPause.setOnFinished(e -> executeUpdateVisibilityFull());
        imagePause.setOnFinished(e -> loadVisibleImages());

        startBatchImageUpdater();

        Pair<FlowGapPane, Integer> first = paneFinder.findPaneAndChildIndex(0);

        // We only initialize stuff if the log actually contains items.
        // If the log contains no items, then it's impossible to add any without switching to Browse view first.
        // Hence, there's no way of having a Log view open without a LazyLoader.
        if (first != null) {

            setFirstVisibleIndex(0);
            setLastVisibleIndex(0);
            makeItemVisible(first.getKey().getChildren().get(first.getValue()));


            // Force adjustGridItemHeights to run post-layout-calculated so the grid items actually have a width.
            Platform.runLater(() -> {

                // Oh wait, turns out Platform.runLater isn't necessarily safe and the scrollPane can still have h/w of 0.
                // Thanks, JavaFX. Use janky AnimationTimer to force to wait for scrollPane layout calculation
                new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        Bounds paneBounds = scrollPane.localToScene(scrollPane.getBoundsInLocal());
                        if (paneBounds.getWidth() > 0 && paneBounds.getHeight() > 0) {

                            adjustGridItemHeights();

                            // And a nested Platform.runLater because adjustGridItemHeights sets min/max/pref height
                            // This is needed so JavaFX can actually set the _true_ height, which this function needs
                            // Thanks, JavaFX
                            Platform.runLater(() -> {
                                updateVisibilityFull();
                            });

                            stop();
                        }
                    }
                }.start();


            });

        }
    }


    /**
     * Runs once on LazyLoader startup (which is when a Log view is created).
     * JavaFX and item heights in our Log grids are janky, hence we enforce them according to our wanted aspect ratio.
     */
    private void adjustGridItemHeights() {
        for (FlowGapPane pane: flowPanes) {
            for (Node node : pane.getChildren()) {
                if (node instanceof VBox animeBox) {
                    double width = animeBox.getWidth();
                    double newHeight = width * aspectRatio.getRatio();
                    animeBox.setMinHeight(newHeight);
                    animeBox.setPrefHeight(newHeight);
                    animeBox.setMaxHeight(newHeight);
                }
            }
        }
    }


    /**
     * Unloads the image backgrounds of all log items currently visible,
     * which is the items with indices from firstVisible to lastVisible.
     */
    public void unloadVisible() {

        // Also stop any potential loading still going on right now
        loaderPause.stop();
        imagePause.stop();
        if (batchImageUpdaterTimer != null) {
            batchImageUpdaterTimer.stop();
            pendingImageUpdates.clear();
        }


        while (firstVisibleIndex <= lastVisibleIndex) {
            Pair<FlowGapPane, Integer> firstNodePair = paneFinder.findPaneAndChildIndex(firstVisibleIndex);

            if (firstNodePair == null) {
                return;
            }

            Node firstNode = firstNodePair.getKey().getChildren().get(firstNodePair.getValue());

            makeItemInvisible(firstNode);
            firstVisibleIndex++;


        }
    }


    /**
     * Does not actually start the visibility update.
     * Instead, this starts the timer that initiates the visibility update when it ends
     * (which may be interrupted / restarted by other calls).
     */
    public void updateVisibilityFull() {
        loaderPause.stop();
        imagePause.stop();
        if (batchImageUpdaterTimer != null) {
            batchImageUpdaterTimer.stop();
        }

        loaderPause.playFromStart();
    }


    /**
     * The actual function to start up a new visibility update.
     * Step 1: Go through visible items and turn them invisible if they left the viewport.
     * Step 2: If no visible items remain, get a new visible item via binary search over the full Log.
     * Step 3: Go through nearby invisible items (starting at visible items) and turn them invisible if in viewport.
     */
    public void executeUpdateVisibilityFull() {
        Bounds paneBounds = scrollPane.localToScene(scrollPane.getBoundsInLocal());

        // Currently visible items: Downwards from start
        while (firstVisibleIndex <= lastVisibleIndex) {
            Pair<FlowGapPane, Integer> firstNodePair = paneFinder.findPaneAndChildIndex(firstVisibleIndex);

            if (firstNodePair == null) {
                return;
            }

            Node firstNode = firstNodePair.getKey().getChildren().get(firstNodePair.getValue());
            boolean inViewport = isItemInViewport(firstNode, paneBounds);

            if (inViewport) {
                break;
            } else {
                makeItemInvisible(firstNode);
                firstVisibleIndex++;
            }
        }

        // Currently visible items: Upwards from end
        while (lastVisibleIndex >= firstVisibleIndex) {
            Pair<FlowGapPane, Integer> lastNodePair = paneFinder.findPaneAndChildIndex(lastVisibleIndex);
            Node lastNode = lastNodePair.getKey().getChildren().get(lastNodePair.getValue());


            boolean inViewport = isItemInViewport(lastNode, paneBounds);

            if (inViewport) {
                break;
            } else {
                makeItemInvisible(lastNode);
                lastVisibleIndex--;
            }
        }


        // Get new index via binary search if we scrolled too far
        if (firstVisibleIndex >= lastVisibleIndex) {
            identifyNewVisibleNode(paneBounds);
        }


        // Check upwards for new visible items
        int index = firstVisibleIndex - 1;
        while (index >= 0) {
            
            Pair<FlowGapPane, Integer> nodePair = paneFinder.findPaneAndChildIndex(index);
            if (nodePair == null) {
                break;
            }

            Node node = nodePair.getKey().getChildren().get(nodePair.getValue());
            boolean inViewport = isItemInViewport(node, paneBounds);

            if (inViewport) {
                firstVisibleIndex = index;
                index--;
            } else {
                break;
            }
        }


        // Check downwards for new visible items
        index = lastVisibleIndex + 1;
        while (index < paneFinder.getTotalItemCount()) {
            Pair<FlowGapPane, Integer> nodePair = paneFinder.findPaneAndChildIndex(index);
            if (nodePair == null) {
                break;
            }

            Node node = nodePair.getKey().getChildren().get(nodePair.getValue());
            boolean inViewport = isItemInViewport(node, paneBounds);

            if (inViewport) {
                lastVisibleIndex = index;
                index++;
            } else {
                break;
            }
        }

        imagePause.playFromStart();
        if (batchImageUpdaterTimer != null) {
            batchImageUpdaterTimer.start();
        }
    }

    /**
     * Basic binary search to identify some child of the Log which is currently in the viewport.
     * The test performed in each search step is just an intersection test between the currently chosen item and the viewport.
     * @param paneBounds The viewport bounds of the scrollPane containing the Log.
     */
    void identifyNewVisibleNode(Bounds paneBounds) {
        int low = 0;
        int high = paneFinder.getTotalItemCount() - 1;

        while (low <= high) {
            int mid = (low + high) / 2;

            Pair<FlowGapPane, Integer> nodePair = paneFinder.findPaneAndChildIndex(mid);
            Node node = nodePair.getKey().getChildren().get(nodePair.getValue());

            boolean inViewport = isItemInViewport(node, paneBounds);

            if (inViewport) {
                setFirstVisibleIndex(mid);
                setLastVisibleIndex(mid);
                return;
            } else {
                Bounds nodeBounds = node.localToScene(node.getBoundsInLocal());
                double nodeY = nodeBounds.getMinY();

                if (nodeY < 0)
                    low = mid + 1;
                else
                    high = mid - 1;
            }
        }
    }



    private boolean isItemInViewport(Node n, Bounds paneBounds) {
        Bounds nodeBounds = n.localToScene(n.getBoundsInLocal());
        return paneBounds.intersects(nodeBounds);
    }


    /**
     * Does not actually start the image loading itself.
     * This goes through the items that _should_ be visible and adds the task to make them visible to the background async pipeline.
     * It's done this way to have an extra check of sorts if this item is still visible or if the user already scrolled past
     * by the time the loader routine reaches this item.
     */
    private void loadVisibleImages() {
        for (int i = firstVisibleIndex; i <= lastVisibleIndex; i++) {
            Pair<FlowGapPane, Integer> nodePair = paneFinder.findPaneAndChildIndex(i);

            if (nodePair == null) {
                return;
            }

            Node node = nodePair.getKey().getChildren().get(nodePair.getValue());
            makeItemVisible(node);
        }
    }


    /**
     * Adds a background thread task to the queue of image loader tasks so this image will be loaded eventually.
     * @param n The Log grid node for which an image needs to be loaded.
     */
    private void makeItemVisible(Node n) {
        if (!n.isVisible()){
            AnimeInfo anime = (AnimeInfo) n.getUserData();

            Future<?> future = CompletableFuture.runAsync(() -> {
                String imageUrl = anime.getImageUrl();
                // String imageUrl = anime.getSmallImageUrl();

                // "true" enables background loading
                Image image = new Image(imageUrl, true);

                image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                    if (newProgress.doubleValue() >= 1.0) {
                        pendingImageUpdates.add(new Pair<>(n, imageUrl));
                    }
                });
            }, imageLoaderExecutor);

        }
    }

    private void makeItemInvisible(Node n) {
        if (n.isVisible()) {
            n.setVisible(false);
            n.setStyle("-fx-background-image: none;");
        }
    }

    public void setFirstVisibleIndex(int index) {
        this.firstVisibleIndex = index;
    }

    public int getFirstVisibleIndex() {
        return this.firstVisibleIndex;
    }

    public void setLastVisibleIndex(int index) {
        this.lastVisibleIndex = index;
    }

    public int getLastVisibleIndex() {
        return this.lastVisibleIndex;
    }


    /**
     * Initialization for the background image setter timer running once per frame.
     * If some image has finished loading, the timer takes it and sets it as the background of the corresponding node.
     */
    public void startBatchImageUpdater() {

        this.batchImageUpdaterTimer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                Pair<Node, String> pair = pendingImageUpdates.poll();
                if (pair != null) {
                    Node node = pair.getKey();
                    Bounds paneBounds = scrollPane.localToScene(scrollPane.getBoundsInLocal());
                    if (isItemInViewport(node, paneBounds)) {
                        node.setVisible(true);
                        node.setOpacity(0.0);

                        node.setStyle("-fx-background-image: url('" + pair.getValue() + "');");


                        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.2), node);
                        fadeIn.setFromValue(0.0);
                        fadeIn.setToValue(1.0);
                        fadeIn.play();
                    }

                }
            }
        };
        batchImageUpdaterTimer.start();

    }

    /**
     * Specifically designated extra worker threads need to be shut down manually.
     * (Else they stay open when the program is closed. That's bad.)
     */
    public void shutdownImageLoaderExecutor() {
        imageLoaderExecutor.shutdown();

        // Force close if issues arise. Internet said this is a good idea
        try {
            if (!imageLoaderExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                imageLoaderExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            imageLoaderExecutor.shutdownNow();
        }
    }
}
