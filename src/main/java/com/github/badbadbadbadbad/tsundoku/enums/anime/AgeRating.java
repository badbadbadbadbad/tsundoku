package com.github.badbadbadbadbad.tsundoku.enums.anime;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static java.util.stream.Collectors.toList;

public enum AgeRating {
    ANY("Any"), //
    G("G"), //
    PG("PG"), //
    PG13("PG13"), //
    R17("R17+"), //
    R("R+"), //
    RX("Rx"), //
    NOT_YET_PROVIDED("Not yet provided"), //
    ;

    private static final EnumSet<AgeRating> FOR_FILTER =
            EnumSet.of(ANY, G, PG, PG13, R17, R, RX);
    private final @NotNull String rating;

    AgeRating(@NotNull String rating) {
        this.rating = rating;
    }

    public static @NotNull List<String> filterValues() {
        return FOR_FILTER.stream().map(AgeRating::toDisplay).collect(toList());
    }

    public static @NotNull List<String> displayValues() {
        return Arrays.stream(values())
                .map(AgeRating::toDisplay)
                .toList();
    }

    public static AgeRating fromDisplay(String string) {
        for (var v : values()) if (v.rating.equals(string)) return v;
        throw new IllegalArgumentException("Value not in enum: " + string);
    }

    public @NotNull String toDisplay() {
        return rating;
    }
}