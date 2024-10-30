package com.github.badbadbadbadbad.tsundoku.models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.badbadbadbadbad.tsundoku.controllers.ConfigListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigModel {
    private static final String appName = "tsundoku";
    private final String configFilePath;

    private final List<ConfigListener> listeners = new ArrayList<>();

    private String igdbSecret;
    private String mangadexSecret;

    private String profiles;

    // Sidebar listener data
    private String lastMediaMode;
    private String lastBrowseMode;

    // Anime grid listener
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


                updateSidebarModes((String) settings.get("lastMediaMode"), (String) settings.get("lastBrowseMode"));
                updateAnimeFilters((Map<String, Boolean>) settings.get("animeTypeFilters"), (Map<String, Boolean>) settings.get("animeRatingFilters"));
                updateAnimeSearchFilters((Map<String, String>) settings.get("animeSearchFilters"));
            } catch (IOException e) {
                System.exit(1);
            }
        } else {
            System.exit(1);
        }
    }


    private void saveConfigFile() {
        File configFile = new File(configFilePath);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> settings = new HashMap<>();

            settings.put("igdbSecret", this.igdbSecret);
            settings.put("mangadexSecret", this.mangadexSecret);
            settings.put("profiles", this.profiles);
            settings.put("lastMediaMode", this.lastMediaMode);
            settings.put("lastBrowseMode", this.lastBrowseMode);

            settings.put("animeTypeFilters", this.animeTypeFilters);
            settings.put("animeRatingFilters", this.animeRatingFilters);

            Map<String, String> animeSearchFilters = new HashMap<>();
            animeSearchFilters.put("Order by", this.animeOrderBy);
            animeSearchFilters.put("Status", this.animeStatus);
            animeSearchFilters.put("Start year", this.animeStartYear);
            animeSearchFilters.put("End year", this.animeEndYear);
            settings.put("animeSearchFilters", animeSearchFilters);

            objectMapper.writeValue(configFile, settings);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setSidebarMediaMode(String mode) {
        this.lastMediaMode = mode;
        notifyListenersSidebarChange();
        saveConfigFile();
    }

    public void setSidebarBrowseMode(String mode) {
        this.lastBrowseMode = mode;
        notifyListenersSidebarChange();
        saveConfigFile();
    }

    public void setAnimeOrderBy(String orderBy) {
        this.animeOrderBy = orderBy;
        notifyListenersAPIChange();
        saveConfigFile();
    }

    public void setAnimeStatus(String status) {
        this.animeStatus = status;
        notifyListenersAPIChange();
    }

    public void setAnimeStartYear(String startYear) {
        this.animeStartYear = startYear;
        notifyListenersAPIChange();
    }

    public void setAnimeEndYear(String endYear) {
        this.animeEndYear = endYear;
        notifyListenersAPIChange();
    }

    public void updateAnimeFilters(Map<String, Boolean> newTypeFilters, Map<String, Boolean> newRatingFilters) {
        this.animeTypeFilters = newTypeFilters;
        this.animeRatingFilters = newRatingFilters;
        notifyListenersAPIChange();
    }


    public void updateAnimeSearchFilters(Map<String, String> newSearchFilters) {
        this.animeOrderBy = newSearchFilters.get("Order by");
        this.animeStatus = newSearchFilters.get("Status");
        this.animeStartYear = newSearchFilters.get("Start year");
        this.animeEndYear = newSearchFilters.get("End year");
        notifyListenersAPIChange();
    }

    public void updateSidebarModes(String mediaMode, String browseMode) {
        this.lastMediaMode = mediaMode;
        this.lastBrowseMode = browseMode;
        notifyListenersSidebarChange();
    }

    public void addConfigListener(ConfigListener listener) {
        listeners.add(listener);
        notifyListenersAPIChange();
        notifyListenersSidebarChange();
    }

    public void removeConfigListener(ConfigListener listener) {
        listeners.remove(listener);
    }


    private void notifyListenersAPIChange() {
        for (ConfigListener listener : listeners) {
            listener.onAnimeTypeAndRatingFiltersUpdated(animeTypeFilters, animeRatingFilters);
            listener.onAnimeSearchFiltersUpdated(animeOrderBy, animeStatus, animeStartYear, animeEndYear);
        }
    }

    private void notifyListenersSidebarChange() {
        for (ConfigListener listener : listeners) {
            listener.onSidebarModesUpdated(lastMediaMode, lastBrowseMode);
        }
    }
}
