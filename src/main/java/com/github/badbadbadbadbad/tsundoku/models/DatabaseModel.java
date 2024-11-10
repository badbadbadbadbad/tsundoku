package com.github.badbadbadbadbad.tsundoku.models;

import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

        String sqlDelete = "DELETE FROM anime WHERE id = ?";
        String sqlUpsert = """
        INSERT INTO anime (id, ownRating, ownStatus, episodesProgress, title, titleJapanese, titleEnglish, imageUrl,
                           publicationStatus, episodesTotal, source, ageRating, synopsis, release, studios, type, lastUpdate)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE)
        ON CONFLICT(id) DO UPDATE SET
            ownRating = excluded.ownRating,
            ownStatus = excluded.ownStatus,
            episodesProgress = excluded.episodesProgress,
            title = excluded.title,
            titleJapanese = excluded.titleJapanese,
            titleEnglish = excluded.titleEnglish,
            imageUrl = excluded.imageUrl,
            publicationStatus = excluded.publicationStatus,
            episodesTotal = excluded.episodesTotal,
            source = excluded.source,
            ageRating = excluded.ageRating,
            synopsis = excluded.synopsis,
            release = excluded.release,
            studios = excluded.studios,
            type = excluded.type,
            lastUpdate = CURRENT_DATE;""";

        try (Connection conn = DriverManager.getConnection(url)) {
            if (anime.getOwnStatus().equals("Untracked")) {
                try (PreparedStatement pstmt = conn.prepareStatement(sqlDelete)) {
                    pstmt.setInt(1, anime.getId());
                    pstmt.executeUpdate();
                }
            } else {
                try (PreparedStatement pstmt = conn.prepareStatement(sqlUpsert)) {
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
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    public AnimeInfo getAnimeEntryFromDatabase(int id) {
        String url = "jdbc:sqlite:" + databaseFilePath;
        String sqlSelect = "SELECT id, ownRating, ownStatus, episodesProgress, title, titleJapanese, titleEnglish, imageUrl, publicationStatus, "
                + "episodesTotal, source, ageRating, synopsis, release, studios, type FROM anime WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sqlSelect)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {

                AnimeInfo animeInfo = new AnimeInfo(rs.getInt("id"), rs.getString("title"), rs.getString("titleJapanese"), rs.getString("titleEnglish"),
                        rs.getString("imageUrl"), rs.getString("publicationStatus"), rs.getInt("episodesTotal"), rs.getString("source"),
                        rs.getString("ageRating"), rs.getString("synopsis"), rs.getString("release"), rs.getString("studios"), rs.getString("type")
                );
                animeInfo.setOwnRating(rs.getString("ownRating"));
                animeInfo.setOwnStatus(rs.getString("ownStatus"));
                animeInfo.setEpisodesProgress(rs.getInt("episodesProgress"));

                return animeInfo;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // If the requested ID isn't in the database, we return null. Is handled on the receiver end.
        return null;
    }


    public AnimeListInfo getFullAnimeDatabase() {
        String url = "jdbc:sqlite:" + databaseFilePath;
        String sqlSelectAll = "SELECT id, ownRating, ownStatus, episodesProgress, title, titleJapanese, titleEnglish, imageUrl, publicationStatus, "
                + "episodesTotal, source, ageRating, synopsis, release, studios, type FROM anime";
        List<AnimeInfo> animeList = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlSelectAll)) {

            while (rs.next()) {
                AnimeInfo animeInfo = new AnimeInfo(rs.getInt("id"), rs.getString("title"), rs.getString("titleJapanese"), rs.getString("titleEnglish"),
                        rs.getString("imageUrl"), rs.getString("publicationStatus"), rs.getInt("episodesTotal"), rs.getString("source"),
                        rs.getString("ageRating"), rs.getString("synopsis"), rs.getString("release"), rs.getString("studios"), rs.getString("type")
                );
                animeInfo.setOwnRating(rs.getString("ownRating"));
                animeInfo.setOwnStatus(rs.getString("ownStatus"));
                animeInfo.setEpisodesProgress(rs.getInt("episodesProgress"));

                animeList.add(animeInfo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new AnimeListInfo(animeList, 0);
    }
}
