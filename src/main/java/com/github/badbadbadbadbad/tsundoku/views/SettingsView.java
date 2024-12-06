package com.github.badbadbadbadbad.tsundoku.views;

import com.github.badbadbadbadbad.tsundoku.controllers.*;
import com.github.badbadbadbadbad.tsundoku.external.SmoothScroll;
import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;

public class SettingsView {

    private final SettingsListener settingsListener;

    private boolean firstSettingsItemCreated;

    private ScrollPane scrollPane;
    private SmoothScroll smoothScroll;
    private Button saveButton;

    private Map<String, Object> settings;


    public SettingsView(SettingsListener settingsListener, Map<String, Object> currentSettings) {
        this.settingsListener = settingsListener;

        this.settings = currentSettings;

        this.firstSettingsItemCreated = false;
    }


    public Region createSettingsView() {

        VBox root = new VBox();
        VBox.setVgrow(root, Priority.ALWAYS);
        HBox.setHgrow(root, Priority.ALWAYS);



        HBox saveButtonElement = createSaveButtonElement();

        this.scrollPane = createScrollableSettings();


        // ScrollPane listener to give controls a bottom border when scrolling
        Region separator = new Region();
        separator.getStyleClass().add("separator");
        separator.setOpacity(0.0);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.2), separator);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.2), separator);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> {

            // This is so the controls-bottom-border can't start showing if the pane scroll bar is fully vertical (no scrolling possible)
            boolean canScroll = scrollPane.getContent().getBoundsInLocal().getHeight() > scrollPane.getViewportBounds().getHeight();
            if (newValue.doubleValue() < 0.99 && canScroll) {
                if (separator.getOpacity() == 0.0) {
                    fadeIn.playFromStart();
                }
            } else {
                if (separator.getOpacity() == 1.0) {
                    fadeOut.playFromStart();
                }
            }
        });


        root.getChildren().addAll(scrollPane, separator, saveButtonElement);
        return root;
    }


    private HBox createSaveButtonElement() {
        HBox saveButtonWrapper = new HBox();
        HBox.setHgrow(saveButtonWrapper, Priority.ALWAYS);
        saveButtonWrapper.setMaxWidth(Double.MAX_VALUE);


        saveButtonWrapper.setStyle("-fx-padding: 15 15 15 15; -fx-min-height: 65; -fx-max-height: 65;");

        this.saveButton = new Button("Save");
        saveButtonWrapper.setAlignment(Pos.CENTER_RIGHT);
        saveButton.getStyleClass().add("controls-button");


        // Initialize as disabled (because no need to save settings if none have been changed yet
        saveButton.setDisable(true);


        saveButton.setOnAction(e -> {

            // Disable after save (is re-enabled on any settings change)
            saveButton.setDisable(true);

            // Bundle settings

            // Fire new settings towards configModel so it updates its states and passes the settings on
            settingsListener.onSettingsChanged(settings);

        });

        saveButtonWrapper.getChildren().add(saveButton);
        return saveButtonWrapper;
    }


    private ScrollPane createScrollableSettings() {

        VBox wrapper = new VBox();
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setStyle("-fx-padding: 15 10 0 10;");


        // Language preference
        VBox languagePreferenceSetting = makeSingleInputComboboxSetting(
                "Title Language Preference",
                "The language used for titles of anime and manga, if provided. \"Default\" will generally mean the Japanese title in Roumaji.",
                List.of("Default", "Japanese", "English"),
                (String) this.settings.get("weebLanguagePreference")
        );

        ComboBox<String> comboBox = (ComboBox<String>) ((HBox) languagePreferenceSetting.getChildren().get(1)).getChildren().get(1);
        comboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.settings.put("weebLanguagePreference", newValue);
            this.saveButton.setDisable(false);
        });


        ScrollPane scrollPane = new ScrollPane(wrapper);
        scrollPane.getStyleClass().add("grid-scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        this.smoothScroll = new SmoothScroll(scrollPane, wrapper);


        wrapper.getChildren().addAll(languagePreferenceSetting);

        return scrollPane;
    }


    private VBox makeSingleInputComboboxSetting(String headerText, String descriptionText, List<String> comboBoxItems, String defaultSelectedItem) {
        VBox wrapper = new VBox(5);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setStyle("-fx-padding: 0 0 10 0;");


        // Separator between settings boxes. Each box gets a top separator.
        // Except for the very first one, which makes all settings have a separator towards other settings.
        if (firstSettingsItemCreated) {
            Region separator = new Region();
            separator.getStyleClass().add("separator-thin");

            wrapper.getChildren().add(separator);
        }


        // Settings item header
        Label headerLabel = new Label(headerText);
        headerLabel.getStyleClass().add("settings-header-text");
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(headerLabel, Priority.NEVER);

        // Settings item content wrapper
        HBox subWrapper = new HBox(20);
        subWrapper.setMaxWidth(Double.MAX_VALUE);

        // Settings item content, left: Description
        Label descriptionLabel = new Label(descriptionText);
        descriptionLabel.getStyleClass().add("settings-description-text");
        HBox.setHgrow(descriptionLabel, Priority.ALWAYS);

        // Settings item content, right: Settings input
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getStyleClass().add("settings-combo-box");
        comboBox.getItems().addAll(comboBoxItems);
        comboBox.setValue(defaultSelectedItem);


        subWrapper.getChildren().addAll(descriptionLabel, comboBox);


        wrapper.getChildren().addAll(headerLabel, subWrapper);

        this.firstSettingsItemCreated = true;
        return wrapper;
    }


    private VBox makeMultiInputComboxboxSetting() {
        VBox wrapper = new VBox();
        wrapper.setMaxWidth(Double.MAX_VALUE);

        if (firstSettingsItemCreated) {
            Region separator = new Region();
            separator.getStyleClass().add("separator-thin");

            wrapper.getChildren().add(separator);
        }

        this.firstSettingsItemCreated = true;
        return wrapper;
    }


    private VBox makeTextInputComboboxSetting() {
        VBox wrapper = new VBox();
        wrapper.setMaxWidth(Double.MAX_VALUE);

        if (firstSettingsItemCreated) {
            Region separator = new Region();
            separator.getStyleClass().add("separator-thin");

            wrapper.getChildren().add(separator);
        }

        this.firstSettingsItemCreated = true;
        return wrapper;
    }
}
