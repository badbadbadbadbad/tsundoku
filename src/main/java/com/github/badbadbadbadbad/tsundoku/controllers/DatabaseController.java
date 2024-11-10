package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import com.github.badbadbadbadbad.tsundoku.models.DatabaseModel;

public class DatabaseController implements DatabaseRequestListener {

    private final DatabaseModel databaseModel;

    public DatabaseController(DatabaseModel databaseModel) {
        this.databaseModel = databaseModel;
    }

    @Override
    public AnimeInfo requestAnimeFromDatabase(int id) {
        return databaseModel.getAnimeEntryFromDatabase(id);
    }

    @Override
    public void onAnimeSaveButtonPressed(AnimeInfo animeInfo) {
        databaseModel.updateAnimeDatabaseWithEntry(animeInfo);
    }
}

