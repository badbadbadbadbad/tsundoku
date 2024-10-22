package com.github.badbadbadbadbad.tsundoku.controllers;

import java.util.Map;

public interface ConfigListener {
    void onAnimeTypeAndRatingFiltersUpdated(Map<String, Boolean> animeTypeFilters, Map<String, Boolean> animeRatingFilters);
    void onAnimeSearchFiltersUpdates(String animeOrderBy, String animeStatus, String animeStartYear, String animeEndYear);
}