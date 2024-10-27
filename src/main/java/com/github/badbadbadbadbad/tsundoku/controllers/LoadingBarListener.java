package com.github.badbadbadbadbad.tsundoku.controllers;

public interface LoadingBarListener {
    void animateLoadingBar(double toPercent, double durationSeconds);
    void fadeOutLoadingBar(double durationSeconds);
}
