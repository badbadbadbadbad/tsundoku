package com.github.badbadbadbadbad.tsundoku.views;

import com.fasterxml.jackson.databind.JsonNode;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainWindowView {

    private static final double SIDEBAR_WIDTH = 0.15;

    public MainWindowView(Stage stage, JsonNode mediaData) {

        HBox root = new HBox();

        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();
        double screenHeight = screen.getBounds().getHeight();

        SidebarView sidebarView = new SidebarView();
        Region sidebar = sidebarView.createSidebar(this::loadSidebarContent);

        // Region mainContent = createMainContent();
        AnimeGridView animeGridView = new AnimeGridView(mediaData);
        Region gridView = animeGridView.createGridView(stage);

        root.getChildren().addAll(sidebar, createSeparator(), gridView);

        Scene scene = new Scene(root);
        //root.setBackground(new Background(new BackgroundFill(Color.rgb(45, 47, 56), CornerRadii.EMPTY, Insets.EMPTY)));
        root.setBackground(new Background(new BackgroundFill(Color.rgb(35, 36, 42), CornerRadii.EMPTY, Insets.EMPTY)));
        scene.setFill(Color.rgb(35, 36, 42)); // To prevent white flicker on expanding resize


        stage.setWidth(screenWidth / 1.5);
        stage.setHeight(screenHeight / 1.5);
        stage.setMinWidth(screenWidth / 2);
        stage.setMinHeight(screenHeight / 2);

        stage.setScene(scene);

    }

    private void loadSidebarContent(String contentName) {
        System.out.println("Loading " + contentName);
    }



    private Region createMainContent() {
        Region mainContent = new Region();
        mainContent.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
        HBox.setHgrow(mainContent, Priority.ALWAYS);
        return mainContent;
    }

    private Region createSeparator() {
        Region separator = new Region();
        separator.setMinWidth(2);
        separator.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
        return separator;
    }
}
