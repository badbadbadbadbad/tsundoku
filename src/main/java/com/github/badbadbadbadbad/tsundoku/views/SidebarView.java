package com.github.badbadbadbadbad.tsundoku.views;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class SidebarView {

    private static final double SIDEBAR_WIDTH = 0.166; // Percent of screen width, not scaling with window size for now

    private final List<Button> modeButtons = new ArrayList<>();
    private Consumer<String> contentChangeListener;

    public Region createSidebar(Consumer<String> contentChangeListener) {
        this.contentChangeListener = contentChangeListener;

        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();

        VBox sidebar = new VBox();
        sidebar.setId("sidebar");

        double adjustedSidebarWidth = screenWidth * SIDEBAR_WIDTH;
        sidebar.setMinWidth(adjustedSidebarWidth);
        sidebar.setMaxWidth(adjustedSidebarWidth);

        Label programLabel = new Label("tsundoku.");
        programLabel.setId("main-label");

        Region separator = new Region();
        separator.getStyleClass().add("separator");

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
        button.getStyleClass().add("sidebar-button");

        button.setOnAction(e -> {
            setActiveButton(button);
            contentChangeListener.accept(label);
        });

        return button;
    }

    private void setActiveButton(Button selectedButton) {
        for (Button button : modeButtons) {
            button.getStyleClass().removeAll("sidebar-button-active");
        }
        selectedButton.getStyleClass().add("sidebar-button-active");
    }

}
