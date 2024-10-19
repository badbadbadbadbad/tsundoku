package com.github.badbadbadbadbad.tsundoku.models;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ConfigModel {
    private static final String appName = "tsundoku";
    private String configFilePath;

    private String igdbSecret;
    private String mangadexSecret;
    private String profiles;
    private Map<String, Boolean> animeTypeFilters;
    private Map<String, Boolean> animeRatingFilters;

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

                this.animeTypeFilters = (Map<String, Boolean>) settings.get("animeTypeFilters");
                this.animeRatingFilters = (Map<String, Boolean>) settings.get("animeRatingFilters");
            } catch (IOException e) {
                System.exit(1);
            }
        } else {
            System.exit(1);
        }
    }
}
