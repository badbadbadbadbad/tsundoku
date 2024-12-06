package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.ConfigModel;

import java.util.Map;

public class ConfigController implements GridFilterListener, SidebarListener, SettingsListener {
    private final ConfigModel configModel;

    public ConfigController(ConfigModel configModel) {
        this.configModel = configModel;
    }

    public Map<String, Object> getCurrentSettings() {return configModel.getCurrentSettings();}

    @Override
    public void onSidebarMediaModeChanged(String mode) {
        configModel.setSidebarMediaMode(mode);
    }

    @Override
    public void onSidebarBrowseModeChanged(String mode) {
        configModel.setSidebarBrowseMode(mode);
    }

    @Override
    public void onSettingsChanged(Map<String, Object> settings) {configModel.updateSettings(settings);}

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
