package com.github.badbadbadbadbad.tsundoku.views;

import com.github.badbadbadbadbad.tsundoku.controllers.*;
import com.github.badbadbadbadbad.tsundoku.external.FlowGridPane;
import com.github.badbadbadbadbad.tsundoku.external.SmoothScroll;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import com.github.badbadbadbadbad.tsundoku.models.AnimeListInfo;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SettingsView {

    private final SettingsListener settingsListener;

    private ScrollPane scrollPane;
    private SmoothScroll smoothScroll;

    private String languagePreference;


    public SettingsView(SettingsListener settingsListener, Map<String, Object> currentSettings) {
        this.settingsListener = settingsListener;
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


        saveButtonWrapper.setStyle("-fx-padding: 10 10 10 0; -fx-min-height: 55; -fx-max-height: 55;");

        Button saveButton = new Button("Save");
        saveButtonWrapper.setAlignment(Pos.CENTER_RIGHT);
        saveButton.getStyleClass().add("controls-button");


        // Initialize as disabled (because no need to save settings if none have been changed yet
        saveButton.setDisable(true);


        saveButton.setOnAction(e -> {

            // Disable after save (is re-enabled on any settings change)
            saveButton.setDisable(true);

            // Fire new settings towards configModel so it updates its states and passes the settings on
            // settingsListener.onSettingsChanged();

        });

        saveButtonWrapper.getChildren().add(saveButton);
        return saveButtonWrapper;
    }

    private ScrollPane createScrollableSettings() {

        VBox wrapper = new VBox();
        wrapper.setMaxWidth(Double.MAX_VALUE);

        ScrollPane scrollPane = new ScrollPane(wrapper);
        scrollPane.getStyleClass().add("grid-scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        this.smoothScroll = new SmoothScroll(scrollPane, wrapper);

        return scrollPane;
    }
}
