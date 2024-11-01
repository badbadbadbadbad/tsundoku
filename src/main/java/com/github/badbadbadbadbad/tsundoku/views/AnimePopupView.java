package com.github.badbadbadbadbad.tsundoku.views;

import com.github.badbadbadbadbad.tsundoku.controllers.PopupListener;
import com.github.badbadbadbadbad.tsundoku.external.FlowGridPane;
import com.github.badbadbadbadbad.tsundoku.external.SmoothScroll;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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

    private final PopupListener popupListener;
    private final AnimeInfo anime;                  // The anime data of the grid item that was clicked to create this popup
    private final AnimeInfo databaseAnime;          // The anime data of the matching anime, received from the local database, if it exists.
    private final VBox darkBackground;              // The background surrounding the popup. Needed to call the destruction event.

    private final VBox popupBox;

    public AnimePopupView(AnimeInfo anime, PopupListener popupListener, VBox darkBackground) {
        this.popupBox = new VBox();
        this.popupListener = popupListener;
        this.anime = anime;
        this.darkBackground = darkBackground;

        this.databaseAnime = popupListener.requestAnimeFromDatabase(anime.getId());
    }

    public VBox createPopup() {
        final double screenWidth = Screen.getPrimary().getVisualBounds().getWidth();
        final double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();

        // Full popup container
        popupBox.getStyleClass().add("grid-media-popup");
        popupBox.setMinWidth(screenWidth * 0.35);
        popupBox.setMaxWidth(screenWidth * 0.35);
        popupBox.setMinHeight(screenHeight * 0.5);
        popupBox.setMaxHeight(screenHeight * 0.5);

        // Title
        Label title = createPopupTitle();

        // Remaining content
        HBox contentWrapper = createPopupContent();

        popupBox.getChildren().addAll(title, contentWrapper);

        return popupBox;
    }


    private Label createPopupTitle() {
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


    private HBox createPopupContent() {
        HBox contentWrapper = new HBox();
        HBox.setHgrow(contentWrapper, Priority.ALWAYS);
        VBox.setVgrow(contentWrapper, Priority.ALWAYS);

        VBox leftContent = createLeftPopupContent();
        VBox rightContent = createRightPopupContent();

        contentWrapper.getChildren().addAll(leftContent, rightContent);

        return contentWrapper;
    }


    private VBox createLeftPopupContent() {
        VBox imageAndSelfStatsWrapper = new VBox();
        VBox.setVgrow(imageAndSelfStatsWrapper, Priority.ALWAYS);
        imageAndSelfStatsWrapper.setMinWidth(popupBox.getMaxWidth() * 0.3);
        imageAndSelfStatsWrapper.setMaxWidth(popupBox.getMaxWidth() * 0.3);
        imageAndSelfStatsWrapper.getStyleClass().add("grid-media-popup-left");

        VBox image = createCoverImage(imageAndSelfStatsWrapper);
        ComboBox<String> status = createStatusBox();
        HBox rating = createRatingBox();
        HBox progress = createProgressTracker("episodes");

        imageAndSelfStatsWrapper.getChildren().addAll(image, status, rating, progress);

        return imageAndSelfStatsWrapper;
    }


    private VBox createCoverImage(VBox wrapper) {
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
        status.setMinHeight(30);
        status.setMaxHeight(30);
        status.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(status, Priority.ALWAYS);
        status.setStyle("-fx-background-radius: 10; -fx-border-width: 0 0 0 0; -fx-border-color: transparent; -fx-unfocus-color: transparent;-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        // Colors for the future:
        // Untracked: Grey
        // Backlog: Tsundoku gold (goldenrod)
        // In progress: Blue?
        // Completed: Green
        // Paused: Blue?
        // Dropped: Red

        // Always possible
        status.getItems().addAll("Untracked", "Backlog");

        // Can be completed as long as it finished releasing. Include "Not yet provided" for entries with missing data
        if (anime.getPublicationStatus().equals("Complete") || anime.getPublicationStatus().equals("Not yet provided"))
            status.getItems().add("Completed");

        // Can be watched as long as it started releasing. Include "Not yet provided" for entries with missing data
        if (!anime.getPublicationStatus().equals("Upcoming"))
            status.getItems().addAll("In progress", "Paused", "Dropped");


        if (databaseAnime != null) {
            status.setValue(databaseAnime.getOwnStatus());
            anime.setOwnStatus(databaseAnime.getOwnStatus());
        } else {
            status.setValue("Untracked");
            anime.setOwnStatus("Untracked");
        }


        status.setOnAction(e -> anime.setOwnStatus(status.getValue()));

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
        heartButton.getStyleClass().addAll("ratings-button", "ikonli-heart-inactive", "heart");

        Button thumbsUpButton = new Button();
        thumbsUpButton.setGraphic(new FontIcon(FluentUiFilledMZ.THUMB_LIKE_24));
        thumbsUpButton.getStyleClass().addAll("ratings-button", "ikonli-thumb-inactive", "like");

        Button thumbsDownButton = new Button();
        thumbsDownButton.setGraphic(new FontIcon(FluentUiFilledMZ.THUMB_DISLIKE_24));
        thumbsDownButton.getStyleClass().addAll("ratings-button", "ikonli-thumb-inactive", "dislike");

        HBox.setHgrow(heartButton, Priority.ALWAYS);
        HBox.setHgrow(thumbsUpButton, Priority.ALWAYS);
        HBox.setHgrow(thumbsDownButton, Priority.ALWAYS);
        heartButton.setMaxWidth(Double.MAX_VALUE);
        thumbsUpButton.setMaxWidth(Double.MAX_VALUE);
        thumbsDownButton.setMaxWidth(Double.MAX_VALUE);

        rating.getChildren().addAll(heartButton, thumbsUpButton, thumbsDownButton);

        heartButton.setOnAction(event -> handleRatingButtonClick(heartButton, "ikonli-heart-active", thumbsUpButton, thumbsDownButton));
        thumbsUpButton.setOnAction(event -> handleRatingButtonClick(thumbsUpButton, "ikonli-thumb-active", heartButton, thumbsDownButton));
        thumbsDownButton.setOnAction(event -> handleRatingButtonClick(thumbsDownButton, "ikonli-thumb-active", heartButton, thumbsUpButton));



        anime.setOwnRating("Unscored");


        if (databaseAnime != null) {
            String ownRating = databaseAnime.getOwnRating();
            if (ownRating.equals("Heart")) {
                heartButton.fire();
            } else if (ownRating.equals("Liked")) {
                thumbsUpButton.fire();
            } else if (ownRating.equals("Disliked")) {
                thumbsDownButton.fire();
            }
        }

        return rating;
    }

    // Rework this a bit more in the future when sure of color scheme
    private void handleRatingButtonClick(Button clickedButton, String activeClass, Button... otherButtons) {

        // Set all rating buttons to unlicked state
        for (Button button : otherButtons) {
            if (button.getStyleClass().contains("ikonli-heart-active")) {
                button.getStyleClass().remove("ikonli-heart-active");
                button.getStyleClass().add("ikonli-heart-inactive");
            } else if (button.getStyleClass().contains("ikonli-thumb-active")) {
                button.getStyleClass().remove("ikonli-thumb-active");
                button.getStyleClass().add("ikonli-thumb-inactive");
            }

        }

        // Set clicked button to clicked state
        if (clickedButton.getStyleClass().contains("ikonli-heart-inactive")) {
            clickedButton.getStyleClass().remove("ikonli-heart-inactive");
            clickedButton.getStyleClass().add(activeClass);
        } else if (clickedButton.getStyleClass().contains("ikonli-thumb-inactive")) {
            clickedButton.getStyleClass().remove("ikonli-thumb-inactive");
            clickedButton.getStyleClass().add(activeClass);
        }

        // Update anime data with clicked state
        if (clickedButton.getStyleClass().contains("heart")) {
            anime.setOwnRating("Heart");
        } else if (clickedButton.getStyleClass().contains("like")) {
            anime.setOwnRating("Liked");
        } else if (clickedButton.getStyleClass().contains("dislike")) {
            anime.setOwnRating("Disliked");
        }

    }


    private HBox createProgressTracker(String unit) {
        // Wrapper
        HBox progressTracker = new HBox();
        progressTracker.setMinHeight(30);
        progressTracker.setMaxHeight(30);
        progressTracker.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(progressTracker, Priority.ALWAYS);

        // Input for current progress.
        TextField numberInput = new TextField(String.valueOf(0));
        numberInput.setMinHeight(30);
        numberInput.setMaxHeight(30);
        numberInput.setStyle("-fx-background-radius: 10; -fx-border-radius: 10;");
        HBox.setHgrow(numberInput, Priority.ALWAYS);


        // If anime exists in database, take progress value from there
        if (databaseAnime != null) {
            anime.setEpisodesProgress(databaseAnime.getEpisodesProgress());
            numberInput.setText(String.valueOf(databaseAnime.getEpisodesProgress()));
        } else {
            anime.setEpisodesProgress(0);
        }


        numberInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                numberInput.setText(newValue.replaceAll("\\D", ""));
            }


            int number;
            if (numberInput.getText().equals("")) // Interpret empty field as "0"
                number = 0;
            else
                number = Integer.parseInt(numberInput.getText());


            number = Math.min(number, anime.getEpisodesTotal()); // Clamp value to at most the total amount of episodes

            anime.setEpisodesProgress(number);
        });


        Label progressLabel = new Label(" / " + anime.getEpisodesTotal() + " " + unit);
        progressLabel.setStyle("-fx-text-fill: white");
        progressLabel.setMinHeight(30);
        progressLabel.setMaxHeight(30);
        progressLabel.setAlignment(Pos.CENTER_RIGHT);
        progressLabel.setMinWidth(Region.USE_PREF_SIZE);

        progressTracker.getChildren().addAll(numberInput, progressLabel);
        return progressTracker;
    }


    private VBox createRightPopupContent() {
        VBox metaStatsAndSaveWrapper = new VBox();
        VBox.setVgrow(metaStatsAndSaveWrapper, Priority.ALWAYS);
        HBox.setHgrow(metaStatsAndSaveWrapper, Priority.ALWAYS);
        metaStatsAndSaveWrapper.getStyleClass().add("grid-media-popup-right");

        ScrollPane synopsis = createSynopsis();
        FlowGridPane metaInfo = createMetaInfo();
        HBox saveButton = createSaveButton(metaStatsAndSaveWrapper);

        metaStatsAndSaveWrapper.getChildren().addAll(metaInfo, synopsis, saveButton);
        return metaStatsAndSaveWrapper;
    }


    private ScrollPane createSynopsis() {

        Label synopsisLabel = new Label(anime.getSynopsis());
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


    private FlowGridPane createMetaInfo() {
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
            return new VBox(5, label, content);
        };

        metaInfo.getChildren().addAll(
                createPropertyBox.apply("Release", anime.getRelease()),
                createPropertyBox.apply("Type", anime.getType()),
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


        // Call the same popup destruction as clicking the darkener around the popup does
        saveButton.setOnAction(e -> {

            // Pass the anime data to the database model, where it will be processed accordingly
            popupListener.onAnimeSaveButtonPressed(this.anime);

            // Destroy darkener background and popup after invoking changes saved
            darkBackground.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                true, true, true, true, true, true, true, true, true, true, null));
        });

        saveButtonWrapper.getChildren().add(saveButton);
        return saveButtonWrapper;
    }
}
