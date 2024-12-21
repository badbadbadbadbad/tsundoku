package com.github.badbadbadbadbad.tsundoku.views;


/**
 * Contract to ensure any views creating a LazyLoader shut it down (so no threads stay open in background)
 */
public interface LazyLoaderView {
    void shutdownLazyLoader();
}
