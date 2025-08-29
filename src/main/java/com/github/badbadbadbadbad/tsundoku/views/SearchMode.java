package com.github.badbadbadbadbad.tsundoku.views;

import org.jetbrains.annotations.NotNull;

public enum SearchMode {
    SEASON("Season"), //
    UPCOMING("Upcoming"), //
    TOP("Top"), //
    SEARCH("Search"), //
    ;

    private final @NotNull String label;

    SearchMode(@NotNull String label) {
        this.label = label;
    }

    public @NotNull String getLabel() {
        return label;
    }
}
