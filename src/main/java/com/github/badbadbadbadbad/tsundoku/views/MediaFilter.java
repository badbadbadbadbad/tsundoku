package com.github.badbadbadbadbad.tsundoku.views;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface MediaFilter {
    @NotNull String getLabel();

    @Nullable List<String> getOptions();
}
