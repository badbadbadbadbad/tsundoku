package com.github.badbadbadbadbad.tsundoku.views;

import com.github.badbadbadbadbad.tsundoku.controllers.SidebarListener;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * The full sidebar component on the left of the main window.
 */
public class SidebarView extends VBox {

    private static final double SIDEBAR_WIDTH = 0.11; // Percent of screen width, not scaling with window size (for now?)

    private final SidebarListener sidebarListener;

    private final List<Button> mediaModeButtons = new ArrayList<>();
    private final List<Button> browseModeButtons = new ArrayList<>();

    private String currentMediaMode;
    private String currentBrowseMode;

    public SidebarView(SidebarListener sidebarListener, String mediaMode, String browseMode) {
        this.sidebarListener = sidebarListener;
        this.currentMediaMode = mediaMode;
        this.currentBrowseMode = browseMode;

        initComponent();
    }

    private void initComponent() {
        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();

        this.setId("sidebar");

        double adjustedSidebarWidth = screenWidth * SIDEBAR_WIDTH;
        this.setMinWidth(adjustedSidebarWidth);
        this.setMaxWidth(adjustedSidebarWidth);


        // Label programLabel = new Label("tsundoku.");
        // Label programLabel = new Label("つんどく｡");       // Full-width dot
        Label programLabel = new Label("つんどく｡");      // Half-width dot
        programLabel.setFont(Font.font("Noto Serif JP Bold", 100));
        programLabel.setId("main-label");


        programLabel.heightProperty().addListener((obs, oldBounds, newBounds) -> {
            if (programLabel.getHeight() > 1.0) {
                adjustFontSizeToContainer(programLabel, 10, -1);
            }
        });


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

        browseButton.heightProperty().addListener((obs, oldBounds, newBounds) -> {
            if (browseButton.getHeight() > 1.0) {
                double tempFontSize = adjustFontSizeToContainer(browseButton, 8, -1);
                logButton.setFont(Font.font("Montserrat Medium", tempFontSize));
            }
        });

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

        settingsButton.heightProperty().addListener((obs, oldBounds, newBounds) -> {
            if (settingsButton.getHeight() > 1.0) {
                double tempFontSize = adjustFontSizeToContainer(settingsButton, 8, -1);
                gamesButton.setFont(Font.font("Montserrat Medium", tempFontSize));
                mangaButton.setFont(Font.font("Montserrat Medium", tempFontSize));
                animeButton.setFont(Font.font("Montserrat Medium", tempFontSize));
                profileButton.setFont(Font.font("Montserrat Medium", tempFontSize));
            }
        });

        Collections.addAll(mediaModeButtons, gamesButton, mangaButton, animeButton, profileButton, settingsButton);
        this.getChildren().addAll(programLabel, separator, browseModeButtonBox, animeButton, stretchRegion, settingsButton);
    }

    private Button createBrowseModeButton(String label) {
        Button button = new Button(label);
        button.getStyleClass().add("sidebar-browse-mode-button");

        button.setFont(Font.font("Montserrat Medium", 20));

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

        button.setFont(Font.font("Montserrat Medium", 26));

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

    /**
     * Adjusts font size on the text of some Labeled (labels, buttons..) downwards until it fits into the rectangular
     * label container without overflow, both vertical and horizontal, padding included in the calculation.
     *
     * @param labeled          The Labeled whose font is to be adjusted
     * @param minFontSize      A minimum font size at which point the routine is forced to stop
     * @param startingFontSize An optional font size to start from instead of the font size the label currently has.
     *                         Ignored if not a positive number.
     * @return The final font size the Lebeled is set to
     */
    private double adjustFontSizeToContainer(Labeled labeled, double minFontSize, double startingFontSize) {
        Font font = labeled.getFont();
        double fontSize = font.getSize();
        String fontFamily = font.getFamily();


        if (startingFontSize > 0) {
            fontSize = startingFontSize;
        }


        Insets insets = labeled.getInsets();
        double containerWidth = labeled.getWidth() - insets.getLeft() - insets.getRight();
        double containerHeight = labeled.getHeight() - insets.getTop() - insets.getBottom();

        // Temporary font needs to be used because JavaFX buggy when overwriting active font.
        Text text = new Text(labeled.getText());
        text.setFont(Font.font(fontFamily, fontSize));


        // Make smaller until no vertical overflow
        while (text.getBoundsInLocal().getHeight() > containerHeight && fontSize > minFontSize) {
            fontSize--;
            text.setFont(Font.font(fontFamily, fontSize));
        }


        // Make smaller until no horizontal overflow
        while (text.getBoundsInLocal().getWidth() > containerWidth && fontSize > minFontSize) {
            fontSize--;
            text.setFont(Font.font(fontFamily, fontSize));
        }

        labeled.setFont(Font.font(fontFamily, fontSize));
        return fontSize;
    }
}
