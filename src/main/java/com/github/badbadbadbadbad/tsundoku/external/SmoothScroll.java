// Inspired by /u/Matjaz on stackoverflow
// https://stackoverflow.com/a/70161549

package com.github.badbadbadbadbad.tsundoku.external;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;

import java.util.Set;

public class SmoothScroll {

    private final ScrollPane scrollPane;

    private final static double BASE_MODIFIER = 1;

    private double accumulatedTargetVValue = 0;
    public DoubleProperty accumulatedTargetVValueProp = new SimpleDoubleProperty();

    public SmoothScroll(final ScrollPane scrollPane, final Node node) {
        this(scrollPane, node, 160);
    }

    public SmoothScroll(final ScrollPane scrollPane, final Node node, final double baseChange) {

        this.scrollPane = scrollPane;

        this.accumulatedTargetVValueProp.set(0.0);

        // When scrollBar is dragged, the accumulated target vvalue needs to be changed manually.
        // Unfortunately, JavaFX does not let us access the scrollBar directly for events.

        // The scrollBar cannot be accessed until the scrollPane's skin is loaded.
        // Platform.runLater is "unsafe" here, but scrollPanes apparently have a skinProperty we can wait on.
        scrollPane.skinProperty().addListener((skinObservable, oldSkin, newSkin) -> {

            // This is how the scrollBar of the scrollPane is accessed.
            Set<Node> nodes = scrollPane.lookupAll(".scroll-bar");
            for (final Node n : nodes) {
                if (n instanceof ScrollBar) {

                    // Also need to handle the "scroll track click" event..
                    n.setOnMouseClicked(event -> {
                        accumulatedTargetVValue = scrollPane.getVvalue();
                        this.accumulatedTargetVValueProp.set(accumulatedTargetVValue);
                    });


                    // Scroll bar drag event
                    n.setOnMouseReleased(event -> {
                        accumulatedTargetVValue = scrollPane.getVvalue();
                        this.accumulatedTargetVValueProp.set(accumulatedTargetVValue);
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

                double oldAcc = accumulatedTargetVValue;

                // JavaFX uses vValue for the scrollpane, hence binding scroll size to scrollPane content size by default.
                // Override this behaviour by normalizing for scrollPane height.
                double viewportHeight = scrollPane.getViewportBounds().getHeight();
                double totalContentHeight = scrollPane.getContent().getBoundsInLocal().getHeight();

                // Speedup modifier for small scrollPanes so they aren't super slow.
                double speedupModifier = Math.min(5, Math.max(1, 5 * viewportHeight / totalContentHeight));

                double pixelScrollChange = baseChange * BASE_MODIFIER * speedupModifier;
                double vvalueChange = -pixelScrollChange / totalContentHeight;

                // Fuse interrupted scroll into new scroll
                accumulatedTargetVValue += vvalueChange * Math.signum(deltaYOrg);
                accumulatedTargetVValue = Math.max(0, Math.min(accumulatedTargetVValue, 1));
                accumulatedTargetVValueProp.set(accumulatedTargetVValue);

                // Multiscrolls cause slowdown if scrolling into the upper or lower end of the scrollPane.
                // Hence, we just skip multiscrolls if they keep crashing into a scrollPane end.
                if (oldAcc == accumulatedTargetVValue){
                    return;
                }

                smoothTransition(scrollPane.getVvalue(), accumulatedTargetVValue, deltaYOrg, vvalueChange);
            }

            // Calling the interpolated animation
            private void smoothTransition(double startingVValue, double finalVValue, double deltaY, double vValueChange) {
                Interpolator interp = Interpolator.LINEAR;

                // Stop the previous transition if it's still running to prevent conflicts
                // If not done, the scrollbar may teleport occasionally
                if (transition != null && transition.getStatus() == Animation.Status.RUNNING) {
                    transition.stop();
                }

                // Scrolls being interrupted by the scrollPane's upper or lower end have their animation time lowered
                // In accordance with the lower distance they travel until being stopped.
                double startEndDistance = Math.abs(finalVValue - startingVValue);
                double animationDurationFactor = Math.min(1, startEndDistance / Math.abs(vValueChange));


                transition = new SmoothishTransition(transition, deltaY, animationDurationFactor) {
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

    public void resetAccumulatedVValue() {
        accumulatedTargetVValue = 0;
        accumulatedTargetVValueProp.set(accumulatedTargetVValue);
    }

    public void adjustAccumulatedVValue() {
        accumulatedTargetVValue = scrollPane.getVvalue();
        accumulatedTargetVValueProp.set(accumulatedTargetVValue);
    }

    public double getAccumulatedVValue() {
        return accumulatedTargetVValue;
    }
}