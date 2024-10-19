package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.AnimeAPIModel;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import com.github.badbadbadbadbad.tsundoku.models.AnimeListInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class APIController {
    private final AnimeAPIModel animeAPIModel;

    public APIController(AnimeAPIModel animeAPIModel) {
        this.animeAPIModel = animeAPIModel;
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
}
