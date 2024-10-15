package com.github.badbadbadbadbad.tsundoku.views;

import com.github.badbadbadbadbad.tsundoku.external.FlowGridPane;
import com.github.badbadbadbadbad.tsundoku.external.SmoothScroll;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import org.kordamp.ikonli.dashicons.Dashicons;
import org.kordamp.ikonli.fluentui.FluentUiFilledMZ;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.BiFunction;

public class AnimePopupView {
    double RATIO = 318.0 / 225.0; // The aspect ratio to use for anime images. Close to most cover images.

    public VBox createPopup(AnimeInfo anime) {
        VBox popupBox = new VBox();
        popupBox.getStyleClass().add("grid-media-popup");
        popupBox.setMinWidth(Screen.getPrimary().getVisualBounds().getWidth() * 0.35);
        popupBox.setMaxWidth(Screen.getPrimary().getVisualBounds().getWidth() * 0.35);
        popupBox.setMinHeight(Screen.getPrimary().getVisualBounds().getHeight() * 0.5);
        popupBox.setMaxHeight(Screen.getPrimary().getVisualBounds().getHeight() * 0.5);
        popupBox.setAlignment(Pos.CENTER);

        Label title = createPopupTitle(anime, popupBox);
        HBox contentWrapper = createPopupContent(anime, popupBox);

        popupBox.getChildren().addAll(title, contentWrapper);

        return popupBox;
    }

    private Label createPopupTitle(AnimeInfo anime, VBox popupBox) {
        // Title
        Label titleLabel = new Label(anime.getTitle());
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setMinHeight(popupBox.getMaxHeight() * 0.1);
        titleLabel.setMaxHeight(popupBox.getMaxHeight() * 0.1);
        titleLabel.getStyleClass().add("grid-media-popup-title");
        titleLabel.setFont(Font.font(30.0)); // Needs to be outside CSS for dynamic adjustments..

        // Adjust font size for very long titles to fit container
        titleLabel.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            adjustFontSizeToContainer(titleLabel, popupBox.getMaxWidth() - 15, 10);
        });

        return titleLabel;
    }

    private void adjustFontSizeToContainer(Label label, double containerWidth, double minFontSize) {
        Font font = label.getFont();
        double fontSize = font.getSize();
        String fontFamily = font.getFamily();

        // Temporary font needs to be used because JavaFX buggy when overwriting active font.
        Text text = new Text(label.getText());
        text.setFont(font);

        while (text.getBoundsInLocal().getWidth() > containerWidth  && fontSize > minFontSize) {
            fontSize--;
            text.setFont(Font.font(fontFamily, fontSize));
        }

        label.setFont(Font.font(fontFamily, fontSize));
    }

    private HBox createPopupContent(AnimeInfo anime, VBox popupBox) {
        HBox contentWrapper = new HBox();
        HBox.setHgrow(contentWrapper, Priority.ALWAYS);
        VBox.setVgrow(contentWrapper, Priority.ALWAYS);

        VBox leftContent = createLeftPopupContent(anime, popupBox);
        VBox rightContent = createRightPopupContent(anime);

        contentWrapper.getChildren().addAll(leftContent, rightContent);

        return contentWrapper;
    }

    private VBox createLeftPopupContent(AnimeInfo anime, VBox popupBox) {
        VBox imageAndSelfStatsWrapper = new VBox();
        VBox.setVgrow(imageAndSelfStatsWrapper, Priority.ALWAYS);
        imageAndSelfStatsWrapper.setMinWidth(popupBox.getMaxWidth() * 0.3);
        imageAndSelfStatsWrapper.setMaxWidth(popupBox.getMaxWidth() * 0.3);
        imageAndSelfStatsWrapper.getStyleClass().add("grid-media-popup-left");

        VBox image = createCoverImage(anime, imageAndSelfStatsWrapper);
        ComboBox<String> status = createStatusBox();
        HBox rating = createRatingBox();
        HBox progress = createProgressTracker(0, "episodes");

        imageAndSelfStatsWrapper.getChildren().addAll(image, status, rating, progress);

        return imageAndSelfStatsWrapper;
    }

    private VBox createCoverImage(AnimeInfo anime, VBox wrapper) {
        VBox imageBox = new VBox();
        imageBox.setStyle("-fx-background-image: url('" + anime.getImageUrl() + "');");
        imageBox.getStyleClass().add("grid-media-box");

        wrapper.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double imageBoxWidth = newWidth.doubleValue() - wrapper.getPadding().getLeft() * 2;
            double imageBoxHeight = imageBoxWidth * RATIO;

            imageBox.setMinWidth(imageBoxWidth);
            imageBox.setMaxWidth(imageBoxWidth);
            imageBox.setMinHeight(imageBoxHeight);
            imageBox.setMaxHeight(imageBoxHeight);
        });

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(imageBox.widthProperty());
        clip.heightProperty().bind(imageBox.heightProperty());
        clip.setArcHeight(40);
        clip.setArcWidth(40);
        imageBox.setClip(clip);

        return imageBox;
    }

    private ComboBox<String> createStatusBox() {
        ComboBox<String> status = new ComboBox<>();
        status.getItems().add("Untracked");
        status.setValue("Untracked");
        status.setMinHeight(30);
        status.setMaxHeight(30);
        status.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(status, Priority.ALWAYS);
        status.setStyle("-fx-background-radius: 10;");

        return status;
    }

    private HBox createRatingBox() {
        HBox rating = new HBox();
        rating.setMinHeight(30);
        rating.setMaxHeight(30);
        rating.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(rating, Priority.ALWAYS);
        rating.setStyle("-fx-background-radius: 10; -fx-background-color: lightgrey;");

        Button heartButton = new Button();
        heartButton.setGraphic(new FontIcon(Dashicons.HEART));
        heartButton.getStyleClass().addAll("ratings-button", "ikonli-heart-active");

        Button thumbsUpButton = new Button();
        thumbsUpButton.setGraphic(new FontIcon(FluentUiFilledMZ.THUMB_LIKE_24));
        thumbsUpButton.getStyleClass().addAll("ratings-button", "ikonli-thumb-active");

        Button thumbsDownButton = new Button();
        thumbsDownButton.setGraphic(new FontIcon(FluentUiFilledMZ.THUMB_DISLIKE_24));
        thumbsDownButton.getStyleClass().addAll("ratings-button", "ikonli-thumb-active");

        HBox.setHgrow(heartButton, Priority.ALWAYS);
        HBox.setHgrow(thumbsUpButton, Priority.ALWAYS);
        HBox.setHgrow(thumbsDownButton, Priority.ALWAYS);
        heartButton.setMaxWidth(Double.MAX_VALUE);
        thumbsUpButton.setMaxWidth(Double.MAX_VALUE);
        thumbsDownButton.setMaxWidth(Double.MAX_VALUE);

        rating.getChildren().addAll(heartButton, thumbsUpButton, thumbsDownButton);

        return rating;
    }

    private HBox createProgressTracker(int progressValue, String unit) {
        // Wrapper
        HBox progressTracker = new HBox();
        progressTracker.setMinHeight(30);
        progressTracker.setMaxHeight(30);
        progressTracker.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(progressTracker, Priority.ALWAYS);

        // Input for current progress.
        TextField numberInput = new TextField(String.valueOf(progressValue));
        numberInput.setMinHeight(30);
        numberInput.setMaxHeight(30);
        numberInput.setStyle("-fx-background-radius: 10; -fx-border-radius: 10;");
        HBox.setHgrow(numberInput, Priority.ALWAYS);

        numberInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                numberInput.setText(newValue.replaceAll("\\D", ""));
            }
        });



        // The text. Replace "<X>" with total episode / chapter / volume count later.
        Label progressLabel = new Label(" / <X> " + unit);
        progressLabel.setStyle("-fx-text-fill: white");
        progressLabel.setMinHeight(30);
        progressLabel.setMaxHeight(30);
        progressLabel.setAlignment(Pos.CENTER_RIGHT);
        progressLabel.setMinWidth(Region.USE_PREF_SIZE);

        progressTracker.getChildren().addAll(numberInput, progressLabel);
        return progressTracker;
    }

    private VBox createRightPopupContent(AnimeInfo anime) {
        VBox metaStatsAndSaveWrapper = new VBox();
        VBox.setVgrow(metaStatsAndSaveWrapper, Priority.ALWAYS);
        HBox.setHgrow(metaStatsAndSaveWrapper, Priority.ALWAYS);
        metaStatsAndSaveWrapper.getStyleClass().add("grid-media-popup-right");

        ScrollPane synopsis = createSynopsis(anime.getSynopsis());
        FlowGridPane metaInfo = createMetaInfo(anime);
        HBox saveButton = createSaveButton(metaStatsAndSaveWrapper);

        metaStatsAndSaveWrapper.getChildren().addAll(metaInfo, synopsis, saveButton);
        return metaStatsAndSaveWrapper;
    }

    private ScrollPane createSynopsis(String synopsis) {

        Label synopsisLabel = new Label(synopsis);
        synopsisLabel.setWrapText(true);
        synopsisLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16;");
        VBox content = new VBox(synopsisLabel);


        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("popup-scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        scrollPane.setStyle("-fx-padding: 10;");

        // Smooth scroll listener
        // in /external/
        new SmoothScroll(scrollPane, content, 80);

        return scrollPane;
    }



    private FlowGridPane createMetaInfo(AnimeInfo anime) {
        FlowGridPane metaInfo = new FlowGridPane(3, 2);
        metaInfo.setHgap(20);
        metaInfo.setVgap(20);
        HBox.setHgrow(metaInfo, Priority.ALWAYS);
        metaInfo.setMaxWidth(Double.MAX_VALUE);

        metaInfo.setMinHeight(Region.USE_PREF_SIZE);
        metaInfo.setMaxHeight(Region.USE_PREF_SIZE);


        metaInfo.setStyle("-fx-padding: 10;");

        BiFunction<String, String, VBox> createPropertyBox = (labelText, contentText) -> {
            Label label = new Label(labelText);
            label.getStyleClass().add("filter-label");
            Label content = new Label(contentText);
            content.getStyleClass().add("filter-label");
            // VBox propertyBox = new VBox(5, label, content);
            return new VBox(5, label, content);
        };

        metaInfo.getChildren().addAll(
                createPropertyBox.apply("Release", anime.getRelease()),
                createPropertyBox.apply("Total Episodes", String.valueOf(anime.getEpisodesTotal())),
                createPropertyBox.apply("Publication Status", anime.getPublicationStatus()),
                createPropertyBox.apply("Source", anime.getSource()),
                createPropertyBox.apply("Age Rating", anime.getAgeRating()),
                createPropertyBox.apply("Studios", anime.getStudios())
        );

        return metaInfo;
    }


    private HBox createSaveButton(VBox wrapper) {
        HBox saveButtonWrapper = new HBox();
        HBox.setHgrow(saveButtonWrapper, Priority.ALWAYS);
        saveButtonWrapper.setMaxWidth(Double.MAX_VALUE);
        saveButtonWrapper.setPadding(new Insets(10));
        saveButtonWrapper.minHeightProperty().bind(wrapper.heightProperty().multiply(0.1));
        saveButtonWrapper.maxHeightProperty().bind(wrapper.heightProperty().multiply(0.1));
        saveButtonWrapper.setStyle("-fx-padding: 0 10 10 0; -fx-border-color: white; -fx-border-width: 0 0 2 0;");

        Button saveButton = new Button("Save changes");
        saveButtonWrapper.setAlignment(Pos.CENTER_RIGHT);
        saveButton.getStyleClass().add("controls-button");
        saveButton.prefHeightProperty().bind(saveButtonWrapper.heightProperty().subtract(10));

        saveButtonWrapper.getChildren().add(saveButton);
        return saveButtonWrapper;
    }
}
