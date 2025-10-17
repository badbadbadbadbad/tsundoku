package com.github.badbadbadbadbad.tsundoku.enums.anime;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static java.util.stream.Collectors.toList;

public enum Type {
    ANY("Any"), //
    TV("TV"), //
    MOVIE("Movie"), //
    OVA("OVA"), //
    SPECIAL("Special"), //
    ONA("ONA"), //
    MUSIC("Music"), //
    CM("CM"), //
    PV("PV"), //
    TV_SPECIAL("TV Special"), //
    NOT_YET_PROVIDED("Not yet provided"), //
    ;

    private static final EnumSet<Type> FOR_FILTER =
            EnumSet.of(ANY, TV, MOVIE, OVA, SPECIAL, ONA, MUSIC, CM, PV, TV_SPECIAL);
    private final @NotNull String type;

    Type(@NotNull String type) {
        this.type = type;
    }

    public static @NotNull List<String> filterValues() {
        return FOR_FILTER.stream().map(Type::toDisplay).collect(toList());
    }

    public static @NotNull List<String> displayValues() {
        return Arrays.stream(values())
                .map(Type::toDisplay)
                .toList();
    }

    public static Type fromDisplay(String string) {
        for (var v : values()) if (v.type.equals(string)) return v;
        throw new IllegalArgumentException("Value not in enum: " + string);
    }

    public @NotNull String toDisplay() {
        return type;
    }
}