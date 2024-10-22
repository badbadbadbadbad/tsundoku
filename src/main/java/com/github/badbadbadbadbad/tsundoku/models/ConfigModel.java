package com.github.badbadbadbadbad.tsundoku.models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.badbadbadbadbad.tsundoku.controllers.APIController;
import com.github.badbadbadbadbad.tsundoku.controllers.ConfigListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigModel {
    private static final String appName = "tsundoku";
    private final String configFilePath;

    private final List<ConfigListener> listeners = new ArrayList<>();
    private APIController apiController;

    private String igdbSecret;
    private String mangadexSecret;

    private String profiles;

    private Map<String, Boolean> animeTypeFilters;
    private Map<String, Boolean> animeRatingFilters;
    private String animeOrderBy;
    private String animeStatus;
    private String animeStartYear;
    private String animeEndYear;

    public ConfigModel() {
        this.configFilePath = Paths.get(getAppDataPath(), "config.json").toString();
        readConfigFile();
    }

    private String getAppDataPath() {
        String homeDir = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return System.getenv("LOCALAPPDATA") + "\\" + appName;
        } else if (os.contains("mac")) {
            System.exit(1); // No mac support for now
            return null; // shut up intellij
        } else {
            return homeDir + "/.local/share/" + appName;
        }
    }

    private void readConfigFile() {
        File configFile = new File(configFilePath);
        if (configFile.exists()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> settings = objectMapper.readValue(configFile, new TypeReference<Map<String, Object>>() {
                });

                this.igdbSecret = (String) settings.get("igdbSecret");
                this.mangadexSecret = (String) settings.get("mangadexSecret");
                this.profiles = (String) settings.get("profiles");

                updateAnimeFilters((Map<String, Boolean>) settings.get("animeTypeFilters"), (Map<String, Boolean>) settings.get("animeRatingFilters"));
                updateAnimeSearchFilters((Map<String, String>) settings.get("animeSearchFilters"));
            } catch (IOException e) {
                System.exit(1);
            }
        } else {
            System.exit(1);
        }
    }


    public void setAnimeOrderBy(String orderBy) {
        this.animeOrderBy = orderBy;
        notifyListeners();
    }

    public void setAnimeStatus(String status) {
        this.animeStatus = status;
        notifyListeners();
    }

    public void setAnimeStartYear(String startYear) {
        this.animeStartYear = startYear;
        notifyListeners();
    }

    public void setAnimeEndYear(String endYear) {
        this.animeEndYear = endYear;
        notifyListeners();
    }

    public void updateAnimeFilters(Map<String, Boolean> newTypeFilters, Map<String, Boolean> newRatingFilters) {
        this.animeTypeFilters = newTypeFilters;
        this.animeRatingFilters = newRatingFilters;
        notifyListeners();
    }


    public void updateAnimeSearchFilters(Map<String, String> newSearchFilters) {
        this.animeOrderBy = newSearchFilters.get("Order by");
        this.animeStatus = newSearchFilters.get("Status");
        this.animeStartYear = newSearchFilters.get("Start year");
        this.animeEndYear = newSearchFilters.get("End year");
        notifyListeners();
    }

    public void addConfigListener(ConfigListener listener) {
        listeners.add(listener);
        notifyListeners(); // So new listeners get current config data on creation. Kinda sloppy.
    }

    public void removeConfigListener(ConfigListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (ConfigListener listener : listeners) {
            listener.onAnimeTypeAndRatingFiltersUpdated(animeTypeFilters, animeRatingFilters);
            listener.onAnimeSearchFiltersUpdates(animeOrderBy, animeStatus, animeStartYear, animeEndYear);
        }
    }
}
