package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;

public interface PopupListener {
    AnimeInfo onAnimePopupCreation();
    void onAnimeSaveButtonPressed(AnimeInfo animeInfo);
}
