package com.github.badbadbadbadbad.tsundoku.controllers;

/**
 * Describes the invoked functions for Browse view filters changing.
 */
public interface GridFilterListener {
    void onAnimeOrderByChanged(String newVal);
    void onAnimeStatusChanged(String newVal);
    void onAnimeStartYearChanged(String newVal);
    void onAnimeEndYearChanged(String newVal);
    String getAnimeOrderByDefault();
    String getAnimeStatusDefault();
    String getAnimeStartYearDefault();
    String getAnimeEndYearDefault();
}
