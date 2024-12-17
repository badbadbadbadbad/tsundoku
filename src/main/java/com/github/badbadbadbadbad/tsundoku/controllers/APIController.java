package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Serves as a connector between any class trying to call an API service and that API service.
 */
public class APIController implements ConfigListener, APIRequestListener {
    private final AnimeAPIModel animeAPIModel;

    /**
     * Constructor.
     * @param animeAPIModel The API used for anime data.
     * @param configModel The model taking care of settings. APIController receives updated data
     *                    from here on settings changes.
     */
    public APIController(AnimeAPIModel animeAPIModel, ConfigModel configModel) {
        this.animeAPIModel = animeAPIModel;
        configModel.addConfigListener(this);
    }

    /**
     * Gets data on anime for the current season. Runs async.
     * @param page The API we currently use uses a "page" parameter with a hardcoded amount of anime per page. This specifies the page to get.
     * @return A list of retrieved anime data, and the page number that was called.
     */
    @Override
    public CompletableFuture<AnimeListInfo> getCurrentAnimeSeason(int page) {
        return animeAPIModel.getCurrentSeason(page);
    }

    /**
     * Gets data on anime for the upcoming seasons. Runs async.
     * @param page The API we currently use uses a "page" parameter with a hardcoded amount of anime per page. This specifies the page to get.
     * @return A list of retrieved anime data, and the page number that was called.
     */
    @Override
    public CompletableFuture<AnimeListInfo> getUpcomingAnime(int page) {
        return animeAPIModel.getUpcoming(page);
    }


    /**
     * Gets data on top rated anime. Runs async.
     * @param page The API we currently use uses a "page" parameter with a hardcoded amount of anime per page. This specifies the page to get.
     * @return A list of retrieved anime data, and the page number that was called.
     */
    @Override
    public CompletableFuture<AnimeListInfo> getTopAnime(int page) {
        return animeAPIModel.getTop(page);
    }


    /**
     * Gets data on anime based on a search query. Runs async.
     * @param query The String search query used for the API call.
     * @param page The API we currently use uses a "page" parameter with a hardcoded amount of anime per page. This specifies the page to get.
     * @return A list of retrieved anime data, and the page number that was called.
     */
    @Override
    public CompletableFuture<AnimeListInfo> getAnimeSearch(String query, int page) {
        return animeAPIModel.getSearchByName(query, page);
    }


    /**
     * Gets data on a single anime based on its MyAnimeList ID. Runs async.
     * @param id The ID used internally by MyAnimeList (and hence, the API we use).
     * @return The retrieved anime data.
     */
    @Override
    public CompletableFuture<AnimeInfo> getAnimeByID(int id) {
        return animeAPIModel.getAnimeByID(id);
    }


    /**
     * Updates specific filters in the anime API model when they are changed in the settings.
     * @param animeTypeFilters A collection of boolean filters (show / don't) for types of anime (TV, OVA, Movie..)
     * @param animeRatingFilters A collection of boolean filters (show / don't) for age ratings of anime (G, PG, R17+..)
     */
    @Override
    public void onAnimeTypeAndRatingFiltersUpdated(Map<String, Boolean> animeTypeFilters, Map<String, Boolean> animeRatingFilters) {
        animeAPIModel.setTypeFilters(animeTypeFilters);
        animeAPIModel.setRatingFilters(animeRatingFilters);
    }

    /**
     * Updates specific filters in the anime API model when they are changed in the anime Browse view.
     * @param animeOrderBy Sort function used internally by the Anime API we use.
     * @param animeStatus Anime completion status filter for API calls (Any, In Progress, Completed..)
     * @param animeStartYear Anime start year filter for API calls.
     * @param animeEndYear Anime end year filter for API calls.
     */
    @Override
    public void onAnimeSearchFiltersUpdated(String animeOrderBy, String animeStatus, String animeStartYear, String animeEndYear) {
        animeAPIModel.setSearchFilters(animeOrderBy, animeStatus, animeStartYear, animeEndYear);
    }


}
