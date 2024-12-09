package com.github.badbadbadbadbad.tsundoku.util;

import com.github.badbadbadbadbad.tsundoku.external.FlowGridPane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.util.List;

public class PaneFinder {
    private final List<FlowGridPane> flowPanes;

    public PaneFinder(List<FlowGridPane> flowPanes) {
        this.flowPanes = flowPanes;
    }

    public Pair<FlowGridPane, Integer> findPaneAndChildIndex(int nodeIndex) {
        int cumulativeCount = 0;
        for (FlowGridPane pane : flowPanes) {
            int paneSize = pane.getChildren().size();
            if (nodeIndex < cumulativeCount + paneSize) {
                int childIndex = nodeIndex - cumulativeCount;
                return new Pair<>(pane, childIndex);
            }
            cumulativeCount += paneSize;
        }
        return null;
    }

    public int findNodeIndexByGridChild(VBox child) {
        int cumulativeIndex = 0;
        int index = 0;
        for (FlowGridPane pane : flowPanes) {
            index = pane.getChildren().indexOf(child);
            if (index != -1) {
                return index + cumulativeIndex;
            } else {
                cumulativeIndex += pane.getChildren().size();
            }
        }
        return -1;
    }

    public int getTotalItemCount() {
        return flowPanes.stream().mapToInt(pane -> pane.getChildren().size()).sum();
    }
}