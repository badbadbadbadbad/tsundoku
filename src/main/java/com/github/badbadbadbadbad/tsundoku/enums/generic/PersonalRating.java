package com.github.badbadbadbadbad.tsundoku.enums.generic;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static java.util.stream.Collectors.toList;

public enum PersonalRating {
    ANY("Any", 0), //
    HEART("Favourite", 1), //
    LIKED("Liked", 2), //
    DISLIKED("Disliked", 3), //
    UNSCORED("Unscored", 4), //
    UNTRACKED("Untracked", Integer.MAX_VALUE), //
    ;

    private static final EnumSet<PersonalRating> FOR_ANIME_INFO =
            EnumSet.of(HEART, LIKED, DISLIKED, UNSCORED, UNTRACKED);
    private static final EnumSet<PersonalRating> FOR_FILTER =
            EnumSet.of(ANY, HEART, LIKED, DISLIKED, UNSCORED);
    private final @NotNull String display;
    private final int sortValue;

    PersonalRating(@NotNull String display, int sortValue) {
        this.display = display;
        this.sortValue = sortValue;
    }

    public static @NotNull List<String> filterValues() {
        return FOR_FILTER.stream().map(PersonalRating::toDisplay).collect(toList());
    }

    public static @NotNull List<String> animeInfoValues() {
        return FOR_ANIME_INFO.stream().map(PersonalRating::toDisplay).collect(toList());
    }

    public static @NotNull List<String> displayValues() {
        return Arrays.stream(values())
                .map(PersonalRating::toDisplay)
                .toList();
    }

    public static PersonalRating fromDisplay(String string) {
        for (var v : values()) if (v.display.equals(string)) return v;
        throw new IllegalArgumentException("Value not in enum: " + string);
    }

    public @NotNull String toDisplay() {
        return display;
    }

    public int getSortValue() {
        return sortValue;
    }
}
