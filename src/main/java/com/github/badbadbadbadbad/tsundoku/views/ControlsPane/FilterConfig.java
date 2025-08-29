package com.github.badbadbadbadbad.tsundoku.views.ControlsPane;

import com.github.badbadbadbadbad.tsundoku.views.MediaFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public record FilterConfig(
        @NotNull Type type,
        @NotNull MediaFilter key,
        @Nullable MediaFilter key2,
        @Nullable List<Consumer<String>> onChange,
        @Nullable List<Consumer<String>> onChange2,
        @Nullable Runnable onEnter,
        @Nullable String initialValue,
        @Nullable String initialValue2
) {

    public static FilterConfig dropdown(@NotNull MediaFilter key,
                                        @NotNull List<Consumer<String>> onChange,
                                        @Nullable String initialValue) {
        return new FilterConfig(Type.DROPDOWN, key, null, onChange, null, null, initialValue, null);
    }

    public static FilterConfig number(@NotNull MediaFilter key,
                                      @Nullable List<Consumer<String>> onChange,
                                      @Nullable Runnable onEnter,
                                      @Nullable String initialValue) {
        return new FilterConfig(Type.NUMBER, key, null, onChange, null, onEnter, initialValue, null);
    }

    public static FilterConfig doubleNumber(@NotNull MediaFilter key1, @NotNull MediaFilter key2,
                                            @NotNull List<Consumer<String>> onChange1,
                                            @NotNull List<Consumer<String>> onChange2,
                                            @Nullable Runnable onEnter,
                                            @Nullable String initial1,
                                            @Nullable String initial2) {
        return new FilterConfig(Type.DOUBLE_NUMBER, key1, key2, onChange1, onChange2, onEnter, initial1, initial2);
    }

    public enum Type {DROPDOWN, NUMBER, DOUBLE_NUMBER}
}