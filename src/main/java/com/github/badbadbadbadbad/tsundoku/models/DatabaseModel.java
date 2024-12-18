package com.github.badbadbadbadbad.tsundoku.models;

import com.github.badbadbadbadbad.tsundoku.controllers.APIRequestListener;

import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This module takes care of everything related to database input / output.
 */
public class DatabaseModel {
    private static final String appName = "tsundoku";
    private static final long REQUEST_COOLDOWN_MS = 5000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final String databaseFilePath;
    private final APIRequestListener apiRequestListener;

    public DatabaseModel(APIRequestListener apiRequestListener) {
        this.apiRequestListener = apiRequestListener;
        this.databaseFilePath = Paths.get(getAppDataPath(), "profiles", "Default.db").toString();

        startAnimeUpdaterBackgroundService();
    }


    /**
     * Determines folder location for this program's files depending on user's operating system.
     */
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


    /**
     * Inserts data of a single anime into the database.
     * This is done as an upsert operation - if an anime with that ID is already present, we overwrite its values.
     * @param anime The data of the anime to be inserted into the database.
     */
    public void updateAnimeDatabaseWithEntry(AnimeInfo anime) {
        String url = "jdbc:sqlite:" + databaseFilePath;

        if (anime.getOwnStatus().equals("Completed")) {
            anime.setEpisodesProgress(anime.getEpisodesTotal());
        }

        String sqlDelete = "DELETE FROM anime WHERE id = ?";
        String sqlUpsert = """
        INSERT INTO anime (id, ownRating, ownStatus, episodesProgress, title, titleJapanese, titleEnglish, imageUrl, smallImageUrl,
                           publicationStatus, episodesTotal, source, ageRating, synopsis, release, studios, type, lastUpdate)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(id) DO UPDATE SET
            ownRating = excluded.ownRating,
            ownStatus = excluded.ownStatus,
            episodesProgress = excluded.episodesProgress,
            title = excluded.title,
            titleJapanese = excluded.titleJapanese,
            titleEnglish = excluded.titleEnglish,
            imageUrl = excluded.imageUrl,
            smallImageUrl = excluded.smallImageUrl,
            publicationStatus = excluded.publicationStatus,
            episodesTotal = excluded.episodesTotal,
            source = excluded.source,
            ageRating = excluded.ageRating,
            synopsis = excluded.synopsis,
            release = excluded.release,
            studios = excluded.studios,
            type = excluded.type,
            lastUpdate = excluded.lastUpdate;""";

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
                    pstmt.setString(9, anime.getSmallImageUrl());
                    pstmt.setString(10, anime.getPublicationStatus());
                    pstmt.setInt(11, anime.getEpisodesTotal());
                    pstmt.setString(12, anime.getSource());
                    pstmt.setString(13, anime.getAgeRating());
                    pstmt.setString(14, anime.getSynopsis());
                    pstmt.setString(15, anime.getRelease());
                    pstmt.setString(16, anime.getStudios());
                    pstmt.setString(17, anime.getType());
                    pstmt.setString(18, anime.getLastUpdated());
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    /**
     * Reads data of a single anime from the database.
     * @param id The ID of the anime to be read from the database.
     */
    public AnimeInfo getAnimeEntryFromDatabase(int id) {
        String url = "jdbc:sqlite:" + databaseFilePath;
        String sqlSelect = "SELECT id, ownRating, ownStatus, episodesProgress, title, titleJapanese, titleEnglish, imageUrl, smallImageUrl, publicationStatus, "
                + "episodesTotal, source, ageRating, synopsis, release, studios, type, lastUpdate FROM anime WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sqlSelect)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {

                AnimeInfo animeInfo = new AnimeInfo(rs.getInt("id"), rs.getString("title"), rs.getString("titleJapanese"), rs.getString("titleEnglish"),
                        rs.getString("imageUrl"), rs.getString("smallImageUrl"), rs.getString("publicationStatus"), rs.getInt("episodesTotal"), rs.getString("source"),
                        rs.getString("ageRating"), rs.getString("synopsis"), rs.getString("release"), rs.getString("studios"), rs.getString("type"), rs.getString("lastUpdate")
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

    /**
     * Reads the full data of the anime database as a List of AnimeInfo objects.
     */
    public AnimeListInfo getFullAnimeDatabase() {
        String url = "jdbc:sqlite:" + databaseFilePath;
        String sqlSelectAll = "SELECT id, ownRating, ownStatus, episodesProgress, title, titleJapanese, titleEnglish, imageUrl, smallImageUrl, publicationStatus, "
                + "episodesTotal, source, ageRating, synopsis, release, studios, type, lastUpdate FROM anime";
        List<AnimeInfo> animeList = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlSelectAll)) {

            while (rs.next()) {
                AnimeInfo animeInfo = new AnimeInfo(rs.getInt("id"), rs.getString("title"), rs.getString("titleJapanese"), rs.getString("titleEnglish"),
                        rs.getString("imageUrl"), rs.getString("smallImageUrl"), rs.getString("publicationStatus"), rs.getInt("episodesTotal"), rs.getString("source"),
                        rs.getString("ageRating"), rs.getString("synopsis"), rs.getString("release"), rs.getString("studios"), rs.getString("type"), rs.getString("lastUpdate")
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

    /**
     * Data on anime - especially upcoming anime - may change every so often. Hence, the local database needs to be updated regularly.
     * This function reads the full anime database once on startup, then updates it slowly while the program is open.
     */
    private void startAnimeUpdaterBackgroundService() {
        AnimeListInfo animeListInfo = getFullAnimeDatabase();
        List<AnimeInfo> animeList = filterAndSortAnimeList(animeListInfo.getAnimeList());
        processNextAnime(animeList, 0);
    }


    /**
     * A difficulty with the database update service is that the API only allows requests for single anime by ID instead of a list at once.
     * Yet, we also don't want to overload the API.
     *
     * <p>We filter anime entries that were updated somewhat recently from this updating process, and also
     * assign a lower priority to entries that are not releasing right now or upcoming.</p>
     * @param animeList The anime information to be filtered in List form.
     * @return The same List, but filtered and sorted by importance.
     */
    private List<AnimeInfo> filterAndSortAnimeList(List<AnimeInfo> animeList) {
        LocalDate currentDate = LocalDate.now(ZoneOffset.UTC);

        return animeList.stream()

                // Filter out:
                // Completed entries that were updated in the last 30 days
                // All other entries that were updated today
                .filter(anime -> {
                    LocalDate lastUpdated = LocalDate.parse(anime.getLastUpdated(), DATE_FORMATTER);
                    if ("Complete".equals(anime.getPublicationStatus())) {
                        return lastUpdated.isBefore(currentDate.minusDays(30));
                    } else {
                        return !lastUpdated.isEqual(currentDate);
                    }
                })

                // Sort by publication status and last updated
                .sorted(Comparator
                        .comparing((AnimeInfo anime) -> {
                            switch (anime.getPublicationStatus()) {
                                case "Upcoming":
                                case "Not yet provided":
                                    return 0;
                                case "Airing":
                                    return 1;
                                case "Complete":
                                    return 2;
                                default:
                                    return Integer.MAX_VALUE;
                            }
                        })
                        .thenComparing(anime -> LocalDate.parse(anime.getLastUpdated(), DATE_FORMATTER)))
                .collect(Collectors.toList());
    }


    /**
     * The actual "update this database entry with updated information" function for the background updater service.
     * Forwards the request for current data on some anime to the API communication service, then invokes a database update with the returned data.
     * @param animeList The full list of anime to be updated.
     * @param index Current index of the list to update. Updates when the update is finished and a timeout has passed.
     */
    private void processNextAnime(List<AnimeInfo> animeList, int index) {

        // No more anime to process
        if (index >= animeList.size()) {
            // System.out.println("Finished updating database.");
            return;
        }

        AnimeInfo animeInfo = animeList.get(index);
        int animeId = animeInfo.getId();

        apiRequestListener.getAnimeByID(animeId).thenAccept(newAnimeInfo -> {

            // Testing if it works
            /*
            System.out.println("Updated anime " + newAnimeInfo.getTitle() +
                    ", was last updated " + animeInfo.getLastUpdated() +
                    ", new date is " + newAnimeInfo.getLastUpdated());

             */

            // We only update the static info, the user's info needs to be kept
            newAnimeInfo.setOwnRating(animeInfo.getOwnRating());
            newAnimeInfo.setOwnStatus(animeInfo.getOwnStatus());
            newAnimeInfo.setEpisodesProgress(animeInfo.getEpisodesProgress());


            updateAnimeDatabaseWithEntry(newAnimeInfo);

            // Schedule the next anime processing with a delay
            CompletableFuture.delayedExecutor(REQUEST_COOLDOWN_MS, TimeUnit.MILLISECONDS)
                    .execute(() -> processNextAnime(animeList, index + 1));
        });
    }
}
