package com.github.badbadbadbadbad.tsundoku.controllers;

import java.util.Map;

/**
 * Describes the invoked function when settings in the settings view are changed.
 */
public interface SettingsListener {
    void onSettingsChanged(Map<String, Object> settings);
}
