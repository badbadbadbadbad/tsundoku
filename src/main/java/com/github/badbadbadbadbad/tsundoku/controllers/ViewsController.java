package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.models.ConfigModel;
import com.github.badbadbadbadbad.tsundoku.views.*;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Map;


public class ViewsController implements LoadingBarListener, ConfigListener {

    private final HBox root;
    private final StackPane rootStack;
    private final Stage stage;

    private boolean firstTimeStartup = true;
    private final APIController apiController;
    private final ConfigController configController;
    private final DatabaseController databaseController;
    public Region loadingBar;

    private LazyLoaderView currentLazyLoaderView = null;

    private String languagePreference = "Default";


    public ViewsController(Stage stage, APIController apiController, ConfigController configController, ConfigModel configModel, DatabaseController databaseController) {
        this.apiController = apiController;
        this.configController = configController;
        this.databaseController = databaseController;
        this.stage = stage;

        this.root = new HBox();
        root.setId("main-root");
        root.getStyleClass().add("root");

        this.rootStack = new StackPane();
        rootStack.getChildren().add(root);


        // Starts chain of events that invokes onSidebarModesUpdates() of this class to handle setup of all view elements
        configModel.addConfigListener(this);


        // Scene scene = new Scene(root);
        Scene scene = new Scene(rootStack);


        // Fonts
        Font.loadFont(getClass().getResource("/fonts/NotoSerifJP-Bold.ttf").toExternalForm(), -1);
        Font.loadFont(getClass().getResource("/fonts/NotoSansJP-Regular.ttf").toExternalForm(), -1);
        Font.loadFont(getClass().getResource("/fonts/Montserrat-Medium.ttf").toExternalForm(), -1);
        Font.loadFont(getClass().getResource("/fonts/Montserrat-Italic.ttf").toExternalForm(), -1);


        // Figuring out the internal names used for the fonts by JavaFX
        // Font font1 = Font.loadFont(getClass().getResource("/fonts/Montserrat-Medium.ttf").toExternalForm(), -1);
        // Font font2 = Font.loadFont(getClass().getResource("/fonts/Montserrat-Italic.ttf").toExternalForm(), -1);
        // System.out.println(String.format("Fonts loaded, names to be used in CSS: %s %s", font1.getName(), font2.getName()));


        // CSS
        scene.getStylesheets().add(getClass().getResource("/CSS/styles.css").toExternalForm());


        // To prevent white flicker on expanding resize
        scene.setFill(Color.rgb(35, 36, 42));




        // There is some bug here specific to Windows. On Linux, this works fine to set width to 50% of monitor.
        // On Windows, it's more like 48%. Yet, the screen's dimensions of my setup are correctly found as 1920x1080.
        // Unsure what's causing it, could not find a fix.
        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();
        double screenHeight = screen.getBounds().getHeight();

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

        // Dark background to overlay when switching to a browse mode
        VBox darkBackground = new VBox();
        darkBackground.getStyleClass().add("grid-media-popup-background");
        VBox.setVgrow(darkBackground, Priority.ALWAYS);
        HBox.setHgrow(darkBackground, Priority.ALWAYS);
        darkBackground.setOpacity(0);

        if (currentLazyLoaderView != null) {
            currentLazyLoaderView.shutdownLazyLoader();
        }

        switch (mediaMode) {
            case "Anime" -> {
                if (browseMode.equals("Browse")) {


                    if (firstTimeStartup) {

                        AnimeBrowseView animeBrowseView = new AnimeBrowseView(stage, this, apiController, configController, databaseController, languagePreference); // TODO Give anime grid initial filters
                        Region gridView = animeBrowseView.createGridView();
                        currentLazyLoaderView = null;


                        if (root.getChildren().size() > 2) { // Content pane exists, remove it and add new one
                            root.getChildren().remove(2);
                        }
                        root.getChildren().add(gridView);


                    } else {
                        darkenWindow(darkBackground, 0.8, () -> {
                            AnimeBrowseView animeBrowseView = new AnimeBrowseView(stage, this, apiController, configController, databaseController, languagePreference); // TODO Give anime grid initial filters
                            Region gridView = animeBrowseView.createGridView();
                            currentLazyLoaderView = null;

                            if (root.getChildren().size() > 2) { // Content pane exists, remove it and add new one
                                root.getChildren().remove(2);
                            }
                            root.getChildren().add(gridView);

                            undarkdenWindow(darkBackground, 0.8);
                        });
                    }

                    firstTimeStartup = false;

                } else {
                    AnimeLogView animeLogView = new AnimeLogView(databaseController, languagePreference);
                    Region gridView = animeLogView.createGridView();
                    currentLazyLoaderView = animeLogView;

                    if (root.getChildren().size() > 2) { // Content pane exists, remove it and add new one
                        root.getChildren().remove(2);
                    }
                    root.getChildren().add(gridView);

                    firstTimeStartup = false;
                }
            }
            case "Manga" -> {
                if (browseMode.equals("Browse")) {

                    System.out.println("Manga!");

                    if (firstTimeStartup) {

                        AnimeBrowseView animeBrowseView = new AnimeBrowseView(stage, this, apiController, configController, databaseController, languagePreference); // TODO Give anime grid initial filters
                        Region gridView = animeBrowseView.createGridView();
                        currentLazyLoaderView = null;

                        if (root.getChildren().size() > 2) { // Content pane exists, remove it and add new one
                            root.getChildren().remove(2);
                        }
                        root.getChildren().add(gridView);

                    } else {
                        darkenWindow(darkBackground, 0.8, () -> {
                            AnimeBrowseView animeBrowseView = new AnimeBrowseView(stage, this, apiController, configController, databaseController, languagePreference); // TODO Give anime grid initial filters
                            Region gridView = animeBrowseView.createGridView();
                            currentLazyLoaderView = null;

                            if (root.getChildren().size() > 2) { // Content pane exists, remove it and add new one
                                root.getChildren().remove(2);
                            }
                            root.getChildren().add(gridView);

                            undarkdenWindow(darkBackground, 0.8);
                        });
                    }

                    firstTimeStartup = false;

                } else {
                    AnimeLogView animeLogView = new AnimeLogView(databaseController, languagePreference);
                    Region gridView = animeLogView.createGridView();
                    currentLazyLoaderView = animeLogView;

                    if (root.getChildren().size() > 2) { // Content pane exists, remove it and add new one
                        root.getChildren().remove(2);
                    }
                    root.getChildren().add(gridView);

                    firstTimeStartup = false;
                }
            }
            case "Games" -> {
                if (browseMode.equals("Browse")) {


                    System.out.println("Games!!");

                    if (firstTimeStartup) {

                        AnimeBrowseView animeBrowseView = new AnimeBrowseView(stage, this, apiController, configController, databaseController, languagePreference); // TODO Give anime grid initial filters
                        Region gridView = animeBrowseView.createGridView();
                        currentLazyLoaderView = null;

                        if (root.getChildren().size() > 2) { // Content pane exists, remove it and add new one
                            root.getChildren().remove(2);
                        }
                        root.getChildren().add(gridView);

                    } else {
                        darkenWindow(darkBackground, 0.8, () -> {
                            AnimeBrowseView animeBrowseView = new AnimeBrowseView(stage, this, apiController, configController, databaseController, languagePreference); // TODO Give anime grid initial filters
                            Region gridView = animeBrowseView.createGridView();
                            currentLazyLoaderView = null;

                            if (root.getChildren().size() > 2) { // Content pane exists, remove it and add new one
                                root.getChildren().remove(2);
                            }
                            root.getChildren().add(gridView);

                            undarkdenWindow(darkBackground, 0.8);
                        });
                    }

                    firstTimeStartup = false;

                } else {
                    AnimeLogView animeLogView = new AnimeLogView(databaseController, languagePreference);
                    Region gridView = animeLogView.createGridView();
                    currentLazyLoaderView = animeLogView;

                    if (root.getChildren().size() > 2) { // Content pane exists, remove it and add new one
                        root.getChildren().remove(2);
                    }
                    root.getChildren().add(gridView);

                    firstTimeStartup = false;
                }
            }
            case "Profile" -> {
                AnimeBrowseView animeBrowseView = new AnimeBrowseView(stage, this, apiController, configController, databaseController, languagePreference);
                Region gridView = animeBrowseView.createGridView();
                currentLazyLoaderView = null;
            }
            case "Settings" -> {

                // Get current settings
                Map<String, Object> currentSettings = configController.getCurrentSettings();

                SettingsView settingsView = new SettingsView(configController, currentSettings);
                Region settingsViewRegion = settingsView.createSettingsView();


                currentLazyLoaderView = null;

                if (root.getChildren().size() > 2) { // Content pane exists, remove it and add new one
                    root.getChildren().remove(2);
                }
                root.getChildren().add(settingsViewRegion);

                firstTimeStartup = false;
            }
        }
    }


    private void darkenWindow(Node node, double finalOpacity, Runnable onFinished) {

        rootStack.getChildren().add(node);

        // Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.2), node);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(finalOpacity);

        fadeIn.setOnFinished(e -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });

        fadeIn.play();

    }


    private void undarkdenWindow(Node node, double startingOpacity) {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.2), node);
        fadeOut.setFromValue(startingOpacity);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> rootStack.getChildren().remove(node));
        fadeOut.play();
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

            // firstTimeStartup = false;
        }
        updateMainContent(mediaMode, browseMode);
    }


    @Override
    public void onLanguagePreferenceUpdated(String language) {
        this.languagePreference = language;
    }


    public void shutdownLazyLoader() {
        if (currentLazyLoaderView != null) {
            currentLazyLoaderView.shutdownLazyLoader();
        }
    }
}
