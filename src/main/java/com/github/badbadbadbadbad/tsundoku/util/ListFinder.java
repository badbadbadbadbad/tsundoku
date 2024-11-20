package com.github.badbadbadbadbad.tsundoku.util;

import com.github.badbadbadbadbad.tsundoku.external.FlowGridPane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

import java.util.List;

public class ListFinder {
    private final List<List<VBox>> lists;

    public ListFinder(List<List<VBox>> lists) {
        this.lists = lists;
    }

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
}