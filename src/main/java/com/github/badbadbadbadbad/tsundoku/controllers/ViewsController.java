package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.ConfigModel;
import com.github.badbadbadbadbad.tsundoku.views.AnimeGridView;
import com.github.badbadbadbadbad.tsundoku.views.SidebarView;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;


public class ViewsController implements LoadingBarListener, ConfigListener {

    private final HBox root;
    private final Stage stage;

    private boolean firstTimeStartup = true;
    private final APIController apiController;
    private final ConfigController configController;
    private final DatabaseController databaseController;
    public Region loadingBar;


    public ViewsController(Stage stage, APIController apiController, ConfigController configController, ConfigModel configModel, DatabaseController databaseController) {
        this.apiController = apiController;
        this.configController = configController;
        this.databaseController = databaseController;
        this.stage = stage;

        this.root = new HBox();
        root.setId("main-root");
        root.getStyleClass().add("root");

        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();
        double screenHeight = screen.getBounds().getHeight();


        // Starts chain of events that invokes onSidebarModesUpdates() of this class to handle setup of all view elements
        configModel.addConfigListener(this);


        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/CSS/styles.css").toExternalForm());
        scene.setFill(Color.rgb(35, 36, 42)); // To prevent white flicker on expanding resize



        if (screenWidth > screenHeight) { // "Normal" monitor
            stage.setWidth(screenWidth / 2);
            stage.setMinWidth(screenWidth / 2);
            stage.setMaxWidth(screenWidth / 2);

            stage.setHeight(screenHeight / 1.5);
            stage.setMinHeight(screenHeight / 1.7);
        } else { // Vertical monitor
            stage.setWidth(screenWidth);
            stage.setMinWidth(screenWidth);
            stage.setMaxWidth(screenWidth);

            stage.setHeight(screenHeight / 1.5);
            stage.setMinHeight(screenHeight / 2);
        }


        stage.setScene(scene);
    }


    private Region createLoadingSeparator() {

        // Empty loading bar as separator between sidebar and content
        Region background = new Region();
        background.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));

        // Loading bar overlaid on empty loading bar
        loadingBar = new Region();
        loadingBar.setMinHeight(0);
        loadingBar.setMaxHeight(0);
        loadingBar.setBackground(new Background(new BackgroundFill(Color.GOLDENROD, CornerRadii.EMPTY, Insets.EMPTY)));

        StackPane separator = new StackPane(background, loadingBar);


        background.getStyleClass().add("loading-seperator");
        loadingBar.getStyleClass().add("loading-seperator");
        separator.getStyleClass().add("loading-seperator");

        return separator;
    }


    private void updateMainContent(String mediaMode, String browseMode) {
        Region gridView = null;

        switch (mediaMode) {
            case "Anime" -> {
                AnimeGridView animeGridView = new AnimeGridView(stage, this, apiController, configController, databaseController); // TODO Give anime grid initial filters
                gridView = animeGridView.createGridView();
            }
            case "Manga" -> {
                AnimeGridView animeGridView = new AnimeGridView(stage, this, apiController, configController, databaseController);
                gridView = animeGridView.createGridView();
            }
            case "Games" -> {
                AnimeGridView animeGridView = new AnimeGridView(stage, this, apiController, configController, databaseController);
                gridView = animeGridView.createGridView();
            }
            case "Profile" -> {
                AnimeGridView animeGridView = new AnimeGridView(stage, this, apiController, configController, databaseController);
                gridView = animeGridView.createGridView();
            }
            case "Settings" -> {
                AnimeGridView animeGridView = new AnimeGridView(stage, this, apiController, configController, databaseController);
                gridView = animeGridView.createGridView();
            }
        }

        if (root.getChildren().size() > 2) { // Content pane exists, remove it and add new one
            root.getChildren().remove(2);
        }
        root.getChildren().add(gridView);
    }

    @Override
    public void animateLoadingBar(double toPercent, double durationSeconds) {
        // Calculate the heights as a percentage of the parent region's height
        double parentHeight = loadingBar.getParent().getLayoutBounds().getHeight();
        double toHeight = (toPercent / 100.0) * parentHeight;

        // Animation for minHeight
        Timeline minHeightAnimation = new Timeline();
        KeyValue minKeyValue = new KeyValue(loadingBar.minHeightProperty(), toHeight);
        KeyFrame minKeyFrame = new KeyFrame(Duration.seconds(durationSeconds), minKeyValue);
        minHeightAnimation.getKeyFrames().add(minKeyFrame);

        // Animation for maxHeight
        Timeline maxHeightAnimation = new Timeline();
        KeyValue maxKeyValue = new KeyValue(loadingBar.maxHeightProperty(), toHeight);
        KeyFrame maxKeyFrame = new KeyFrame(Duration.seconds(durationSeconds), maxKeyValue);
        maxHeightAnimation.getKeyFrames().add(maxKeyFrame);

        // Play both animations simultaneously using ParallelTransition
        ParallelTransition parallelTransition = new ParallelTransition(minHeightAnimation, maxHeightAnimation);
        parallelTransition.play();
    }

    @Override
    public void fadeOutLoadingBar(double durationSeconds) {
        FadeTransition fadeTransition = new FadeTransition(Duration.seconds(durationSeconds), loadingBar);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.0);
        fadeTransition.setOnFinished(event -> {
            // Reset the loading bar
            loadingBar.setMinHeight(0);
            loadingBar.setMaxHeight(0);
            loadingBar.setOpacity(1.0);
        });
        fadeTransition.play();
    }


    @Override
    public void onSidebarModesUpdated(String mediaMode, String browseMode) {
        if (firstTimeStartup) {
            SidebarView sidebarView = new SidebarView(mediaMode, browseMode);
            sidebarView.setSidebarListener(configController);
            Region sidebar = sidebarView.createSidebar();

            Region loadingSeparator = createLoadingSeparator();

            root.getChildren().addAll(sidebar, loadingSeparator);

            firstTimeStartup = false;
        }
        updateMainContent(mediaMode, browseMode);
    }
}
