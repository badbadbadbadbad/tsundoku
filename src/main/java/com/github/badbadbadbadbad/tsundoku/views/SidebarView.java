package com.github.badbadbadbadbad.tsundoku.views;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class SidebarView {

    private static final double SIDEBAR_WIDTH = 0.166;
    // private static final Color SIDEBAR_COLOR = Color.rgb(35, 36, 42);
    private static final Color SIDEBAR_COLOR = Color.rgb(45, 47, 56);

    private Button activeButton = null;
    private final List<Button> modeButtons;
    private Consumer<String> contentChangeListener;
    
    public SidebarView() {
        this.modeButtons = new ArrayList<>();
    }

    public Region createSidebar(Consumer<String> contentChangeListener) {
        this.contentChangeListener = contentChangeListener;

        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();

        VBox sidebar = new VBox();

        double adjustedSidebarWidth = screenWidth * SIDEBAR_WIDTH;
        sidebar.setMinWidth(adjustedSidebarWidth);
        sidebar.setMaxWidth(adjustedSidebarWidth);
        sidebar.setBackground(new Background(new BackgroundFill(SIDEBAR_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));
        sidebar.setPadding(new Insets(10));
        sidebar.setSpacing(10);
        sidebar.setAlignment(Pos.CENTER);

        Label programLabel = new Label("tsundoku.");
        programLabel.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;");

        Region separator = new Region();
        separator.setPrefHeight(1);
        separator.setBackground(new Background(new BackgroundFill(Color.ORANGE, CornerRadii.EMPTY, Insets.EMPTY)));
        
        Button gamesButton = createModeButton("Games");
        Button mangaButton = createModeButton("Manga");
        Button animeButton = createModeButton("Anime");

        Region stretchRegion = new Region();
        VBox.setVgrow(stretchRegion, Priority.ALWAYS);

        Button profileButton = createModeButton("Profile");
        Button settingsButton = createModeButton("Settings");


        Collections.addAll(modeButtons, gamesButton, mangaButton, animeButton, profileButton, settingsButton);
        sidebar.getChildren().addAll(programLabel, separator, gamesButton, mangaButton, animeButton, stretchRegion, profileButton, settingsButton);

        
        return sidebar;
    }

    private Button createModeButton(String label) {
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px;");

        button.setOnMouseEntered(e -> {
            if (!button.equals(activeButton)) {
                button.setStyle("-fx-background-color: #353538; -fx-text-fill: white; -fx-font-size: 18px;");
            } else {
                button.setStyle("-fx-background-color: #57575e; -fx-text-fill: white; -fx-font-size: 18px;");
            }
        });

        button.setOnMouseExited(e -> {
            if (!button.equals(activeButton)) {
                button.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px;");
            } else {
                button.setStyle("-fx-background-color: #45454A; -fx-text-fill: white; -fx-font-size: 18px;");
            }
        });

        button.setOnAction(e -> {
            setActiveButton(button);
            contentChangeListener.accept(label);
        });

        return button;
    }

    private void setActiveButton(Button selectedButton) {
        for (Button button : modeButtons) {
            button.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 18px;");
        }
        selectedButton.setStyle("-fx-background-color: #57575e; -fx-text-fill: white; -fx-font-size: 18px;");
        activeButton = selectedButton;
    }

}
