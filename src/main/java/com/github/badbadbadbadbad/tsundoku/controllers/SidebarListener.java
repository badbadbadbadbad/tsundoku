package com.github.badbadbadbadbad.tsundoku.controllers;

/**
 * Describes the invoked functions when buttons in the sidebar are clicked.
 */
public interface SidebarListener {
    void onSidebarMediaModeChanged(String mode);
    void onSidebarBrowseModeChanged(String mode);
}
