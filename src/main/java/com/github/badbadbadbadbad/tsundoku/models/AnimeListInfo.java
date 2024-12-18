package com.github.badbadbadbadbad.tsundoku.models;

import java.util.List;

/**
 * A container object for a List of AnimeInfo objects.
 * This class exists (compared to just using a List of AnimeInfo) because the pagination information needs to be stored along with the list for some uses.
 */
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
