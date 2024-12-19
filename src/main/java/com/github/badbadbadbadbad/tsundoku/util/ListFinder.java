package com.github.badbadbadbadbad.tsundoku.util;

import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.util.List;


/**
 * Helper class to treat multiple Lists as a "super-list" with indices incrementing throughout all lists.
 */
public class ListFinder {
    private final List<List<VBox>> lists;

    public ListFinder(List<List<VBox>> lists) {
        this.lists = lists;
    }

    /**
     * Goes through the lists attached to this ListFinder and finds some item specified by the super-index across the Lists.
     * So if the first List has 10 items, and we request index 15, then we keep counting past the first List.
     * @param nodeIndex The requested index (shared across all the lists).
     * @return A Pair, made up of one of the Lists of this ListFinder and some index into this List.
     * The Pair points to the item specified by the List of Lists of this ListFinder and the requested nodeIndex.
     */
    public Pair<List<VBox>, Integer> findPaneAndChildIndex(int nodeIndex) {
        int cumulativeCount = 0;
        for (List<VBox> list : lists) {
            int paneSize = list.size();
            if (nodeIndex < cumulativeCount + paneSize) {
                int childIndex = nodeIndex - cumulativeCount;
                return new Pair<>(list, childIndex);
            }
            cumulativeCount += paneSize;
        }
        return null;
    }

    /**
     * Reverse functionality of findPaneAndChildIndex.
     * For some child of the Lists of this ListFinder, we identify its corresponding super-index (shared across all Lists).
     * @param child The child whose super-index is requested.
     * @return The super-index of the specified child.
     */
    public int findNodeIndexByGridChild(VBox child) {
        int cumulativeIndex = 0;
        int index = 0;
        for (List<VBox> list : lists) {
            index = list.indexOf(child);
            if (index != -1) {
                return index + cumulativeIndex;
            } else {
                cumulativeIndex += list.size();
            }
        }
        return -1;
    }

    public int getTotalItemCount() {
        return lists.stream()
                .mapToInt(List::size)
                .sum();
    }
}