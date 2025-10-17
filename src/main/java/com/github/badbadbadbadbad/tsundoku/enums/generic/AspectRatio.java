package com.github.badbadbadbadbad.tsundoku.enums.generic;

public enum AspectRatio {
    ANIME(318.0, 225.0), //
    ;

    private final double height;
    private final double width;

    AspectRatio(double height, double width) {
        this.height = height;
        this.width = width;
    }

    public double getRatio() {
        return height / width;
    }

    public double getHeight() {
        return height;
    }

    public double getWidth() {
        return width;
    }
}
