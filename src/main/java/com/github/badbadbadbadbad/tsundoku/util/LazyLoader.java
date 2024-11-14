package com.github.badbadbadbadbad.tsundoku.util;

import com.github.badbadbadbadbad.tsundoku.external.FlowGridPane;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.control.ScrollPane;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.Pair;
import java.util.*;
import javafx.animation.AnimationTimer;
import java.util.concurrent.Flow;

public class LazyLoader {
    private final double RATIO = 318.0 / 225.0;

    private final PaneFinder paneFinder;
    private final ScrollPane scrollPane;
    private final List<FlowGridPane> flowPanes;
    private int firstVisibleIndex;
    private int lastVisibleIndex;

    public LazyLoader(ScrollPane scrollPane, List<FlowGridPane> flowPanes) {
        this.scrollPane = scrollPane;
        this.flowPanes = flowPanes;
        this.paneFinder = new PaneFinder(flowPanes);

        Pair<FlowGridPane, Integer> first = paneFinder.findPaneAndChildIndex(0);
        if (first != null) {
            /*
            Platform.runLater(() -> {
                // setFirstVisibleIndex(0);
                // setLastVisibleIndex(0);
                // makeItemVisible(first.getKey().getChildren().get(first.getValue()));

                // updateVisibilityFull();
            });

             */

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


    public void updateVisibilityFull() {
        Bounds paneBounds = scrollPane.localToScene(scrollPane.getBoundsInLocal());

        // Verify integrity of visible indices
        /*
        for (int i = firstVisibleIndex; i <= lastVisibleIndex; i++) {
            Node node = paneFinder.findPaneAndChildIndex(i).getKey().getChildren().get(i);
            boolean inViewport = isItemInViewport(node, paneBounds);

            if (inViewport && !node.isVisible()) {
                makeItemVisible(node);
            } else if (!inViewport && node.isVisible()) {
                makeItemInvisible(node);
            }
        }

         */

        while (firstVisibleIndex <= lastVisibleIndex) {
            Pair<FlowGridPane, Integer> firstNodePair = paneFinder.findPaneAndChildIndex(firstVisibleIndex);
            Node firstNode = firstNodePair.getKey().getChildren().get(firstNodePair.getValue());


            boolean inViewport = isItemInViewport(firstNode, paneBounds);


            if (inViewport) {
                makeItemVisible(firstNode);
                break; // Stop if the current firstVisibleIndex is indeed visible
            } else {
                makeItemInvisible(firstNode);
                firstVisibleIndex++; // Move upwards to next potentially visible item
            }
            // firstVisibleIndex++;
        }


        while (lastVisibleIndex >= firstVisibleIndex) {
            Pair<FlowGridPane, Integer> lastNodePair = paneFinder.findPaneAndChildIndex(lastVisibleIndex);
            Node lastNode = lastNodePair.getKey().getChildren().get(lastNodePair.getValue());

            // Node lastNode = paneFinder.findPaneAndChildIndex(lastVisibleIndex).getKey().getChildren().get(lastVisibleIndex);

            boolean inViewport = isItemInViewport(lastNode, paneBounds);

            if (inViewport) {
                makeItemVisible(lastNode);
                break; // Stop if the current lastVisibleIndex is indeed visible
            } else {
                makeItemInvisible(lastNode);
                lastVisibleIndex--; // Move downwards to next potentially visible item
            }
            // lastVisibleIndex--;
        }


        // Check upwards for new visible items
        int index = firstVisibleIndex - 1;
        while (index >= 0) {
            Pair<FlowGridPane, Integer> nodePair = paneFinder.findPaneAndChildIndex(index);
            Node node = nodePair.getKey().getChildren().get(nodePair.getValue());

            // Node node = paneFinder.findPaneAndChildIndex(index).getKey().getChildren().get(index);



            boolean inViewport = isItemInViewport(node, paneBounds);

            if (inViewport) {
                makeItemVisible(node);
                firstVisibleIndex = index;
                index--; // Continue checking upwards
            } else {

                break; // Stop when an item is out of viewport
            }
        }


        // Check downwards for new visible items
        index = lastVisibleIndex + 1;
        while (index < paneFinder.getTotalItemCount()) {
            Pair<FlowGridPane, Integer> nodePair = paneFinder.findPaneAndChildIndex(index);
            Node node = nodePair.getKey().getChildren().get(nodePair.getValue());


            // Node node = paneFinder.findPaneAndChildIndex(index).getKey().getChildren().get(index);



            boolean inViewport = isItemInViewport(node, paneBounds);

            AnimeInfo anime = (AnimeInfo) node.getUserData();


            if (inViewport) {
                makeItemVisible(node);
                lastVisibleIndex = index;
                index++; // Continue checking downwards
            } else {
                break; // Stop when an item is out of viewport
            }
        }

    }

    private boolean isItemInViewport(Node n, Bounds paneBounds) {
        Bounds nodeBounds = n.localToScene(n.getBoundsInLocal());


        return paneBounds.intersects(nodeBounds);
    }

    private void makeItemVisible(Node n) {
        if (!n.isVisible()){
            AnimeInfo anime = (AnimeInfo) n.getUserData();

            n.setStyle("-fx-background-image: url('" + anime.getImageUrl() + "');");
            n.setVisible(true);

            // TODO This fade-animation can be removed later, it's for testing right now. Probably expensive. Unsure.
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.02), n);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        }
        /*
        AnimeInfo anime = (AnimeInfo) n.getUserData();

        n.setStyle("-fx-background-image: url('" + anime.getImageUrl() + "');");
        n.setVisible(true);

        // TODO This fade-animation can be removed later, it's for testing right now. Probably expensive. Unsure.
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), n);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();

         */
    }

    private void makeItemInvisible(Node n) {
        if (n.isVisible()) {
            n.setVisible(false);
            n.setStyle("-fx-background-image: none;");
        }
        /*
        n.setVisible(false);
        n.setStyle("-fx-background-image: none;");

         */
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
}
