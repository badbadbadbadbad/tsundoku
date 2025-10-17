package com.github.badbadbadbadbad.tsundoku.enums;

import com.github.badbadbadbadbad.tsundoku.enums.anime.*;
import com.github.badbadbadbadbad.tsundoku.enums.generic.PersonalRating;
import com.github.badbadbadbadbad.tsundoku.enums.generic.PersonalStatus;
import com.github.badbadbadbadbad.tsundoku.views.MediaFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum AnimeFilters implements MediaFilter {
    ORDER_BY("Order by", OrderBy.displayValues()),
    RELEASE_STATUS("Release status", ReleaseStatus.filterValues()),
    YEAR_MIN("Year ≥", null),
    YEAR_MAX("Year ≤", null),
    PERSONAL_STATUS("Personal status", PersonalStatus.filterValues()),
    PERSONAL_RATING("Personal rating", PersonalRating.filterValues()),
    AGE_RATING("Age rating", AgeRating.filterValues()),
    SEASON("Season", Season.displayValues()),
    EPISODES_MIN("Episodes ≥", null),
    EPISODES_MAX("Episodes ≤", null),
    TYPE("Type", Type.filterValues()),
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