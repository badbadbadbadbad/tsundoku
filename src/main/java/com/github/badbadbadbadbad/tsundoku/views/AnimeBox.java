package com.github.badbadbadbadbad.tsundoku.views;

import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import com.github.badbadbadbadbad.tsundoku.util.StyleUtils;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.function.Consumer;

public class AnimeBox extends VBox {

    private final double RATIO = 318.0 / 225.0;

    public AnimeBox(AnimeInfo anime, String languagePreference) {
        super();

        setAlignment(Pos.CENTER);
        getStyleClass().add("grid-media-box");
        setUserData(anime);

        // setRatingBorder(animeBox);

        // Clipping rectangle because JavaFX doesn't have any kind of background image clipping. WHY??
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        clip.setArcHeight(40);
        clip.setArcWidth(40);
        setClip(clip);


        // Label with anime name to be shown on animeBox hover
        // Change title depending on language preference
        Label titleLabel = new Label();
        String title = anime.getTitle();

        if (languagePreference.equals("Japanese") && !anime.getTitleJapanese().equals("Not yet provided")) {
            title = anime.getTitleJapanese();
            titleLabel.getStyleClass().add("grid-media-box-text-jp");
        } else if (languagePreference.equals("English") && !anime.getTitleEnglish().equals("Not yet provided")) {
            title = anime.getTitleEnglish();
            titleLabel.getStyleClass().add("grid-media-box-text-en");
        } else {
            titleLabel.getStyleClass().add("grid-media-box-text-en");
        }

        titleLabel.setText(title);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.getStyleClass().add("grid-media-box-text");
        titleLabel.setOpacity(0.0);
        titleLabel.setMaxWidth(0.0); // This fixes the different label sizes causing different animeBox sizes. Why..


        // AnchorPane wrapper to hold the label because JavaFX freaks out with animeBox sizing otherwise
        AnchorPane ap = new AnchorPane();
        ap.setMaxHeight(Double.MAX_VALUE);
        ap.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(ap, Priority.ALWAYS);
        HBox.setHgrow(ap, Priority.ALWAYS);
        ap.getStyleClass().add("grid-media-box-anchor");

        // We set the anchors to grow two pixels outwards because the animeBox borders look a little aliased otherwise.
        AnchorPane.setBottomAnchor(titleLabel, -2.0);
        AnchorPane.setTopAnchor(titleLabel, -2.0);
        AnchorPane.setLeftAnchor(titleLabel, -2.0);
        AnchorPane.setRightAnchor(titleLabel, -2.0);

        ap.getChildren().add(titleLabel);
        getChildren().add(ap);


        // Fade events for the label popup
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.2), titleLabel);
        fadeIn.setToValue(1.0);
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.2), titleLabel);
        fadeOut.setToValue(0.0);
        setOnMouseEntered(event -> fadeIn.playFromStart());
        setOnMouseExited(event -> fadeOut.playFromStart());


        widthProperty().addListener((obs, oldWidth, newWidth) -> {
            // Platform.runLater needed to trigger layout update post-resizing
            // Has a chance to get a bit wonky on window snaps otherwise
            Platform.runLater(() -> {
                double newHeight = newWidth.doubleValue() * RATIO;
                setMinHeight(newHeight);
                setPrefHeight(newHeight);
                setMaxHeight(newHeight);
            });
        });

        setVisible(false);
    }

    public void setOnMouseClick(Consumer<AnimeBox> callback) {
        setOnMouseClicked(e -> callback.accept(this));
    }

    public void setRatingBorder(AnimeInfo databaseAnime, boolean useBlueFallback) {
        this.getStyleClass().removeAll(
                "grid-media-box-gold",
                "grid-media-box-green",
                "grid-media-box-red",
                "grid-media-box-blue",
                "grid-media-box-grey"
        );

        this.getStyleClass().add(StyleUtils.computeBorderClass(databaseAnime, useBlueFallback));
    }
}
