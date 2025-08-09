package com.github.badbadbadbadbad.tsundoku.views;

public enum SearchMode {
    SEASON("Season"), //
    UPCOMING("Upcoming"), //
    TOP("Top"), //
    SEARCH("Search"), //
    ;

    private final String label;

    SearchMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
