package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class APIController implements ConfigListener, APIRequestListener {
    private final AnimeAPIModel animeAPIModel;

    public APIController(AnimeAPIModel animeAPIModel, ConfigModel configModel) {
        this.animeAPIModel = animeAPIModel;
        configModel.addConfigListener(this);
    }


    @Override
    public CompletableFuture<AnimeListInfo> getCurrentAnimeSeason(int page) {
        return animeAPIModel.getCurrentSeason(page);
    }

    @Override
    public CompletableFuture<AnimeListInfo> getTopAnime(int page) {
        return animeAPIModel.getTop(page);
    }

    @Override
    public CompletableFuture<AnimeListInfo> getAnimeSearch(String query, int page) {
        return animeAPIModel.getSearchByName(query, page);
    }

    @Override
    public void onAnimeTypeAndRatingFiltersUpdated(Map<String, Boolean> animeTypeFilters, Map<String, Boolean> animeRatingFilters) {
        animeAPIModel.setTypeFilters(animeTypeFilters);
        animeAPIModel.setRatingFilters(animeRatingFilters);
    }

    @Override
    public void onAnimeSearchFiltersUpdated(String animeOrderBy, String animeStatus, String animeStartYear, String animeEndYear) {
        animeAPIModel.setSearchFilters(animeOrderBy, animeStatus, animeStartYear, animeEndYear);
    }


}
