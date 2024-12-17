package com.github.badbadbadbadbad.tsundoku.controllers;

/**
 * Describes the invoked functions used to anime the loading bar on API requests.
 */
public interface LoadingBarListener {
    void animateLoadingBar(double toPercent, double durationSeconds);
    void fadeOutLoadingBar(double durationSeconds);
}
