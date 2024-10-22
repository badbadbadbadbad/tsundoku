package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class APIController implements ConfigChangeListener {
    private final AnimeAPIModel animeAPIModel;
    private final ConfigModel configModel;

    public APIController(AnimeAPIModel animeAPIModel, ConfigModel configModel) {
        this.animeAPIModel = animeAPIModel;
        this.configModel = configModel;

        configModel.addConfigChangeListener(this);
    }

    public CompletableFuture<AnimeListInfo> getCurrentAnimeSeason(int page) {
        return animeAPIModel.getCurrentSeason(page);
    }

    public CompletableFuture<AnimeListInfo> getTopAnime(int page) {
        return animeAPIModel.getTop(page);
    }

    public CompletableFuture<AnimeListInfo> getAnimeSearch(String query, int page) {
        return animeAPIModel.getSearchByName(query, page);
    }

    @Override
    public void onAnimeTypeAndRatingFiltersUpdated(Map<String, Boolean> animeTypeFilters, Map<String, Boolean> animeRatingFilters) {
        animeAPIModel.setTypeFilters(animeTypeFilters);
        animeAPIModel.setRatingFilters(animeRatingFilters);
    }

    @Override
    public void onAnimeSearchFiltersUpdates(String animeOrderBy, String animeStatus, String animeStartYear, String animeEndYear) {
        animeAPIModel.setSearchFilters(animeOrderBy, animeStatus, animeStartYear, animeEndYear);
    }


}
