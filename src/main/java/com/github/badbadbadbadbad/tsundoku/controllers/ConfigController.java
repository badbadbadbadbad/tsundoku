package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.ConfigModel;
import com.github.badbadbadbadbad.tsundoku.views.AnimeGridView;

public class ConfigController implements GridFilterListener {
    private final ConfigModel configModel;

    public ConfigController(ConfigModel configModel) {
        this.configModel = configModel;
    }

    public void listenToAnimeGrid(AnimeGridView animeGridView) {
        animeGridView.addGridFilterListener(this);
    }

    @Override
    public void onAnimeOrderByChanged(String orderBy) {
        configModel.setAnimeOrderBy(orderBy);
    }

    @Override
    public void onAnimeStatusChanged(String status) {
        configModel.setAnimeStatus(status);
    }

    @Override
    public void onAnimeStartYearChanged(String startYear) {
        configModel.setAnimeStartYear(startYear);
    }

    @Override
    public void onAnimeEndYearChanged(String endYear) {
        configModel.setAnimeEndYear(endYear);
    }
}
