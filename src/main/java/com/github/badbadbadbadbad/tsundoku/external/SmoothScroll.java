// Inspired by /u/Matjaz on stackoverflow
// https://stackoverflow.com/a/70161549

package com.github.badbadbadbadbad.tsundoku.external;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;

import java.util.Set;

public class SmoothScroll {

    private final static double BASE_MODIFIER = 1;
    private double accumulatedTargetVValue = 0;

    public SmoothScroll(final ScrollPane scrollPane, final Node node) {
        this(scrollPane, node, 160);
    }
    public SmoothScroll(final ScrollPane scrollPane, final Node node, final double baseChange) {

        // When scrollBar is dragged, the accumulated target vvalue needs to be changed manually.
        // Unfortunately, JavaFX does not let us access the scrollBar directly for events.

        // The scrollBar cannot be accessed until the scrollPane's skin is loaded.
        // Platform.runLater is unsafe here, but scrollPanes apparently have a skinProperty we can wait on.
        scrollPane.skinProperty().addListener((skinObservable, oldSkin, newSkin) -> {

            // This is how the scrollBar of the scrollPane is accessed.
            Set<Node> nodes = scrollPane.lookupAll(".scroll-bar");
            for (final Node n : nodes) {
                if (n instanceof ScrollBar) {
                    n.setOnMouseReleased(event -> {
                        accumulatedTargetVValue = scrollPane.getVvalue();
                    });
                }
            }
        });


        // The actual smooth scroller.
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

                // Stop the previous transition if it's still running to prevent conflicts
                if (transition != null && transition.getStatus() == Animation.Status.RUNNING) {
                    transition.stop();
                }

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

            /*
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

             */
        });
    }
}