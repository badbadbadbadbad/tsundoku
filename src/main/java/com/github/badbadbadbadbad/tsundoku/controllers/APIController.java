package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.AnimeAPIModel;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import com.github.badbadbadbadbad.tsundoku.models.AnimeListInfo;

import java.util.List;

public class APIController {
    private final AnimeAPIModel animeAPIModel;

    public APIController(AnimeAPIModel animeAPIModel) {
        this.animeAPIModel = animeAPIModel;
    }

    public AnimeListInfo getCurrentAnimeSeason(int page) {
        return animeAPIModel.getCurrentSeason(page);
    }

    public AnimeListInfo getTopAnime(int page) {
        return animeAPIModel.getTop(page);
    }

    public AnimeListInfo getAnimeSearch(String query, int page) {
        return animeAPIModel.getSearchByName(query, page);
    }
}
