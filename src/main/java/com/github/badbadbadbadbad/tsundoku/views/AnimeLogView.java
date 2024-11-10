package com.github.badbadbadbadbad.tsundoku.views;

import com.github.badbadbadbadbad.tsundoku.controllers.DatabaseRequestListener;
import com.github.badbadbadbadbad.tsundoku.external.FlowGridPane;
import javafx.animation.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AnimeLogView {

    private final double RATIO = 318.0 / 225.0; // The aspect ratio to use for anime images. This doesn't match all exactly, but is close enough.

    private final Stage stage;
    private final DatabaseRequestListener popupListener;
    // private final DatabaseRequestListener databaseRequestListener;

    private ScrollPane scrollPane;
    private FlowGridPane animeGrid;
    private StackPane stackPane;

    private static final BooleanProperty filtersHidden = new SimpleBooleanProperty(true);

    public AnimeLogView(Stage stage, DatabaseRequestListener popupListener) {
        this.stage = stage;
        this.popupListener = popupListener;
    }

    public Region createGridView() {

        VBox root = new VBox();
        VBox.setVgrow(root, Priority.ALWAYS);
        HBox.setHgrow(root, Priority.ALWAYS);


        // StackPane wrapper to allow for popup functionality when grid element is clicked
        stackPane = new StackPane();
        VBox.setVgrow(stackPane, Priority.ALWAYS);
        HBox.setHgrow(stackPane, Priority.ALWAYS);
        stackPane.getChildren().add(root);


        // Controls above the grid
        FlowGridPane filters = createFilters();
        HBox searchAndFilterToggleBox = createSearchAndFilterToggle(filters);

        VBox controls = new VBox();
        controls.getStyleClass().add("content-pane-controls");
        controls.setMinHeight(Control.USE_PREF_SIZE);

        controls.getChildren().addAll(searchAndFilterToggleBox, filters);


        // The actual grids. Make each header + grid with a function call.
        // Then wrap them all in a scrollPane.
        // AnimeListInfo animeListInfo = databaseRequestListener.getCurrentAnimeSeason(1).join();
        // this.scrollPane = createLogGrid(animeListInfo);




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

        /*
        scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> {

            // Platform.runLater else the border starts as shown on scrollPane default position on full grid reload

            Platform.runLater(() -> {
                // This is so the controls-bottom-border can't start showing if the pane scroll bar is fully vertical (no scrolling possible)
                boolean canScroll = scrollPane.getContent().getBoundsInLocal().getHeight() > scrollPane.getViewportBounds().getHeight();

                if (newValue.doubleValue() > 0.01 && canScroll) {
                    if (separator.getOpacity() == 0.0) {
                        fadeIn.playFromStart();
                    }
                } else {
                    if (separator.getOpacity() == 1.0) {
                        fadeOut.playFromStart();
                    }
                }
            });
        });

         */


        // root.getChildren().addAll(controls, separator, scrollPane);
        root.getChildren().addAll(controls, separator);
        return stackPane;
    }


    private HBox createSearchAndFilterToggle(FlowGridPane filters) {
        // Wrapper
        HBox searchAndModeBox = new HBox();
        searchAndModeBox.getStyleClass().add("search-bar-and-filter-toggle");

        // Search bar
        TextField searchBar = new TextField();
        searchBar.setId("search-bar");
        searchBar.setPromptText("Enter anime name..");
        HBox.setHgrow(searchBar, Priority.ALWAYS);

        // Regex to allow only english alphanumeric, JP / CN / KR characters, and spaces
        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.matches("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{Alnum} ]*")) {
                // searchString = newValue;
            } else {
                searchBar.setText(oldValue);
            }
        });


        // Filter toggle button
        ToggleButton toggleFiltersButton = new ToggleButton("Show filters");
        toggleFiltersButton.getStyleClass().add("controls-button");

        // Filter toggle logic
        toggleFiltersButton.setOnAction(e -> {
            toggleFiltersButton.setDisable(true);

            if (!toggleFiltersButton.isSelected()) {
                toggleFiltersButton.setText("Show filters");
                hideFilters(filters, toggleFiltersButton);
            } else {
                toggleFiltersButton.setText("Hide filters");
                showFilters(filters, toggleFiltersButton);
            }
        });

        searchAndModeBox.getChildren().addAll(searchBar, toggleFiltersButton);
        return searchAndModeBox;
    }


    private FlowGridPane createFilters() {
        FlowGridPane filtersGrid = new FlowGridPane(2, 3);
        filtersGrid.setHgap(10);
        filtersGrid.setVgap(10);
        filtersGrid.setMaxWidth(Double.MAX_VALUE);
        filtersGrid.setMinHeight(0); // Necessary for fade animation

        // We initialize with filters hidden
        filtersGrid.setMaxHeight(0);
        filtersGrid.setOpacity(0.0);


        // Filters
        VBox personalStatusFilter = createDropdownFilter("Personal status", new String[]{
                "Any",
                "In progress", "Backlog",
                "Completed", "Paused",
                "Dropped"}, "Any");

        VBox statusFilter = createDropdownFilter("Status",
                new String[]{"Any", "Complete", "Airing", "Upcoming"}, "Any");

        VBox startYearFilter = createNumberFilter("Start year ≥");
        VBox endYearFilter = createNumberFilter("End year ≤");


        filtersGrid.getChildren().addAll(personalStatusFilter, statusFilter, startYearFilter, endYearFilter);


        if (!filtersHidden.get()) {
            filtersGrid.setMaxHeight(filtersGrid.prefHeight(filtersGrid.getWidth()));
        }


        return filtersGrid;
    }


    private VBox createDropdownFilter(String labelText, String[] options, String defaultValue) {
        Label label = new Label(labelText);
        label.getStyleClass().add("filter-label");

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(options);
        comboBox.setValue(defaultValue);
        comboBox.getStyleClass().add("filter-combo-box");
        comboBox.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(comboBox, Priority.ALWAYS);

        // Filter change listeners
        if (labelText.equals("Personal status")) {
            comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {

            });
        } else if (labelText.equals("Status")) {
            comboBox.valueProperty().addListener((obs, oldVal, newVal) -> {

            });
        }

        return new VBox(5, label, comboBox);
    }


    private VBox createNumberFilter(String labelText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("filter-label");

        TextField textField = new TextField("");
        textField.getStyleClass().add("filter-text-box");
        textField.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(textField, Priority.ALWAYS);

        // Numeric input and at most four digits regex filter
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d{0,4}")) {
                textField.setText(oldValue);
            }
        });

        // Filter change listeners
        if (labelText.equals("Start year ≥")) {
            textField.textProperty().addListener((obs, oldVal, newVal) -> {

            });
        } else if (labelText.equals("End year ≤")) {
            textField.textProperty().addListener((obs, oldVal, newVal) -> {

            });
        }

        // Even when filters are hidden, mouse cursor changes to text field cursor when hovering
        // where the number filters would be.
        filtersHidden.addListener((obs, oldValue, newValue) -> {
            textField.setDisable(newValue);
        });
        textField.setDisable(filtersHidden.get()); // Since filters are hidden on startup


        // Listener to fire searches on enter press.
        // Feels more natural when it fires not only when in the search bar, but also these year fields.
        return new VBox(5, label, textField);
    }


    private void hideFilters(FlowGridPane filters, ToggleButton button) {

        filtersHidden.set(true);

        // Fade animation
        FadeTransition fade = new FadeTransition(Duration.millis(150), filters);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        // Move animation
        KeyFrame visible = new KeyFrame(Duration.ZERO, new KeyValue(filters.maxHeightProperty(), filters.getHeight()));
        KeyFrame hidden = new KeyFrame(Duration.millis(100), new KeyValue(filters.maxHeightProperty(), 0));
        Timeline move = new Timeline(visible, hidden);

        // Filters bottom padding
        KeyFrame paddingVisible = new KeyFrame(Duration.ZERO, new KeyValue(filters.paddingProperty(), new Insets(0, 0, 15, 0)));
        KeyFrame paddingHidden = new KeyFrame(Duration.millis(100), new KeyValue(filters.paddingProperty(), new Insets(0, 0, 0, 0)));
        Timeline movePadding = new Timeline(paddingVisible, paddingHidden);

        // Cooldown
        PauseTransition cooldown = new PauseTransition(Duration.millis(200));
        cooldown.setOnFinished(event -> button.setDisable(false));

        // Gather animations
        ParallelTransition test = new ParallelTransition(move, movePadding);
        SequentialTransition st = new SequentialTransition(fade, test);
        ParallelTransition pt = new ParallelTransition(cooldown, st);


        // TODO Is this right here?
        pt.setOnFinished(e -> {
            // updateVisibleGridItems(scrollPane);
        });

        pt.play();
    }


    private void showFilters(FlowGridPane filters, ToggleButton button) {

        filtersHidden.set(false);

        // Fade animation
        FadeTransition fade = new FadeTransition(Duration.millis(150), filters);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        // Move animation
        KeyFrame hidden = new KeyFrame(Duration.ZERO, new KeyValue(filters.maxHeightProperty(), 0));
        KeyFrame visible = new KeyFrame(Duration.millis(100), new KeyValue(filters.maxHeightProperty(), filters.prefHeight(filters.getWidth())));
        Timeline move = new Timeline(hidden, visible);

        // Filters bottom padding
        KeyFrame paddingHidden = new KeyFrame(Duration.ZERO, new KeyValue(filters.paddingProperty(), new Insets(0, 0, 0, 0)));
        KeyFrame paddingVisible = new KeyFrame(Duration.millis(100), new KeyValue(filters.paddingProperty(), new Insets(0, 0, 15, 0)));
        Timeline movePadding = new Timeline(paddingHidden, paddingVisible);

        // Cooldown
        PauseTransition cooldown = new PauseTransition(Duration.millis(200));
        cooldown.setOnFinished(event -> button.setDisable(false));

        // Gather animations
        ParallelTransition test = new ParallelTransition(move, movePadding);
        SequentialTransition st = new SequentialTransition(test, fade);
        ParallelTransition pt = new ParallelTransition(cooldown, st);

        // TODO Is this right here?
        pt.setOnFinished(e -> {
            // updateVisibleGridItems(scrollPane);
        });

        // Filters are a bit squished after clicking "show filters" button until a resize if this is not done
        test.setOnFinished(e -> {
            filters.setMaxHeight(filters.prefHeight(filters.getWidth()));
        });

        pt.play();
    }

}
