package com.github.badbadbadbadbad.tsundoku.views;

public class Anime {
    private final int id;
    private final String title;
    private final String imageUrl;

    public Anime (int id, String title, String imageUrl) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
