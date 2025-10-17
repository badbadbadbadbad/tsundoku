package com.github.badbadbadbadbad.tsundoku.enums.generic;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static java.util.stream.Collectors.toList;

public enum PersonalStatus {
    ANY("Any", 0), //
    IN_PROGRESS("In progress", 1), //
    BACKLOG("Backlog", 2), //
    COMPLETED("Completed", 3), //
    PAUSED("Paused", 4), //
    DROPPED("Dropped", 5), //
    UNTRACKED("Untracked", Integer.MAX_VALUE), //
    ;

    private static final EnumSet<PersonalStatus> FOR_ANIME_INFO =
            EnumSet.of(IN_PROGRESS, BACKLOG, COMPLETED, PAUSED, DROPPED, UNTRACKED);
    private static final EnumSet<PersonalStatus> FOR_FILTER =
            EnumSet.of(ANY, IN_PROGRESS, BACKLOG, COMPLETED, PAUSED, DROPPED);
    private final @NotNull String display;
    private final int sortValue;

    PersonalStatus(@NotNull String display, int sortValue) {
        this.display = display;
        this.sortValue = sortValue;
    }

    public static @NotNull List<String> filterValues() {
        return FOR_FILTER.stream().map(PersonalStatus::toDisplay).collect(toList());
    }

    public static @NotNull List<String> animeInfoValues() {
        return FOR_ANIME_INFO.stream().map(PersonalStatus::toDisplay).collect(toList());
    }

    public static @NotNull List<String> displayValues() {
        return Arrays.stream(values())
                .map(PersonalStatus::toDisplay)
                .toList();
    }

    public static PersonalStatus fromDisplay(String string) {
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
