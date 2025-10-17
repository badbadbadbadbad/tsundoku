package com.github.badbadbadbadbad.tsundoku.enums.anime;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public enum OrderBy {
    DEFAULT("Default"), //
    TITLE_ASC("Title: Ascending"), //
    TITLE_DESC("Title: Descending"), //
    RATING_HIGH("Rating: Highest"), //
    RATING_LOW("Rating: Lowest"), //
    POPULAR_MOST("Popular: Most"), //
    POPULAR_LEAST("Popular: Least"), //
    ;

    private final @NotNull String order;

    OrderBy(@NotNull String order) {
        this.order = order;
    }

    public static @NotNull List<String> displayValues() {
        return Arrays.stream(values())
                .map(OrderBy::toDisplay)
                .toList();
    }

    public static OrderBy fromDisplay(String string) {
        for (var v : values()) if (v.order.equals(string)) return v;
        throw new IllegalArgumentException("Value not in enum: " + string);
    }

    public @NotNull String toDisplay() {
        return order;
    }
}