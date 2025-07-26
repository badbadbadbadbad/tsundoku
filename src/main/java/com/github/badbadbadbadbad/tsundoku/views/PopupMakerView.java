package com.github.badbadbadbadbad.tsundoku.views;

/**
 * Contract to ensure views creating a PopupView do their own updates when it's closed again
 */
public interface PopupMakerView {
    void onPopupClosed(AnimeBox popupParent);
}
