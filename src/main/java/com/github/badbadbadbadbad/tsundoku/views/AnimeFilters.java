package com.github.badbadbadbadbad.tsundoku.views;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum AnimeFilters {
    ORDER_BY("Order by", List.of(
            "Default",
            "Title: Ascending", "Title: Descending",
            "Rating: Highest", "Rating: Lowest",
            "Popular: Most", "Popular: Least"
    )),

    RELEASE_STATUS("Release status", List.of(
            "Any", "Complete", "Airing", "Upcoming"
    )),

    YEAR_MIN("Year ≥", null),
    YEAR_MAX("Year ≤", null);

    private final @NotNull String label;
    private final @Nullable List<String> options;

    AnimeFilters(@NotNull String label, @Nullable List<String> options) {
        this.label = label;
        this.options = options;
    }

    public @NotNull String getLabel() {
        return label;
    }

    public boolean hasOptions() {
        return options != null;
    }

    public @Nullable List<String> getOptions() {
        return options;
    }
}