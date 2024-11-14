package com.github.badbadbadbadbad.tsundoku.util;

import com.github.badbadbadbadbad.tsundoku.external.FlowGridPane;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import javafx.animation.FadeTransition;
import javafx.geometry.Bounds;
import javafx.scene.control.ScrollPane;
import javafx.scene.Node;
import javafx.util.Duration;
import javafx.util.Pair;
import java.util.*;
import java.util.concurrent.Flow;

public class LazyLoader {
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
            setFirstVisibleIndex(0);
            setLastVisibleIndex(0);
            makeItemVisible(first.getKey().getChildren().get(first.getValue()));

            updateVisibilityFull();
        }
    }

    public void updateVisibilityFull() {
        Bounds paneBounds = scrollPane.localToScene(scrollPane.getBoundsInLocal());

        // Verify integrity of visible indices
        for (int i = firstVisibleIndex; i <= lastVisibleIndex; i++) {
            Node node = paneFinder.findPaneAndChildIndex(i).getKey().getChildren().get(i);
            boolean inViewport = isItemInViewport(node, paneBounds);

            if (inViewport && !node.isVisible()) {
                makeItemVisible(node);
            } else if (!inViewport && node.isVisible()) {
                makeItemInvisible(node);
            }
        }

        // Check upwards for new visible items
        int index = firstVisibleIndex - 1;
        while (index >= 0) {
            Node node = paneFinder.findPaneAndChildIndex(index).getKey().getChildren().get(index);
            if (isItemInViewport(node, paneBounds)) {
                makeItemVisible(node);
                firstVisibleIndex = index;
                index--; // move upwards
            } else {
                break; // stop when item is out of viewport
            }
        }

        // Check downwards for new visible items

    }

    private boolean isItemInViewport(Node n, Bounds paneBounds) {
        Bounds nodeBounds = n.localToScene(n.getBoundsInLocal());
        return paneBounds.intersects(nodeBounds);
    }

    private void makeItemVisible(Node n) {
        AnimeInfo anime = (AnimeInfo) n.getUserData();

        n.setStyle("-fx-background-image: url('" + anime.getImageUrl() + "');");
        n.setVisible(true);

        // TODO This fade-animation can be removed later, it's for testing right now. Probably expensive. Unsure.
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), n);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void makeItemInvisible(Node n) {
        n.setVisible(false);
        n.setStyle("-fx-background-image: none;");
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
