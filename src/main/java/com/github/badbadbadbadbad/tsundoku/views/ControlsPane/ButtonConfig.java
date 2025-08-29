package com.github.badbadbadbadbad.tsundoku.views.ControlsPane;

import org.jetbrains.annotations.NotNull;

public record ButtonConfig(@NotNull String label, @NotNull Runnable onClick) {
}
