package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.AnimeListInfo;

import java.util.concurrent.CompletableFuture;

public interface APIRequestListener {
    CompletableFuture<AnimeListInfo> getCurrentAnimeSeason(int page);
    CompletableFuture<AnimeListInfo> getTopAnime(int page);
    CompletableFuture<AnimeListInfo> getAnimeSearch(String query, int page);
}
