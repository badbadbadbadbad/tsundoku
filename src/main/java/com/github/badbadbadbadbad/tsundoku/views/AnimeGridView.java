package com.github.badbadbadbadbad.tsundoku.views;

import com.github.badbadbadbadbad.tsundoku.util.FlowGridPane;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class AnimeGridView {

    private static final int VGAP = 10;

    public Region createGridView(Stage stage) {

        VBox root = new VBox();
        root.setPadding(new Insets(20));
        root.setSpacing(15);
        root.setStyle("-fx-background-color: #1e1e1e;");

        HBox searchAndModeBox = createSearchAndModeSelector();
        FlowGridPane filters = createFilters(stage);
        HBox buttonBox = createButtons();

        root.getChildren().addAll(searchAndModeBox, filters, buttonBox);

        return root;
    }


    private HBox createSearchAndModeSelector() {
        HBox searchAndModeBox = new HBox();
        searchAndModeBox.setSpacing(10);

        // Search bar
        TextField searchBar = new TextField();
        searchBar.setPromptText("Enter query..");
        searchBar.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.5);");
        searchBar.setPrefHeight(35);
        HBox.setHgrow(searchBar, Priority.ALWAYS);

        // Mode selector
        ToggleButton browseButton = new ToggleButton("Browse");
        ToggleButton logButton = new ToggleButton("Log");

        // Style the mode selector buttons for dark mode
        browseButton.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        logButton.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        browseButton.setPrefHeight(35);
        logButton.setPrefHeight(35);

        // Create ToggleGroup to ensure only one can be selected at a time
        ToggleGroup modeGroup = new ToggleGroup();
        browseButton.setToggleGroup(modeGroup);
        logButton.setToggleGroup(modeGroup);

        // Needed to prevent button jumping upon toggling to active.
        browseButton.setFocusTraversable(false);
        logButton.setFocusTraversable(false);

        // Change later
        browseButton.setSelected(true);

        // toggling styles
        modeGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle == browseButton) {
                browseButton.setStyle("-fx-background-color: #666; -fx-text-fill: white;");
                logButton.setStyle("-fx-background-color: #444; -fx-text-fill: white;");
                browseButton.setFocusTraversable(false);
                logButton.setFocusTraversable(false);
            } else if (newToggle == logButton) {
                logButton.setStyle("-fx-background-color: #666; -fx-text-fill: white;");
                browseButton.setStyle("-fx-background-color: #444; -fx-text-fill: white;");
                browseButton.setFocusTraversable(false);
                logButton.setFocusTraversable(false);
            }
        });


        HBox modeSelectorBox = new HBox(browseButton, logButton);
        modeSelectorBox.setSpacing(2);


        searchAndModeBox.getChildren().addAll(searchBar, modeSelectorBox);
        return searchAndModeBox;
    }


    private FlowGridPane createFilters(Stage stage) {
        FlowGridPane filtersGrid = new FlowGridPane(2, 3);
        filtersGrid.setHgap(10);
        filtersGrid.setVgap(10);

        filtersGrid.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(filtersGrid, Priority.ALWAYS);

        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();

        stage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double windowWidth = newWidth.doubleValue();

            int cols, rows;

            // Column amount
            // Row amount needs to scale too for FlowGridPane to not have extra bottom padding.
            // Hardcoded for now
            if (windowWidth < screenWidth * 0.625) {
                cols = 2;  // Minimum 2 columns
                rows = 3;
            } else if (windowWidth < screenWidth * 0.75) {
                cols = 3;
                rows = 2;
            } else if (windowWidth < screenWidth * 0.875) {
                cols = 4;
                rows = 2;
            } else {
                cols = 5;  // Maximum 5 columns
                rows = 1;
            }

            filtersGrid.setColsCount(cols);
            filtersGrid.setRowsCount(rows);
        });



        VBox orderByFilter = createDropdownFilter("Order by", new String[]{
                "Title: Ascending", "Title: Descending",
                "Rating: Highest", "Rating: Lowest",
                "Popular: Most", "Popular: Least"}, "Popular: Most");

        VBox startYearFilter = createNumberFilter("Start year");
        VBox endYearFilter = createNumberFilter("End year");

        VBox sfwFilter = createDropdownFilter("Filter adult entries",
                new String[]{"Yes", "No"}, "Yes");

        VBox statusFilter = createDropdownFilter("Status",
                new String[]{"Any", "Complete", "Airing", "Upcoming"}, "Any");


        filtersGrid.getChildren().addAll(orderByFilter, startYearFilter, endYearFilter, sfwFilter, statusFilter);

        VBox.setVgrow(filtersGrid, Priority.NEVER);
        filtersGrid.setPrefHeight(Region.USE_COMPUTED_SIZE);



        return filtersGrid;
    }


    private VBox createDropdownFilter(String labelText, String[] options, String defaultValue) {
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: white;");

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(options);
        comboBox.setValue(defaultValue);
        comboBox.setPrefHeight(30);

        comboBox.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(comboBox, Priority.ALWAYS);

        VBox filterBox = new VBox(5, label, comboBox);
        filterBox.setPrefWidth(200);

        return filterBox;
    }


    private VBox createNumberFilter(String labelText) {
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: white;");

        TextField textField = new TextField("");
        textField.setPrefHeight(30);

        // Numeric input regex filter
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textField.setText(newValue.replaceAll("\\D", ""));
            }
        });

        textField.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(textField, Priority.ALWAYS);

        VBox filterBox = new VBox(5, label, textField);
        filterBox.setPrefWidth(200);

        return filterBox;
    }


    private HBox createButtons() {
        Button seasonButton = new Button("Season");
        Button topButton = new Button("Top");
        Button searchButton = new Button("Search");

        String buttonStyle = "-fx-background-color: #333; -fx-text-fill: white;";

        seasonButton.setStyle(buttonStyle);
        topButton.setStyle(buttonStyle);
        searchButton.setStyle(buttonStyle);

        seasonButton.setPrefSize(100, 30);
        topButton.setPrefSize(100, 30);
        searchButton.setPrefSize(100, 30);


        HBox buttonBox = new HBox(10, seasonButton, topButton, searchButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);  // Align buttons to the right


        HBox.setHgrow(seasonButton, Priority.ALWAYS);
        HBox.setHgrow(topButton, Priority.ALWAYS);
        HBox.setHgrow(searchButton, Priority.ALWAYS);


        return buttonBox;
    }
}
