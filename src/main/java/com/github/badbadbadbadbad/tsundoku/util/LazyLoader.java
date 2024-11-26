package com.github.badbadbadbadbad.tsundoku.util;

import com.github.badbadbadbadbad.tsundoku.external.FlowGridPane;
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

public class LazyLoader {
    private final double RATIO = 318.0 / 225.0;

    private final ExecutorService imageLoaderExecutor = Executors.newFixedThreadPool(3);
    private AnimationTimer batchImageUpdaterTimer;

    private final List<Future<?>> activeImageTasks = new ArrayList<>();
    private final ConcurrentLinkedQueue<Pair<Node, String>> pendingImageUpdates = new ConcurrentLinkedQueue<>();

    private final PaneFinder paneFinder;
    private final ScrollPane scrollPane;
    private final List<FlowGridPane> flowPanes;
    private int firstVisibleIndex;
    private int lastVisibleIndex;

    private final PauseTransition loaderPause = new PauseTransition(Duration.seconds(0.1));
    private final PauseTransition imagePause = new PauseTransition(Duration.seconds(0.01));

    public LazyLoader(ScrollPane scrollPane, List<FlowGridPane> flowPanes) {
        this.scrollPane = scrollPane;
        this.flowPanes = flowPanes;
        this.paneFinder = new PaneFinder(flowPanes);

        loaderPause.setOnFinished(e -> executeUpdateVisibilityFull());
        imagePause.setOnFinished(e -> loadVisibleImages());

        startBatchImageUpdater();

        Pair<FlowGridPane, Integer> first = paneFinder.findPaneAndChildIndex(0);

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


    private void adjustGridItemHeights() {
        for (FlowGridPane pane: flowPanes) {
            for (Node node : pane.getChildren()) {
                if (node instanceof VBox animeBox) {
                    double width = animeBox.getWidth();
                    double newHeight = width * RATIO;
                    animeBox.setMinHeight(newHeight);
                    animeBox.setPrefHeight(newHeight);
                    animeBox.setMaxHeight(newHeight);
                }
            }
        }
    }

    public void unloadVisible() {

        // Also stop any potential loading still going on right now
        loaderPause.stop();
        imagePause.stop();
        if (batchImageUpdaterTimer != null) {
            batchImageUpdaterTimer.stop();
            pendingImageUpdates.clear();
        }


        while (firstVisibleIndex <= lastVisibleIndex) {
            Pair<FlowGridPane, Integer> firstNodePair = paneFinder.findPaneAndChildIndex(firstVisibleIndex);

            if (firstNodePair == null) {
                return;
            }

            Node firstNode = firstNodePair.getKey().getChildren().get(firstNodePair.getValue());

            makeItemInvisible(firstNode);
            firstVisibleIndex++;


        }
    }

    public void updateVisibilityFull() {
        loaderPause.stop();
        imagePause.stop();
        if (batchImageUpdaterTimer != null) {
            batchImageUpdaterTimer.stop();
        }

        loaderPause.playFromStart();
    }

    public void executeUpdateVisibilityFull() {
        Bounds paneBounds = scrollPane.localToScene(scrollPane.getBoundsInLocal());

        // Currently visible items: Downwards from start
        while (firstVisibleIndex <= lastVisibleIndex) {
            Pair<FlowGridPane, Integer> firstNodePair = paneFinder.findPaneAndChildIndex(firstVisibleIndex);

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
            Pair<FlowGridPane, Integer> lastNodePair = paneFinder.findPaneAndChildIndex(lastVisibleIndex);
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
            Pair<FlowGridPane, Integer> nodePair = paneFinder.findPaneAndChildIndex(index);
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
            Pair<FlowGridPane, Integer> nodePair = paneFinder.findPaneAndChildIndex(index);
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

    // Basic binary search to identify new visible content on long scrolls
    void identifyNewVisibleNode(Bounds paneBounds) {
        int low = 0;
        int high = paneFinder.getTotalItemCount() - 1;

        while (low <= high) {
            int mid = (low + high) / 2;

            Pair<FlowGridPane, Integer> nodePair = paneFinder.findPaneAndChildIndex(mid);
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


    private void loadVisibleImages() {
        for (int i = firstVisibleIndex; i <= lastVisibleIndex; i++) {
            Pair<FlowGridPane, Integer> nodePair = paneFinder.findPaneAndChildIndex(i);

            if (nodePair == null) {
                return;
            }

            Node node = nodePair.getKey().getChildren().get(nodePair.getValue());
            makeItemVisible(node);
        }
    }


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

            synchronized (activeImageTasks) {
                activeImageTasks.add(future);
            }
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

    public void shutdownImageLoaderExecutor() {
        imageLoaderExecutor.shutdown();

        try {
            if (!imageLoaderExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                imageLoaderExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            imageLoaderExecutor.shutdownNow();
        }
    }
}
