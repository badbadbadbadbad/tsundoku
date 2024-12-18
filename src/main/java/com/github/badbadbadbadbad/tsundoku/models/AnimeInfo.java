package com.github.badbadbadbadbad.tsundoku.models;

/**
 * A container object for data on a single anime. The data is provided by Jikan.Moe API,
 * <a href="https://docs.api.jikan.moe/#tag/anime/operation/getAnimeById">see example here</a>.
 *
 * <p>We specifically do not use a Java Record class because this class also contains the user's progress data for this anime, which is mutable.</p>
 */
public class AnimeInfo {
    private String ownRating = "Unscored";                  // Rating of "Unscored", "Heart", "Liked", "Disliked"
    private String ownStatus = "Untracked";                 // Own progress status of "Untracked", "Backlog", "In progress", "Completed", "Paused", "Dropped"
    private int episodesProgress = 0;                       // Amount of episodes watched

    private final int id;                                   // MyAnimeList ID
    private final String title;                             // "Default" title used by MyAnimeList (usually roumaji)
    private final String titleJapanese;                     // "Japanese" title used by MyAnimeList, or "Not yet provided"
    private final String titleEnglish;                      // "English" title used by MyAnimeList, or "Not yet provided"
    private final String imageUrl;                          // URL pointing to cover art used on MyAnimeList, or "Not yet provided"
    private final String smallImageUrl;                     // URL pointing to smaller cover art used on MyAnimeList, or "Not yet provided"
    private final String publicationStatus;                 // Current status of an anime of "Airing", "Complete", "Upcoming", or "Not yet provided"
    private final int episodesTotal;                        // Total episodes of this anime. May be 0, which we keep as a "not yet known".
    private final String source;                            // Original source of this anime, like "Manga", "Light Novel", "Original".
    private final String ageRating;                         // Age rating, includes both nudity and gore (thanks MAL for having a shit system), or "Not yet provided"
    private final String synopsis;                          // Synopsis / description text, or "Not yet provided"
    private final String release;                           // We attempt to calculate a "<Season> <Year>" String from provided data, else "Not yet provided"
    private final String studios;                           // Studios involved in creating, or "Not yet provided"
    private final String type;                              // Enum of "TV", "Movie", "OVA", "Special", "ONA", "Music", "CM", "PV", "TV Special", or "Not yet provided"
    private final String lastUpdated;                       // Last update of this information container in YYYY-MM-DD format, UTC time zone (because SQLite uses that)

    public AnimeInfo(int id, String title, String titleJapanese, String titleEnglish, String imageUrl,
                     String smallImageUrl, String publicationStatus, int episodesTotal, String source,
                     String ageRating, String synopsis, String release, String studios, String type, String lastUpdated) {
        this.id = id;
        this.title = title;
        this.titleJapanese = titleJapanese;
        this.titleEnglish = titleEnglish;
        this.imageUrl = imageUrl;
        this.smallImageUrl = smallImageUrl;
        this.publicationStatus = publicationStatus;
        this.episodesTotal = episodesTotal;
        this.source = source;
        this.ageRating = ageRating;
        this.synopsis = synopsis;
        this.release = release;
        this.studios = studios;
        this.type = type;
        this.lastUpdated = lastUpdated;
    }

    public void setOwnRating(String newRating) {
        this.ownRating = newRating;
    }

    public String getOwnRating() {
        return ownRating;
    }

    public void setOwnStatus(String newStatus) {
        this.ownStatus = newStatus;
    }

    public String getOwnStatus() {
        return ownStatus;
    }

    public void setEpisodesProgress(int newEpisodes) {
        this.episodesProgress = newEpisodes;
    }

    public int getEpisodesProgress() {
        return episodesProgress;
    }


    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getTitleJapanese() {
        return titleJapanese;
    }

    public String getTitleEnglish() {
        return titleEnglish;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getSmallImageUrl() {
        return smallImageUrl;
    }

    public String getPublicationStatus() {
        return publicationStatus;
    }

    public int getEpisodesTotal() {
        return episodesTotal;
    }

    public String getSource() {
        return source;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public String getRelease() {
        return release;
    }

    public String getStudios() {
        return studios;
    }

    public String getType() {
        return type;
    }

    public String getLastUpdated() { return lastUpdated; }
}
