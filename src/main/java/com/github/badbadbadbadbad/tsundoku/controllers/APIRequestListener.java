package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import com.github.badbadbadbadbad.tsundoku.models.AnimeListInfo;

import java.util.concurrent.CompletableFuture;

/**
 * Describes the API requests to be forwarded to API models.
 */
public interface APIRequestListener {
    CompletableFuture<AnimeListInfo> getCurrentAnimeSeason(int page);
    CompletableFuture<AnimeListInfo> getUpcomingAnime(int page);
    CompletableFuture<AnimeListInfo> getTopAnime(int page);
    CompletableFuture<AnimeListInfo> getAnimeSearch(String query, int page);
    CompletableFuture<AnimeInfo> getAnimeByID(int id);
}
