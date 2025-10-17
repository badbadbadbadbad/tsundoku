package com.github.badbadbadbadbad.tsundoku.views.components.ControlsPane;

import org.jetbrains.annotations.NotNull;

public record ButtonConfig(@NotNull String label, @NotNull Runnable onClick) {
}
