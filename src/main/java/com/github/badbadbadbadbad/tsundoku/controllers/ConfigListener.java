package com.github.badbadbadbadbad.tsundoku.controllers;

import java.util.Map;

public interface ConfigListener {
    default void onAnimeTypeAndRatingFiltersUpdated(Map<String, Boolean> animeTypeFilters, Map<String, Boolean> animeRatingFilters) {

    }

    default void onAnimeSearchFiltersUpdated(String animeOrderBy, String animeStatus, String animeStartYear, String animeEndYear) {

    }

    default void onSidebarModesUpdated(String mediaMode, String browseMode) {

    }

    default void onLanguagePreferenceUpdated(String language) {

    }
}