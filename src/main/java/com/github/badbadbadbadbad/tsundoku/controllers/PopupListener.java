package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;

public interface PopupListener {
    AnimeInfo requestAnimeFromDatabase(int id);
    void onAnimeSaveButtonPressed(AnimeInfo animeInfo);
}
