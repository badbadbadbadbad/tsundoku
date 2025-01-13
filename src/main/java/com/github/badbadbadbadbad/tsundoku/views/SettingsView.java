package com.github.badbadbadbadbad.tsundoku.views;

import com.github.badbadbadbadbad.tsundoku.controllers.*;
import com.github.badbadbadbadbad.tsundoku.external.SmoothScroll;
import javafx.animation.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * The full view component displayed in the main content pane for media mode "Settings".
 */
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


    /**
     * Called once by ViewsController, creates the whole View component
     * @return The finished view
     */
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
                if (separator.getOpacity() == 0.0 || fadeOut.getStatus() == Animation.Status.RUNNING) {
                    fadeOut.stop();
                    fadeIn.playFromStart();
                }
            } else {
                if (separator.getOpacity() == 1.0 || fadeIn.getStatus() == Animation.Status.RUNNING) {
                    fadeIn.stop();
                    fadeOut.playFromStart();
                }
            }
        });


        root.getChildren().addAll(scrollPane, separator, saveButtonElement);
        return root;
    }


    /**
     * Save button component. When clicked, fires a settings update so other components of this program may be updated.
     * @return The finished component
     */
    private HBox createSaveButtonElement() {
        HBox saveButtonWrapper = new HBox();
        HBox.setHgrow(saveButtonWrapper, Priority.ALWAYS);
        saveButtonWrapper.setMaxWidth(Double.MAX_VALUE);


        saveButtonWrapper.setStyle("-fx-padding: 15 15 15 15; -fx-min-height: 65; -fx-max-height: 65;");

        this.saveButton = new Button("Save");
        saveButtonWrapper.setAlignment(Pos.CENTER_LEFT);
        saveButton.getStyleClass().add("controls-button");


        // Initialize as disabled (because no need to save settings if none have been changed yet
        saveButton.setDisable(true);


        saveButton.setOnAction(e -> {

            // Disable after save (is re-enabled on any settings change)
            saveButton.setDisable(true);

            // Fire new settings towards configModel so it updates its states and passes the settings on
            settingsListener.onSettingsChanged(settings);
        });

        saveButtonWrapper.getChildren().add(saveButton);
        return saveButtonWrapper;
    }


    /**
     * Wrapper scrollPane for all the settings (which also creates the settings)
     * @return The finished component
     */
    private ScrollPane createScrollableSettings() {

        VBox wrapper = new VBox();
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setStyle("-fx-padding: 15 10 0 10;");


        // Language preference
        VBox languagePreferenceSetting = makeSingleInputComboboxSetting(
                "Title Language",
                "The language used for titles of anime and manga. Falls back to \"Default\" if data source has not provided alternative versions." +
                        "\n\"Default\" will generally mean the Japanese title in Roumaji, though this may vary.",
                List.of("Default", "Japanese", "English"),
                (String) this.settings.get("weebLanguagePreference")
        );

        ComboBox<String> comboBox = (ComboBox<String>) ((HBox) languagePreferenceSetting.getChildren().get(1)).getChildren().get(2);
        comboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            this.settings.put("weebLanguagePreference", newValue);
            this.saveButton.setDisable(false);
        });


        // Anime rating filters
        VBox animeRatingFilterSetting = makeMultiInputComboboxSetting(
                "Anime: Age Ratings",
                "Only anime with the chosen age ratings will be displayed. " +
                        "\ntsundoku uses the MyAnimeList anime age rating system as they are the original data source." +
                        "\nThis setting activates exclusively on the Browse view.",
                "animeRatingFilters",
                Arrays.asList("G", "PG", "PG13", "R17+", "R+", "Rx", "Not yet provided")
        );


        // Anime type filters
        VBox animeTypeFilterSetting = makeMultiInputComboboxSetting(
                "Anime: Types",
                "Only anime of the chosen types will be displayed." +
                        "\ntsundoku uses the MyAnimeList anime type system as they are the original data source." +
                        "\nThis setting activates exclusively on the Browse view.",
                "animeTypeFilters",
                Arrays.asList("TV", "Movie", "OVA", "ONA", "Special", "TV Special", "PV", "CM", "Music", "Not yet provided")
        );




        ScrollPane scrollPane = new ScrollPane(wrapper);
        scrollPane.getStyleClass().add("grid-scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        this.smoothScroll = new SmoothScroll(scrollPane, wrapper);


        wrapper.getChildren().addAll(languagePreferenceSetting, animeRatingFilterSetting, animeTypeFilterSetting);

        return scrollPane;
    }


    /**
     * Creates the first type of setting component that we need, a combobox only allowing single items to be chosen.
     * @param headerText Name of the setting
     * @param descriptionText Description text for the setting
     * @param comboBoxItems The choices for this setting
     * @param defaultSelectedItem Value to initialize this setting with (provided by config)
     * @return The finished component
     */
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
        subWrapper.setAlignment(Pos.TOP_RIGHT);

        // Settings item content, left: Description
        Label descriptionLabel = new Label(descriptionText);
        descriptionLabel.getStyleClass().add("settings-description-text");
        HBox.setHgrow(descriptionLabel, Priority.ALWAYS);

        // Settings item content, right: Settings input
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getStyleClass().add("settings-combo-box");
        comboBox.getItems().addAll(comboBoxItems);
        comboBox.setValue(defaultSelectedItem);


        // Spacer to push ComboBox to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);


        subWrapper.getChildren().addAll(descriptionLabel, spacer, comboBox);


        wrapper.getChildren().addAll(headerLabel, subWrapper);

        this.firstSettingsItemCreated = true;
        return wrapper;
    }


    /**
     * Creates the second type of setting component that we need, a combobox allowing multi-inputs.
     *
     * <p>JavaFX somehow does not have this, and the custom version by ControlsFX is horribly buggy.
     * Hence, we're making a custom version here as a "popup" of sorts (looks the same as normal comboboxes).</p>
     * @param headerText Name of the setting
     * @param descriptionText Description text for the setting
     * @param settingsMapName The choices for this setting and their current true / false values
     * @param desiredOrder Order of the choices for this setting (because the raw Map from the config file has jumbled order)
     * @return The finished component
     */
    private VBox makeMultiInputComboboxSetting(String headerText, String descriptionText, String settingsMapName, List<String> desiredOrder) {

        VBox wrapper = new VBox(5);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setStyle("-fx-padding: 0 0 10 0;");

        // Separator between settings boxes
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
        subWrapper.setAlignment(Pos.TOP_RIGHT);

        // Settings item content, left: Description
        Label descriptionLabel = new Label(descriptionText);
        descriptionLabel.getStyleClass().add("settings-description-text");
        HBox.setHgrow(descriptionLabel, Priority.ALWAYS);

        Map<String, Boolean> settingsMap = (Map<String, Boolean>) this.settings.get(settingsMapName);

        // The actual setting. "Fake combo box".
        Label selectionLabel = new Label();
        selectionLabel.getStyleClass().add("settings-multi-combo-box");

        // Display the currently selected items from the fake popup (in provided order)
        Runnable updateLabel = () -> {
            String selectedItems = desiredOrder.stream()
                    .filter(key -> settingsMap.getOrDefault(key, false))
                    .collect(Collectors.joining(", "));
            selectionLabel.setText(selectedItems.isEmpty() ? "None" : selectedItems);
        };
        updateLabel.run();


        // Listener for changes in the settings map
        settingsMap.forEach((key, value) -> {
            BooleanProperty prop = new SimpleBooleanProperty(value);
            prop.addListener((obs, wasSelected, isSelected) -> {
                settingsMap.put(key, isSelected);
                updateLabel.run();
            });
        });

        // The fake "combo box dropdown", a popup. Hides / unhides as expected from a combo box.
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        popup.getContent().add(createPopupContent(settingsMap, desiredOrder, updateLabel));

        selectionLabel.setOnMouseClicked(event -> {
            if (popup.isShowing()) {
                popup.hide();
            } else {
                Bounds bounds = selectionLabel.localToScreen(selectionLabel.getBoundsInLocal());
                double popupX = bounds.getMinX();
                double popupY = bounds.getMinY() + 38; // 38 pixels is label size + offset of 3, same as normal combo box

                popup.show(selectionLabel, popupX, popupY);
            }
        });

        // Spacer to push ComboBox to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        subWrapper.getChildren().addAll(descriptionLabel, spacer, selectionLabel);
        wrapper.getChildren().addAll(headerLabel, subWrapper);

        this.firstSettingsItemCreated = true;
        return wrapper;
    }


    /**
     * Creates the fake dropdown created by makeMultiInputComboboxSetting.
     * @param settingsMap The choices for this setting and their current true / false values
     * @param desiredOrder Order of the choices for this setting (because the raw Map from the config file has jumbled order)
     * @param updateLabel A Runnable that changes the label of the dropdown invoker based on the settings currently set to "true"
     * @return The finished dropdown popup
     */
    private VBox createPopupContent(Map<String, Boolean> settingsMap, List<String> desiredOrder, Runnable updateLabel) {
        VBox popupContent = new VBox();
        popupContent.getStyleClass().add("settings-multi-combo-box-popup");

        for (String key : desiredOrder) {
            Boolean value = settingsMap.getOrDefault(key, false);

            HBox itemBox = new HBox(10);
            itemBox.setMaxWidth(Double.MAX_VALUE);
            itemBox.getStyleClass().add("settings-multi-combo-box-popup-cell");

            CheckBox checkBox = new CheckBox();
            checkBox.getStyleClass().add("settings-multi-combo-box-popup-cell-checkbox");
            checkBox.setSelected(value);
            checkBox.setMouseTransparent(true);
            checkBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                settingsMap.put(key, isSelected);
                updateLabel.run();
                this.saveButton.setDisable(false);
            });

            itemBox.setOnMouseClicked(event -> {
                checkBox.setSelected(!checkBox.isSelected());
            });

            Label nameLabel = new Label(key);
            nameLabel.getStyleClass().add("settings-multi-combo-box-popup-cell-text");
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            itemBox.getChildren().addAll(checkBox, nameLabel);
            popupContent.getChildren().add(itemBox);
        }

        return popupContent;
    }


    /**
     * Creates the third type of setting component that we need, a password-like text input.
     * Not used yet.
     * @return The finished component.
     */
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
