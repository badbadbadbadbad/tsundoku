package com.github.badbadbadbadbad.tsundoku.views;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum AnimeFilters implements MediaFilter {
    ORDER_BY("Order by", List.of(
            "Default",
            "Title: Ascending", "Title: Descending",
            "Rating: Highest", "Rating: Lowest",
            "Popular: Most", "Popular: Least"
    )),
    RELEASE_STATUS("Release status", List.of("Any", "Complete", "Airing", "Upcoming")),
    YEAR_MIN("Year ≥", null),
    YEAR_MAX("Year ≤", null),
    PERSONAL_STATUS("Personal status", List.of("Any", "In progress", "Backlog", "Completed", "Paused", "Dropped")),
    PERSONAL_RATING("Personal rating", List.of("Any", "Heart", "Liked", "Disliked", "Unscored")),
    AGE_RATING("Age rating", List.of("Any", "G", "PG", "PG13", "R17+", "R+", "Rx")),
    SEASON("Season", List.of("Any", "Winter", "Spring", "Summer", "Fall")),
    EPISODES_MIN("Episodes ≥", null),
    EPISODES_MAX("Episodes ≤", null),
    TYPE("Type", List.of("Any", "TV", "Movie", "OVA", "Special", "ONA", "Music", "CM", "PV", "TV Special")),
    ;

    private final @NotNull String label;
    private final @Nullable List<String> options;

    AnimeFilters(@NotNull String label, @Nullable List<String> options) {
        this.label = label;
        this.options = options;
    }

    @Override
    public @NotNull String getLabel() {
        return label;
    }

    @Override
    public @Nullable List<String> getOptions() {
        return options;
    }
}