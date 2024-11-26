package com.github.badbadbadbadbad.tsundoku;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Initializer {
    private static final String appName = "tsundoku";
    private static String appDataPath;

    public static void init() throws IOException {
        verifyFileIntegrity();
    }

    private static void verifyFileIntegrity() throws IOException {
        getFolderLocation();
        createProgramFolder();
        createSettingsFile();
        createDefaultProfile();
    }

    private static void getFolderLocation() {
        String homeDir  = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows: AppData\Local
            appDataPath = System.getenv("LOCALAPPDATA") + "\\" + appName;
        } else if (os.contains("mac")) {
            // No mac support for now
            System.exit(1);
        } else {
            // Linux: ~/.local/share
            appDataPath = homeDir + "/.local/share/" + appName;
        }
    }

    private static void createProgramFolder() throws IOException {
        Path path = Paths.get(appDataPath);
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createSettingsFile() throws IOException {
        String settingsFilePath = Paths.get(appDataPath, "config.json").toString();
        File settingsFile = new File(settingsFilePath);

        if (!settingsFile.exists()) {
            // Test settings for now. Do this properly when we know what settings are needed.
            Map<String, Object> defaultSettings = new HashMap<>();

            defaultSettings.put("igdbSecret", null);
            defaultSettings.put("mangadexSecret", null);
            defaultSettings.put("profiles", "Default");
            defaultSettings.put("lastMediaMode", "Anime");
            defaultSettings.put("lastBrowseMode", "Browse");

            // Anime types
            Map<String, Boolean> animeTypeFilters = new HashMap<>();
            animeTypeFilters.put("TV", true);
            animeTypeFilters.put("Movie", true);
            animeTypeFilters.put("OVA", true);
            animeTypeFilters.put("Special", true);
            animeTypeFilters.put("ONA", true);
            animeTypeFilters.put("Music", false);
            animeTypeFilters.put("CM", false);
            animeTypeFilters.put("PV", false);
            animeTypeFilters.put("TV Special", false);
            animeTypeFilters.put("Not yet provided", false);

            // Anime age ratings
            Map<String, Boolean> animeRatingFilters = new HashMap<>();
            animeRatingFilters.put("G", true);
            animeRatingFilters.put("PG", true);
            animeRatingFilters.put("PG13", true);
            animeRatingFilters.put("R17+", true);
            animeRatingFilters.put("R+", false);
            animeRatingFilters.put("Rx", false);
            animeRatingFilters.put("Not yet provided", true);

            // Anime search filters
            Map<String, String> animeSearchFilters = new HashMap<>();
            animeSearchFilters.put("Order by", "Default");
            animeSearchFilters.put("Status", "Any");
            animeSearchFilters.put("Start year", "");
            animeSearchFilters.put("End year", "");


            defaultSettings.put("animeTypeFilters", animeTypeFilters);
            defaultSettings.put("animeRatingFilters", animeRatingFilters);
            defaultSettings.put("animeSearchFilters", animeSearchFilters);

            defaultSettings.put("weebLanguagePreference", "Default");

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(settingsFile, defaultSettings);
        }
    }

    private static void createDefaultProfile() throws IOException {
        Path profilesDir = Paths.get(appDataPath, "profiles");
        if (Files.exists(profilesDir)) {
            return;
        }

        Files.createDirectories(profilesDir);
        String databaseFilePath = Paths.get(appDataPath, "profiles", "Default.db").toString();

        // DB stuff
        String url = "jdbc:sqlite:" + databaseFilePath;

        String sqlCreateGamesTable = "CREATE TABLE IF NOT EXISTS games ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "status TEXT NOT NULL"
                + ");";

        String sqlCreateMangaTable = "CREATE TABLE IF NOT EXISTS manga ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "status TEXT NOT NULL"
                + ");";

        String sqlCreateAnimeTable = "CREATE TABLE IF NOT EXISTS anime ("
                + "id INTEGER PRIMARY KEY, "
                + "ownRating TEXT, "
                + "ownStatus TEXT, "
                + "episodesProgress INTEGER, "
                + "title TEXT, "
                + "titleJapanese TEXT, "
                + "titleEnglish TEXT, "
                + "imageUrl TEXT, "
                + "smallImageUrl TEXT, "
                + "publicationStatus TEXT, "
                + "episodesTotal INTEGER, "
                + "source TEXT, "
                + "ageRating TEXT, "
                + "synopsis TEXT, "
                + "release TEXT, "
                + "studios TEXT, "
                + "type TEXT, "
                + "lastUpdate TEXT DEFAULT CURRENT_DATE"
                + ");";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlCreateGamesTable);
            stmt.execute(sqlCreateMangaTable);
            stmt.execute(sqlCreateAnimeTable);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
