package com.github.badbadbadbadbad.tsundoku.models;

import java.util.Map;

public interface ConfigChangeListener {
    void onAnimeTypeAndRatingFiltersUpdated(Map<String, Boolean> animeTypeFilters, Map<String, Boolean> animeRatingFilters);
    void onAnimeSearchFiltersUpdates(String animeOrderBy, String animeStatus, String animeStartYear, String animeEndYear);
}