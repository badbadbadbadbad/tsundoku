package com.github.badbadbadbadbad.tsundoku.enums.anime;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static java.util.stream.Collectors.toList;

public enum ReleaseStatus {
    ANY("Any"), //
    COMPLETE("Complete"), //
    AIRING("Airing"), //
    UPCOMING("Upcoming"), //
    NOT_YET_PROVIDED("Not yet provided"), //
    ;

    private static final EnumSet<ReleaseStatus> FOR_FILTER =
            EnumSet.of(ANY, COMPLETE, AIRING, UPCOMING);
    private final @NotNull String releaseStatus;

    ReleaseStatus(@NotNull String releaseStatus) {
        this.releaseStatus = releaseStatus;
    }

    public static @NotNull List<String> filterValues() {
        return FOR_FILTER.stream().map(ReleaseStatus::toDisplay).collect(toList());
    }

    public static @NotNull List<String> displayValues() {
        return Arrays.stream(values())
                .map(ReleaseStatus::toDisplay)
                .toList();
    }

    public static ReleaseStatus fromDisplay(String string) {
        for (var v : values()) if (v.releaseStatus.equals(string)) return v;
        throw new IllegalArgumentException("Value not in enum: " + string);
    }

    public @NotNull String toDisplay() {
        return releaseStatus;
    }
}