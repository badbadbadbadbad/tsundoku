package com.github.badbadbadbadbad.tsundoku.util;

import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;

public class StyleUtils {

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
     * @param anime The box to change the border for
     * @param useBlueFallback Default should be blue in Browse view, grey in Log view
     */
    public static String computeBorderClass(AnimeInfo anime, boolean useBlueFallback) {
        if (anime == null) return "grid-media-box-grey";

        switch (anime.getOwnRating()) {
            case "Heart": return "grid-media-box-gold";
            case "Liked": return "grid-media-box-green";
            case "Disliked": return "grid-media-box-red";
        }

        if (useBlueFallback && !anime.getOwnStatus().equals("Untracked")) {
            return "grid-media-box-blue";
        }

        return "grid-media-box-grey";
    }
}
