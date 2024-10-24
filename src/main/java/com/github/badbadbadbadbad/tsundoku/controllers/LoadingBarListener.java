package com.github.badbadbadbadbad.tsundoku.controllers;

public interface LoadingBarListener {
    void animateLoadingBar(double fromPercent, double toPercent, double durationSeconds);
    void fadeOutLoadingBar(double durationSeconds);
}
