// Inspired by /u/Matjaz on stackoverflow
// https://stackoverflow.com/a/70161549

package com.github.badbadbadbadbad.tsundoku.external;

import javafx.animation.Transition;

import javafx.util.Duration;

public abstract class SmoothishTransition extends Transition {
    private final double mod;
    private double delta = 0;

    private final static int TRANSITION_DURATION = 100;


    // TODO: Experiment more if we even need the mod / delta functionality. Could probably leave it out.

    public SmoothishTransition(SmoothishTransition old, double delta) {

        setCycleDuration(Duration.millis(TRANSITION_DURATION));
        setCycleCount(1);

        // System.out.println("Test");

        // if (old != null && sameSign(delta, old.delta)) {
        if (old != null) {
            mod = old.getMod() + Math.abs(delta / 40);
            this.delta += delta;
        } else { // This shouldn't be necessary any longer. Just resets on new SmoothScroll load.
            mod = 1;
            this.delta = delta;
        }
    }

    public double getMod() {
        return mod;
    }

    public void setDelta(double delta) {
        this.delta += delta;
    }

    public double getRemainingDistance() {
        double elapsedFrac = getCurrentTime().toMillis() / getCycleDuration().toMillis();
        return delta * (1 - elapsedFrac);
    }

    @Override
    public void play() {
        super.play();
        // Even with a linear interpolation, startup is visibly slower than the middle.
        // So skip a small bit of the animation to keep up with the speed of prior
        // animation. The value of 10 works and isn't noticeable unless you really pay
        // close attention. This works best on linear but also is decent for others.
        if (getMod() > 1) {
            jumpTo(getCycleDuration().divide(10));
        }
    }

    private static boolean playing(Transition t) {
        return t.getStatus() == Status.RUNNING;
    }

    private static boolean sameSign(double d1, double d2) {
        return (d1 > 0 && d2 > 0) || (d1 < 0 && d2 < 0);
    }
}