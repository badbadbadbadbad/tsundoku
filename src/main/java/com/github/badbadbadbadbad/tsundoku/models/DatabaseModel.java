package com.github.badbadbadbadbad.tsundoku.models;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseModel {
    private static final String appName = "tsundoku";
    private final String databaseFilePath;

    public DatabaseModel() {
        this.databaseFilePath = Paths.get(getAppDataPath(), "profiles", "Default.db").toString();
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

    public void updateAnimeDatabaseWithEntry(AnimeInfo anime) {
        String url = "jdbc:sqlite:" + databaseFilePath;

        if (anime.getOwnStatus().equals("Completed")) {
            anime.setEpisodesProgress(anime.getEpisodesTotal());
        }

        String sqlInsert = "INSERT INTO anime (id, ownRating, ownStatus, episodesProgress, title, titleJapanese, titleEnglish, imageUrl, " +
                "publicationStatus, episodesTotal, source, ageRating, synopsis, release, studios, type, lastUpdate) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {

            pstmt.setInt(1, anime.getId());
            pstmt.setString(2, anime.getOwnRating());
            pstmt.setString(3, anime.getOwnStatus());
            pstmt.setInt(4, anime.getEpisodesProgress());
            pstmt.setString(5, anime.getTitle());
            pstmt.setString(6, anime.getTitleJapanese());
            pstmt.setString(7, anime.getTitleEnglish());
            pstmt.setString(8, anime.getImageUrl());
            pstmt.setString(9, anime.getPublicationStatus());
            pstmt.setInt(10, anime.getEpisodesTotal());
            pstmt.setString(11, anime.getSource());
            pstmt.setString(12, anime.getAgeRating());
            pstmt.setString(13, anime.getSynopsis());
            pstmt.setString(14, anime.getRelease());
            pstmt.setString(15, anime.getStudios());
            pstmt.setString(16, anime.getType());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }


        /*
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:anime.db");
             PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM anime WHERE id = ?");
             PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO anime (id, ownRating, ownStatus, episodesProgress, title, titleJapanese, titleEnglish, imageUrl, publicationStatus, episodesTotal, source, ageRating, synopsis, release, studios, type, lastUpdate) "
                     + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)");
             PreparedStatement updateStmt = conn.prepareStatement("UPDATE anime SET ownRating = ?, ownStatus = ?, episodesProgress = ?, title = ?, titleJapanese = ?, titleEnglish = ?, imageUrl = ?, publicationStatus = ?, episodesTotal = ?, source = ?, ageRating = ?, synopsis = ?, release = ?, studios = ?, type = ?, lastUpdate = CURRENT_DATE WHERE id = ?")) {

            // Handle "Untracked" status - delete if exists
            if ("Untracked".equals(anime.getOwnStatus())) {
                deleteStmt.setInt(1, anime.getId());
                deleteStmt.executeUpdate();
                return;
            }

            // Set episodesProgress to total if "Completed"
            if ("Completed".equals(anime.getOwnStatus())) {
                anime.setEpisodesProgress(Integer.parseInt(anime.getEpisodesTotal()));
            }

            // Check if the anime already exists
            try (PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM anime WHERE id = ?")) {
                checkStmt.setInt(1, anime.getId());
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {  // Entry exists, so update
                    updateStmt.setString(1, anime.getOwnRating());
                    updateStmt.setString(2, anime.getOwnStatus());
                    updateStmt.setInt(3, anime.getEpisodesProgress());
                    updateStmt.setString(4, anime.getTitle());
                    updateStmt.setString(5, anime.getTitleJapanese());
                    updateStmt.setString(6, anime.getTitleEnglish());
                    updateStmt.setString(7, anime.getImageUrl());
                    updateStmt.setString(8, anime.getPublicationStatus());
                    updateStmt.setString(9, anime.getEpisodesTotal());
                    updateStmt.setString(10, anime.getSource());
                    updateStmt.setString(11, anime.getAgeRating());
                    updateStmt.setString(12, anime.getSynopsis());
                    updateStmt.setString(13, anime.getRelease());
                    updateStmt.setString(14, anime.getStudios());
                    updateStmt.setString(15, anime.getType());
                    updateStmt.setInt(16, anime.getId());
                    updateStmt.executeUpdate();
                } else {  // Entry doesn't exist, so insert
                    insertStmt.setInt(1, anime.getId());
                    insertStmt.setString(2, anime.getOwnRating());
                    insertStmt.setString(3, anime.getOwnStatus());
                    insertStmt.setInt(4, anime.getEpisodesProgress());
                    insertStmt.setString(5, anime.getTitle());
                    insertStmt.setString(6, anime.getTitleJapanese());
                    insertStmt.setString(7, anime.getTitleEnglish());
                    insertStmt.setString(8, anime.getImageUrl());
                    insertStmt.setString(9, anime.getPublicationStatus());
                    insertStmt.setString(10, anime.getEpisodesTotal());
                    insertStmt.setString(11, anime.getSource());
                    insertStmt.setString(12, anime.getAgeRating());
                    insertStmt.setString(13, anime.getSynopsis());
                    insertStmt.setString(14, anime.getRelease());
                    insertStmt.setString(15, anime.getStudios());
                    insertStmt.setString(16, anime.getType());
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

         */
    }
}
