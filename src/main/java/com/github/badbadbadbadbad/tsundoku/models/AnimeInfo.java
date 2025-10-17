package com.github.badbadbadbadbad.tsundoku.models;

import com.github.badbadbadbadbad.tsundoku.enums.anime.AgeRating;
import com.github.badbadbadbadbad.tsundoku.enums.anime.ReleaseStatus;
import com.github.badbadbadbadbad.tsundoku.enums.anime.Type;
import com.github.badbadbadbadbad.tsundoku.enums.generic.PersonalRating;
import com.github.badbadbadbadbad.tsundoku.enums.generic.PersonalStatus;
import org.jetbrains.annotations.NotNull;

/**
 * A container object for data on a single anime. The data is provided by Jikan.Moe API,
 * <a href="https://docs.api.jikan.moe/#tag/anime/operation/getAnimeById">see example here</a>.
 *
 * <p>We specifically do not use a Java Record class because this class also contains the user's progress data for this anime, which is mutable.</p>
 */
public class AnimeInfo {
    private final int id;                                   // MyAnimeList ID
    private final String title;                             // "Default" title used by MyAnimeList (usually roumaji)
    private final String titleJapanese;                     // "Japanese" title used by MyAnimeList, or "Not yet provided"
    private final String titleEnglish;                      // "English" title used by MyAnimeList, or "Not yet provided"
    private final String imageUrl;                          // URL pointing to cover art used on MyAnimeList, or "Not yet provided"
    private final String smallImageUrl;                     // URL pointing to smaller cover art used on MyAnimeList, or "Not yet provided"
    private final ReleaseStatus publicationStatus;
    private final int episodesTotal;                        // Total episodes of this anime. May be 0, which we keep as a "not yet known".
    private final String source;                            // Original source of this anime, like "Manga", "Light Novel", "Original".
    private final AgeRating ageRating;                         // Age rating, includes both nudity and gore (thanks MAL for having a shit system), or "Not yet provided"
    private final String synopsis;                          // Synopsis / description text, or "Not yet provided"
    private final String release;                           // We attempt to calculate a "<Season> <Year>" String from provided data, else "Not yet provided"
    private final String studios;                           // Studios involved in creating, or "Not yet provided"
    private final Type type;                              // Enum of "TV", "Movie", "OVA", "Special", "ONA", "Music", "CM", "PV", "TV Special", or "Not yet provided"
    private final String lastUpdated;                       // Last update of this information container in YYYY-MM-DD format, UTC time zone (because SQLite uses that)
    private @NotNull PersonalRating ownRating = PersonalRating.UNSCORED;
    private @NotNull PersonalStatus ownStatus = PersonalStatus.UNTRACKED;
    private int episodesProgress = 0;

    public AnimeInfo(int id, String title, String titleJapanese, String titleEnglish, String imageUrl,
                     String smallImageUrl, ReleaseStatus publicationStatus, int episodesTotal, String source,
                     AgeRating ageRating, String synopsis, String release, String studios, Type type, String lastUpdated) {
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

    public @NotNull PersonalRating getOwnRating() {
        return ownRating;
    }

    public void setOwnRating(@NotNull PersonalRating newRating) {
        this.ownRating = newRating;
    }

    public @NotNull PersonalStatus getOwnStatus() {
        return ownStatus;
    }

    public void setOwnStatus(@NotNull PersonalStatus newStatus) {
        this.ownStatus = newStatus;
    }

    public int getEpisodesProgress() {
        return episodesProgress;
    }

    public void setEpisodesProgress(int newEpisodes) {
        this.episodesProgress = newEpisodes;
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

    public ReleaseStatus getPublicationStatus() {
        return publicationStatus;
    }

    public int getEpisodesTotal() {
        return episodesTotal;
    }

    public String getSource() {
        return source;
    }

    public AgeRating getAgeRating() {
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

    public Type getType() {
        return type;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }
}
