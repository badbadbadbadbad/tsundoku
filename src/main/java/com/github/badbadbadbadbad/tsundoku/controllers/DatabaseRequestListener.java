package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import com.github.badbadbadbadbad.tsundoku.models.AnimeListInfo;

/**
 * Describes the functions for local database reads / writes.
 */
public interface DatabaseRequestListener {
    AnimeInfo requestAnimeFromDatabase(int id);
    AnimeListInfo requestFullAnimeDatabase();
    void onAnimeSaveButtonPressed(AnimeInfo animeInfo);
}
