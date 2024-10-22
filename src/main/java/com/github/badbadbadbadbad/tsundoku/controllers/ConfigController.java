package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.ConfigModel;

public class ConfigController {
    private final ConfigModel configModel;

    public ConfigController(ConfigModel configModel) {
        this.configModel = configModel;
    }

    public void onAnimeOrderByChanged(String orderBy) {
        configModel.setAnimeOrderBy(orderBy);
    }

    public void onAnimeStatusChanged(String status) {
        configModel.setAnimeStatus(status);
    }

    public void onAnimeStartYearChanged(String startYear) {
        configModel.setAnimeStartYear(startYear);
    }

    public void onAnimeEndYearChanged(String endYear) {
        configModel.setAnimeEndYear(endYear);
    }
}
