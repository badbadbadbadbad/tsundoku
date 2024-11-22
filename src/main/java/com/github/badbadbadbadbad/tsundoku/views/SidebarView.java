package com.github.badbadbadbadbad.tsundoku.views;

import com.github.badbadbadbadbad.tsundoku.controllers.SidebarListener;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SidebarView {

    private static final double SIDEBAR_WIDTH = 0.12; // Percent of screen width, not scaling with window size for now

    private SidebarListener sidebarListener;

    private final List<Button> mediaModeButtons = new ArrayList<>();
    private final List<Button> browseModeButtons = new ArrayList<>();

    private String currentMediaMode;
    private String currentBrowseMode;

    public SidebarView(String mediaMode, String browseMode) {
        this.currentMediaMode = mediaMode;
        this.currentBrowseMode = browseMode;
    }

    public Region createSidebar() {

        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();

        VBox sidebar = new VBox();
        sidebar.setId("sidebar");

        double adjustedSidebarWidth = screenWidth * SIDEBAR_WIDTH;
        sidebar.setMinWidth(adjustedSidebarWidth);
        sidebar.setMaxWidth(adjustedSidebarWidth);


        // Label programLabel = new Label("tsundoku.");
        // Label programLabel = new Label("つんどく｡");       // Full-width dot
        Label programLabel = new Label("つんどく｡");      // Half-width dot
        programLabel.setId("main-label");

        Region separator = new Region();
        separator.getStyleClass().add("separator");


        // Mode selector buttons
        Button browseButton = createBrowseModeButton("Browse");
        Button logButton = createBrowseModeButton("Log");

        browseButton.setId("sidebar-browse-mode-browse-button");
        logButton.setId("sidebar-browse-mode-log-button");

        Collections.addAll(browseModeButtons, browseButton, logButton);
        HBox browseModeButtonBox = new HBox(browseButton, logButton);
        browseModeButtonBox.setPadding(new Insets(5, 0, 0, 0));


        // Media type buttons
        Button gamesButton = createMediaModeButton("Games");
        Button mangaButton = createMediaModeButton("Manga");
        Button animeButton = createMediaModeButton("Anime");

        // Empty space between media types and meta stuff
        Region stretchRegion = new Region();
        VBox.setVgrow(stretchRegion, Priority.ALWAYS);

        // Meta stuff buttons
        Button profileButton = createMediaModeButton("Profile");
        Button settingsButton = createMediaModeButton("Settings");

        Collections.addAll(mediaModeButtons, gamesButton, mangaButton, animeButton, profileButton, settingsButton);
        sidebar.getChildren().addAll(programLabel, separator, browseModeButtonBox, gamesButton, mangaButton, animeButton, stretchRegion, profileButton, settingsButton);
        return sidebar;
    }

    // TODO Combine button creator functions
    private Button createBrowseModeButton(String label) {
        Button button = new Button(label);
        // button.getStyleClass().addAll("controls-button", "sidebar-browse-mode-button");
        button.getStyleClass().add("sidebar-browse-mode-button");

        button.setOnAction(e -> {
            if (!button.getText().equals(currentBrowseMode)) {
                setActiveBrowseModeButton(button);
            }
        });

        if (label.equals(currentBrowseMode)) {
            button.getStyleClass().add("sidebar-browse-mode-button-active");
        }

        return button;
    }

    private void setActiveBrowseModeButton(Button selectedButton) {
        for (Button button : browseModeButtons) {
            button.getStyleClass().removeAll("sidebar-browse-mode-button-active");
        }
        selectedButton.getStyleClass().add("sidebar-browse-mode-button-active");
        currentBrowseMode = selectedButton.getText();
        sidebarListener.onSidebarBrowseModeChanged(selectedButton.getText());
    }

    private Button createMediaModeButton(String label) {
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("sidebar-media-button");

        button.setOnAction(e -> {
            if (!button.getText().equals(currentMediaMode)) {
                setActiveMediaModeButton(button);
            }
        });

        if (label.equals(currentMediaMode)) {
            button.getStyleClass().add("sidebar-media-button-active");
        }

        return button;
    }

    private void setActiveMediaModeButton(Button selectedButton) {
        for (Button button : mediaModeButtons) {
            button.getStyleClass().removeAll("sidebar-media-button-active");
        }
        selectedButton.getStyleClass().add("sidebar-media-button-active");
        currentMediaMode = selectedButton.getText();
        sidebarListener.onSidebarMediaModeChanged(selectedButton.getText());
    }


    public void setSidebarListener(SidebarListener sidebarListener) {
        this.sidebarListener = sidebarListener;
    }

}
