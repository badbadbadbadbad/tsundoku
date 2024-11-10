package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;

public interface DatabaseRequestListener {
    AnimeInfo requestAnimeFromDatabase(int id);
    void onAnimeSaveButtonPressed(AnimeInfo animeInfo);
}
