// Inspired by /u/Matjaz on stackoverflow
// https://stackoverflow.com/a/70161549

package com.github.badbadbadbadbad.tsundoku.external;

import javafx.animation.Interpolator;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;

public class SmoothScroll {

    private final static double BASE_MODIFIER = 1;
    private double accumulatedTargetVValue = 0;

    public SmoothScroll(final ScrollPane scrollPane, final Node node) {
        this(scrollPane, node, 160);
    }
    public SmoothScroll(final ScrollPane scrollPane, final Node node, final double baseChange) {
        node.setOnScroll(new EventHandler<ScrollEvent>() {
            private SmoothishTransition transition;

            @Override
            public void handle(ScrollEvent event) {
                if (scrollPane==null) {
                    return;
                }
                double deltaYOrg = event.getDeltaY();
                if (deltaYOrg==0) {
                    return;
                }

                // JavaFX uses vValue for the scrollpane, hence binding scroll size to scrollPane content size by default.
                // Override this behaviour by normalizing for scrollPane height.
                double pixelScrollChange = baseChange * BASE_MODIFIER;
                double totalContentHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
                double vvalueChange = -pixelScrollChange / totalContentHeight;

                // Fuse interrupted scroll into new scroll
                accumulatedTargetVValue += vvalueChange * Math.signum(deltaYOrg);
                accumulatedTargetVValue = Math.max(0, Math.min(accumulatedTargetVValue, 1));

                smoothTransition(scrollPane.getVvalue(), accumulatedTargetVValue, deltaYOrg);

            }

            private void smoothTransition(double startingVValue, double finalVValue, double deltaY) {
                Interpolator interp = Interpolator.LINEAR;
                transition = new SmoothishTransition(transition, deltaY) {
                    @Override
                    protected void interpolate(double frac) {
                        scrollPane.setVvalue(
                                interp.interpolate(startingVValue, finalVValue, frac)
                        );
                    }
                };
                transition.playFromStart();
            }
        });
    }
}