package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.ConfigModel;

import java.util.Map;

/**
 * Serves as a connector between views changing settings, and the config storing all settings.
 */
public class ConfigController implements GridFilterListener, SidebarListener, SettingsListener {
    private final ConfigModel configModel;

    public ConfigController(ConfigModel configModel) {
        this.configModel = configModel;
    }

    /**
     * Getter for full collection of current settings. Used to initialize the SettingsView.
     * @return The full collection of current settings.
     */
    public Map<String, Object> getCurrentSettings() {return configModel.getCurrentSettings();}

    /**
     * Invoked when the sidebar's media mode (Anime, Games, Manga..) is changed.
     * @param mode The String name of the new mode.
     */
    @Override
    public void onSidebarMediaModeChanged(String mode) {
        configModel.setSidebarMediaMode(mode);
    }

    /**
     * Invoked when the sidebar's browse mode (Browse / Log) is changed.
     * @param mode The String name of the new mode.
     */
    @Override
    public void onSidebarBrowseModeChanged(String mode) {
        configModel.setSidebarBrowseMode(mode);
    }

    /**
     * Invoked when settings are changed in the settings view.
     * @param settings The collection of new values to use for settings.
     */
    @Override
    public void onSettingsChanged(Map<String, Object> settings) {configModel.updateSettings(settings);}

    /**
     * Invoked when "Order by" setting in Anime Browse view is changed. Forwards it to config model.
     * @param orderBy The new value of the "Order by" setting.
     */
    @Override
    public void onAnimeOrderByChanged(String orderBy) {
        configModel.setAnimeOrderBy(orderBy);
    }

    /**
     * Invoked when "Status" setting in Anime Browse view is changed. Forwards it to config model.
     * @param status The new value of the "Status" setting.
     */
    @Override
    public void onAnimeStatusChanged(String status) {
        configModel.setAnimeStatus(status);
    }

    /**
     * Invoked when "Start Year" setting in Anime Browse view is changed. Forwards it to config model.
     * @param startYear The new value of the "Start Year" setting.
     */
    @Override
    public void onAnimeStartYearChanged(String startYear) {
        configModel.setAnimeStartYear(startYear);
    }

    /**
     * Invoked when "End Year" setting in Anime Browse view is changed. Forwards it to config model.
     * @param endYear The new value of the "End Year" setting.
     */
    @Override
    public void onAnimeEndYearChanged(String endYear) {
        configModel.setAnimeEndYear(endYear);
    }

    @Override
    public String getAnimeOrderByDefault() {
        return configModel.getAnimeOrderBy();
    }

    @Override
    public String getAnimeStatusDefault() {
        return configModel.getAnimeStatus();
    }

    @Override
    public String getAnimeStartYearDefault() {
        return configModel.getAnimeStartYear();
    }

    @Override
    public String getAnimeEndYearDefault() {
        return configModel.getAnimeEndYear();
    }
}
