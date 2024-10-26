package com.github.badbadbadbadbad.tsundoku.views;

import com.github.badbadbadbadbad.tsundoku.controllers.SidebarListener;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class SidebarView {

    private static final double SIDEBAR_WIDTH = 0.12; // Percent of screen width, not scaling with window size for now

    private SidebarListener sidebarListener;

    private final List<Button> mediaModeButtons = new ArrayList<>();
    private final List<Button> browseModeButtons = new ArrayList<>();
    // private String currentMediaMode = "Anime";
    // private String currentBrowseMode = "Browse";

    private String currentMediaMode; // TODO Inittialize with values from Config
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

        Label programLabel = new Label("tsundoku.");
        programLabel.setId("main-label");

        Region separator = new Region();
        separator.getStyleClass().add("separator");


        // Mode selector buttons
        Button browseButton = createBrowseModeButton("Browse");
        Button logButton = createBrowseModeButton("Log");

        browseButton.setId("browse-mode-browse-button");
        logButton.setId("browse-mode-log-button");


        Collections.addAll(browseModeButtons, browseButton, logButton);

        // Treat them as one component
        HBox browseModeButtonBox = new HBox(browseButton, logButton);


        Button gamesButton = createMediaModeButton("Games");
        Button mangaButton = createMediaModeButton("Manga");
        Button animeButton = createMediaModeButton("Anime");

        Region stretchRegion = new Region();
        VBox.setVgrow(stretchRegion, Priority.ALWAYS);

        Button profileButton = createMediaModeButton("Profile");
        Button settingsButton = createMediaModeButton("Settings");

        Collections.addAll(mediaModeButtons, gamesButton, mangaButton, animeButton, profileButton, settingsButton);
        sidebar.getChildren().addAll(programLabel, separator, browseModeButtonBox, gamesButton, mangaButton, animeButton, stretchRegion, profileButton, settingsButton);
        return sidebar;
    }

    private Button createBrowseModeButton(String label) {
        Button button = new Button(label);
        // button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().addAll("controls-button", "browse-mode-button");

        button.setOnAction(e -> {
            if (!button.getText().equals(currentBrowseMode)) {
                setActiveBrowseModeButton(button);
            }
        });

        if (label.equals(currentBrowseMode)) {
            button.getStyleClass().add("browse-mode-button-active");
        }

        return button;
    }

    private void setActiveBrowseModeButton(Button selectedButton) {
        for (Button button : browseModeButtons) {
            button.getStyleClass().removeAll("browse-mode-button-active");
        }
        selectedButton.getStyleClass().add("browse-mode-button-active");
        currentBrowseMode = selectedButton.getText();
        sidebarListener.onSidebarBrowseModeChanged(selectedButton.getText());
    }

    private Button createMediaModeButton(String label) {
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("sidebar-button");

        button.setOnAction(e -> {
            if (!button.getText().equals(currentMediaMode)) {
                setActiveMediaModeButton(button);
            }
        });

        if (label.equals(currentMediaMode)) {
            button.getStyleClass().add("sidebar-button-active");
        }

        return button;
    }

    private void setActiveMediaModeButton(Button selectedButton) {
        for (Button button : mediaModeButtons) {
            button.getStyleClass().removeAll("sidebar-button-active");
        }
        selectedButton.getStyleClass().add("sidebar-button-active");
        currentMediaMode = selectedButton.getText();
        sidebarListener.onSidebarMediaModeChanged(selectedButton.getText());
    }


    public void setSidebarListener(SidebarListener sidebarListener) {
        this.sidebarListener = sidebarListener;
    }

}
