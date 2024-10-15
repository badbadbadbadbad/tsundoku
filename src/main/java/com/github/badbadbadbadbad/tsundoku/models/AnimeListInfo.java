package com.github.badbadbadbadbad.tsundoku.models;

import java.util.List;

public class AnimeListInfo {
    private final List<AnimeInfo> animeList;
    private final int lastPage;

    public AnimeListInfo(List<AnimeInfo> animeList, int lastPage) {
        this.animeList = animeList;
        this.lastPage = lastPage;
    }

    public List<AnimeInfo> getAnimeList() {
        return animeList;
    }

    public int getLastPage() {
        return lastPage;
    }
}
