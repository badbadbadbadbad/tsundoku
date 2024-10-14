package com.github.badbadbadbadbad.tsundoku.models;

public class AnimeInfo {
    private String ownRating = "UNSCORED";
    private String ownStatus = "UNTRACKED";
    private int episodesProgress = 0;

    private final int id;
    private final String title;
    private final String titleJapanese;
    private final String titleEnglish;
    private final String imageUrl;
    private final String publicationStatus;
    private final int episodesTotal;
    private final String source;
    private final String ageRating;
    private final String synopsis;
    private final String release;
    private final String studios;

    public AnimeInfo(int id, String title, String titleJapanese, String titleEnglish, String imageUrl,
                     String publicationStatus, int episodesTotal, String source, String ageRating,
                     String synopsis, String release, String studios) {
        this.id = id;
        this.title = title;
        this.titleJapanese = titleJapanese;
        this.titleEnglish = titleEnglish;
        this.imageUrl = imageUrl;
        this.publicationStatus = publicationStatus;
        this.episodesTotal = episodesTotal;
        this.source = source;
        this.ageRating = ageRating;
        this.synopsis = synopsis;
        this.release = release;
        this.studios = studios;
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
}
