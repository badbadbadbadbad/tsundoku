package com.github.badbadbadbadbad.tsundoku.views;

import com.fasterxml.jackson.databind.JsonNode;

import com.github.badbadbadbadbad.tsundoku.controllers.APIController;
import com.github.badbadbadbadbad.tsundoku.controllers.ConfigController;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;

public class MainWindowView {
    private final APIController apiController;
    private final ConfigController configController;
    public Region loadingBar;

    public MainWindowView(Stage stage, APIController apiController, ConfigController configController) {
        this.apiController = apiController;
        this.configController = configController;

        HBox root = new HBox();
        root.setId("main-root");

        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();
        double screenHeight = screen.getBounds().getHeight();

        SidebarView sidebarView = new SidebarView();
        Region sidebar = sidebarView.createSidebar(this::loadSidebarContent);

        Region sep = createSeparator();

        AnimeGridView animeGridView = new AnimeGridView(apiController, configController, loadingBar);
        Region gridView = animeGridView.createGridView(stage);

        root.getChildren().addAll(sidebar, sep, gridView);

        // TODO REMOVE LATER WHEN VIEWS CONTROLLER IS IMPLEMENTED
        configController.listenToAnimeGrid(animeGridView);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/CSS/styles.css").toExternalForm());
        scene.setFill(Color.rgb(35, 36, 42)); // To prevent white flicker on expanding resize

        stage.setWidth(screenWidth / 1.5);
        stage.setHeight(screenHeight / 1.5);
        stage.setMinWidth(screenWidth / 2);
        stage.setMinHeight(screenHeight / 1.7);

        stage.setScene(scene);
    }

    private void loadSidebarContent(String contentName) {
        // System.out.println("Loading " + contentName);
    }


    private Region createSeparator() {
        Region background = new Region();
        background.setMinWidth(2);
        background.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));

        loadingBar = new Region();
        loadingBar.setMinWidth(2);
        loadingBar.setMinHeight(0);
        loadingBar.setMaxHeight(0);
        loadingBar.setBackground(new Background(new BackgroundFill(Color.GOLDENROD, CornerRadii.EMPTY, Insets.EMPTY)));

        StackPane separator = new StackPane(background, loadingBar);
        separator.setMinWidth(2);
        separator.setMaxWidth(2);
        separator.setPrefWidth(2);
        // separator.setMinHeight(200); // Example height, adjust as needed


        return separator;
    }
}
