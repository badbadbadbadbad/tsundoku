package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import com.github.badbadbadbadbad.tsundoku.models.AnimeListInfo;
import com.github.badbadbadbadbad.tsundoku.models.DatabaseModel;

/**
 * Serves as a connector between the local database and any service trying to read / write data.
 */
public class DatabaseController implements DatabaseRequestListener {

    private final DatabaseModel databaseModel;

    public DatabaseController(DatabaseModel databaseModel) {
        this.databaseModel = databaseModel;
    }

    /**
     * Reads data on a single anime from local database.
     * @param id MyAnimeList ID of the requested anime.
     * @return The full data of the requested anime.
     */
    @Override
    public AnimeInfo requestAnimeFromDatabase(int id) {
        return databaseModel.getAnimeEntryFromDatabase(id);
    }

    /**
     * Reads data on all anime from local database.
     * @return The full data of all locally saved anime.
     */
    @Override
    public AnimeListInfo requestFullAnimeDatabase() {
        return databaseModel.getFullAnimeDatabase();
    }

    /**
     * Writes data of a single anime to local database.
     * Implemented as an upsert: If data on this anime's ID already present, overwrite it.
     * @param animeInfo The full information to be saved for this anime.
     */
    @Override
    public void onAnimeSaveButtonPressed(AnimeInfo animeInfo) {
        databaseModel.updateAnimeDatabaseWithEntry(animeInfo);
    }
}

