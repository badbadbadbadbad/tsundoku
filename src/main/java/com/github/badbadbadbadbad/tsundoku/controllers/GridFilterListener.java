package com.github.badbadbadbadbad.tsundoku.controllers;

public interface GridFilterListener {
    void onAnimeOrderByChanged(String newVal);
    void onAnimeStatusChanged(String newVal);
    void onAnimeStartYearChanged(String newVal);
    void onAnimeEndYearChanged(String newVal);
}
