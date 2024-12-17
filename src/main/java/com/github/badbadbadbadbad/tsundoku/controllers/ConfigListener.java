package com.github.badbadbadbadbad.tsundoku.controllers;

import java.util.Map;

/**
 * Describes how updates in settings in ConfigModel are forwarded to things dependent on those settings.
 * All functions are set to an empty default because not every listener uses every function here.
 */
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