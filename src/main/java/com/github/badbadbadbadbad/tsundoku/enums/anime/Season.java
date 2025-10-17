package com.github.badbadbadbadbad.tsundoku.enums.anime;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public enum Season {
    ANY("Any"), //
    WINTER("Winter"), //
    SPRING("Spring"), //
    SUMMER("Summer"), //
    FALL("Fall"), //
    ;

    private final @NotNull String season;

    Season(@NotNull String season) {
        this.season = season;
    }

    public static @NotNull List<String> displayValues() {
        return Arrays.stream(values())
                .map(Season::toDisplay)
                .toList();
    }

    public static Season fromDisplay(String string) {
        for (var v : values()) if (v.season.equals(string)) return v;
        throw new IllegalArgumentException("Value not in enum: " + string);
    }

    public @NotNull String toDisplay() {
        return season;
    }
}