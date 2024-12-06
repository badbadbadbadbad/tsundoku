package com.github.badbadbadbadbad.tsundoku.controllers;

import java.util.Map;

public interface SettingsListener {
    void onSettingsChanged(Map<String, Object> settings);
}
