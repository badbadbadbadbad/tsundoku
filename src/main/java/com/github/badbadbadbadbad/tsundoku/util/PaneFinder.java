package com.github.badbadbadbadbad.tsundoku.util;

import com.github.badbadbadbadbad.tsundoku.external.FlowGapPane;
import com.github.badbadbadbadbad.tsundoku.external.FlowGridPane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.util.List;

/**
 * Helper class to treat multiple FlowPanes as a "super-list" with indices incrementing throughout all lists.
 */
public class PaneFinder {
    private final List<FlowGapPane> flowPanes;

    public PaneFinder(List<FlowGapPane> flowPanes) {
        this.flowPanes = flowPanes;
    }

    /**
     * Goes through the panes attached to this PaneFinder and finds some item specified by the super-index across the Panes.
     * So if the first Pane has 10 items, and we request index 15, then we keep counting past the first Pane.
     * @param nodeIndex The requested index (shared across all the panes).
     * @return A Pair, made up of one of the Panes of this PaneFinder and some index into this Pane.
     * The Pair points to the item specified by the List of Pane of this PaneFinder and the requested nodeIndex.
     */
    public Pair<FlowGapPane, Integer> findPaneAndChildIndex(int nodeIndex) {
        int cumulativeCount = 0;
        for (FlowGapPane pane : flowPanes) {
            int paneSize = pane.getChildren().size();
            if (nodeIndex < cumulativeCount + paneSize) {
                int childIndex = nodeIndex - cumulativeCount;
                return new Pair<>(pane, childIndex);
            }
            cumulativeCount += paneSize;
        }
        return null;
    }


    /**
     * Reverse functionality of findPaneAndChildIndex.
     * For some child of the Panes of this PaneFinder, we identify its corresponding super-index (shared across all Panes).
     * @param child The child whose super-index is requested.
     * @return The super-index of the specified child.
     */
    public int findNodeIndexByGridChild(VBox child) {
        int cumulativeIndex = 0;
        int index = 0;
        for (FlowGapPane pane : flowPanes) {
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