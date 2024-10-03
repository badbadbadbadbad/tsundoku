package com.github.badbadbadbadbad.tsundoku.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.badbadbadbadbad.tsundoku.util.FlowGridPane;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class AnimeGridView {

    private static boolean filtersHidden = false;
    private List<Anime> animeList = new ArrayList<>();
    private final List<Button> browseModeButtons = new ArrayList<>();

    public AnimeGridView(JsonNode animeData) {
        this.animeList = parseAnimeData(animeData);
    }

    public Region createGridView(Stage stage) {
        VBox root = new VBox();
        VBox.setVgrow(root, Priority.ALWAYS);
        HBox.setHgrow(root, Priority.ALWAYS);


        FlowGridPane filters = createFilters(stage);
        HBox buttonBox = createButtons();
        HBox searchAndFilterToggleBox = createSearchAndFilterToggle(filters);
        ScrollPane animeGrid = createAnimeGrid(stage);


        VBox controls = new VBox();
        controls.getStyleClass().add("content-pane-controls");
        controls.setMinHeight(Control.USE_PREF_SIZE);


        controls.getChildren().addAll(searchAndFilterToggleBox, filters, buttonBox);
        root.getChildren().addAll(controls, animeGrid);
        return root;
    }


    private HBox createSearchAndFilterToggle(FlowGridPane filters) {
        HBox searchAndModeBox = new HBox();
        searchAndModeBox.setSpacing(10);

        // Search bar
        TextField searchBar = new TextField();
        searchBar.setId("search-bar");
        searchBar.setPromptText("Enter query..");
        HBox.setHgrow(searchBar, Priority.ALWAYS);

        // Filter toggle button
        ToggleButton toggleFiltersButton = new ToggleButton("Hide filters");
        toggleFiltersButton.getStyleClass().add("controls-button");

        // Filter toggle logic
        toggleFiltersButton.setOnAction(e -> {
            toggleFiltersButton.setDisable(true);

            if (toggleFiltersButton.isSelected()) {
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


    private FlowGridPane createFilters(Stage stage) {
        FlowGridPane filtersGrid = new FlowGridPane(2, 3);
        filtersGrid.setHgap(10);
        filtersGrid.setVgap(10);
        filtersGrid.setMaxWidth(Double.MAX_VALUE);
        filtersGrid.setMinHeight(0); // Necessary for fade animation

        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();

        // Filters
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


        // Dynamically adjust column amount based on window size
        int filtersAmount = filtersGrid.getChildren().size();
        stage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double windowWidth = newWidth.doubleValue();
            int cols, rows;

            if (windowWidth < screenWidth * 0.625) {
                cols = 2;  // Minimum 2 columns
            } else if (windowWidth < screenWidth * 0.75) {
                cols = 3;
            } else if (windowWidth < screenWidth * 0.875) {
                cols = 4;
            } else {
                cols = 5;  // Maximum 5 columns
            }

            rows = (int) Math.ceil((double) filtersAmount / cols); // Need an int value, but need float division, hence ugly casting..

            filtersGrid.setColsCount(cols);
            filtersGrid.setRowsCount(rows);

            // Necessary for the fade animation to work
            if (!filtersHidden) {
                filtersGrid.setMaxHeight(filtersGrid.prefHeight(filtersGrid.getWidth()));
            }
        });

        return filtersGrid;
    }


    private VBox createDropdownFilter(String labelText, String[] options, String defaultValue) {
        Label label = new Label(labelText);
        label.getStyleClass().add("filter-label");

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(options);
        comboBox.setValue(defaultValue);
        comboBox.getStyleClass().add("filter-box");
        comboBox.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(comboBox, Priority.ALWAYS);

        return new VBox(5, label, comboBox);
    }


    private VBox createNumberFilter(String labelText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("filter-label");

        TextField textField = new TextField("");
        textField.getStyleClass().add("filter-box");
        textField.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(textField, Priority.ALWAYS);

        // Numeric input regex filter
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textField.setText(newValue.replaceAll("\\D", ""));
            }
        });

        return new VBox(5, label, textField);
    }


    private void hideFilters(FlowGridPane filters, ToggleButton button) {

        filtersHidden = true;

        // Fade animation
        FadeTransition fade = new FadeTransition(Duration.millis(150), filters);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        // Move animation
        KeyFrame visible = new KeyFrame(Duration.ZERO, new KeyValue(filters.maxHeightProperty(), filters.getHeight()));
        KeyFrame hidden = new KeyFrame(Duration.millis(100), new KeyValue(filters.maxHeightProperty(), 0));
        Timeline move = new Timeline(visible, hidden);

        // Cooldown
        PauseTransition cooldown = new PauseTransition(Duration.millis(200));
        cooldown.setOnFinished(event -> button.setDisable(false));

        // Gather animations
        SequentialTransition st = new SequentialTransition(fade, move);
        ParallelTransition pt = new ParallelTransition(cooldown, st);

        pt.play();
    }


    private void showFilters(FlowGridPane filters, ToggleButton button) {

        filtersHidden = false;

        // Fade animation
        FadeTransition fade = new FadeTransition(Duration.millis(150), filters);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        // Move animation
        KeyFrame hidden = new KeyFrame(Duration.ZERO, new KeyValue(filters.maxHeightProperty(), 0));
        KeyFrame visible = new KeyFrame(Duration.millis(100), new KeyValue(filters.maxHeightProperty(), filters.prefHeight(filters.getWidth())));
        Timeline move = new Timeline(hidden, visible);

        // Cooldown
        PauseTransition cooldown = new PauseTransition(Duration.millis(200));
        cooldown.setOnFinished(event -> button.setDisable(false));

        // Gather animations
        SequentialTransition st = new SequentialTransition(move, fade);
        ParallelTransition pt = new ParallelTransition(cooldown, st);

        pt.play();
    }


    private HBox createButtons() {

        // Mode selector buttons
        Button browseButton = createModeButton("Browse");
        Button logButton = createModeButton("Log");

        browseButton.setId("browse-mode-browse-button");
        logButton.setId("browse-mode-log-button");


        Collections.addAll(browseModeButtons, browseButton, logButton);

        // Needed to prevent button jumping upon toggling to active.
        // browseButton.setFocusTraversable(false);
        // logButton.setFocusTraversable(false);

        // Change later
        // browseButton.setSelected(true);

        // Treat them as one component
        HBox leftButtonBox = new HBox(browseButton, logButton);
        leftButtonBox.setAlignment(Pos.CENTER_LEFT);

        // Right buttons
        Button seasonButton = new Button("Season");
        Button topButton = new Button("Top");
        Button searchButton = new Button("Search");

        seasonButton.getStyleClass().add("controls-button");
        topButton.getStyleClass().add("controls-button");
        searchButton.getStyleClass().add("controls-button");

        HBox rightButtonBox = new HBox(10, seasonButton, topButton, searchButton);
        rightButtonBox.setAlignment(Pos.CENTER_RIGHT);

        // Fill middle region with empty space
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);

        HBox buttonBox = new HBox(leftButtonBox, space, rightButtonBox);
        HBox.setHgrow(buttonBox, Priority.ALWAYS);

        return buttonBox;
    }


    private Button createModeButton(String label) {
        Button button = new Button(label);
        // button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().addAll("controls-button", "browse-mode-button");

        button.setOnAction(e -> {
            setActiveButton(button);
        });

        return button;
    }


    private void setActiveButton(Button selectedButton) {
        for (Button button : browseModeButtons) {
            button.getStyleClass().removeAll("browse-mode-button-active");
        }
        selectedButton.getStyleClass().add("browse-mode-button-active");
    }


    private ScrollPane createAnimeGrid(Stage stage) {
        FlowGridPane animeGrid = new FlowGridPane(2, 3);
        animeGrid.setHgap(20);
        animeGrid.setVgap(20);
        animeGrid.setMaxWidth(Double.MAX_VALUE);

        for (Anime anime : animeList) {
            VBox animeBox = createAnimeBox(anime);
            animeGrid.getChildren().add(animeBox);
        }

        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();

        int animesAmount = animeGrid.getChildren().size();
        stage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double windowWidth = newWidth.doubleValue();

            int cols, rows;

            if (windowWidth < screenWidth * 0.6) {
                cols = 3;  // Minimum 2 columns
            } else if (windowWidth < screenWidth * 0.7) {
                cols = 4;
            } else if (windowWidth < screenWidth * 0.8) {
                cols = 5;
            } else if (windowWidth < screenWidth * 0.9) {
                cols = 6;
            } else {
                cols = 7;
            }

            rows = (int) Math.ceil((double) animesAmount / cols);

            animeGrid.setColsCount(cols);
            animeGrid.setRowsCount(rows);
        });

        ScrollPane scrollPane = new ScrollPane(animeGrid);
        scrollPane.getStyleClass().add("grid-scroll-pane");
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.NEVER);

        return scrollPane;
    }


    private List<Anime> parseAnimeData(JsonNode animeData) {
        List<Anime> animeList = new ArrayList<>();
        JsonNode dataArray = animeData.get("data");

        if (dataArray.isArray()) {
            for (JsonNode animeNode : dataArray) {
                int id = animeNode.get("mal_id").asInt();
                String title = animeNode.get("title").asText();
                String imageUrl = animeNode.get("images").get("jpg").get("large_image_url").asText();
                // String imageUrl = animeNode.get("images").get("jpg").get("image_url").asText();

                Anime anime = new Anime(id, title, imageUrl);
                animeList.add(anime);
            }
        }

        return animeList;
    }


    private VBox createAnimeBox(Anime anime) {
        // Anime covers on MAL have _slightly_ different image sizes.
        // This seems to be the most common? We force all images to be this size
        double RATIO = 318.0 / 225.0;

        // Make image into VBox background, CSS cover sizing to look okay
        VBox animeBox = new VBox();
        animeBox.setAlignment(Pos.CENTER);
        animeBox.setStyle("-fx-background-image: url('" + anime.getImageUrl() + "');");
        animeBox.getStyleClass().add("grid-media-box");


        // TEST THIS NEXT TIME TO ACHIEVE ROUNDED BORDERS?
        /*
        // new Image(url)
        Image image = new Image(CurrentClass.class.getResource("/path/to/package/bg.jpg"));
        // new BackgroundSize(width, height, widthAsPercentage, heightAsPercentage, contain, cover)
        BackgroundSize backgroundSize = new BackgroundSize(100, 100, true, true, true, false);
        // new BackgroundImage(image, repeatX, repeatY, position, size)
        BackgroundImage backgroundImage = new BackgroundImage(image, BackgroundRepeat.REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, backgroundSize);
        // new Background(images...)
        Background background = new Background(backgroundImage);

         */


        animeBox.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            // Platform.runLater needed to trigger layout update post-resizing
            // Has a chance to get a bit wonky on window snaps otherwise
            Platform.runLater(() -> {
                double newHeight = newWidth.doubleValue() * RATIO;
                animeBox.setMinHeight(newHeight);
                animeBox.setPrefHeight(newHeight);
                animeBox.setMaxHeight(newHeight);
            });
        });

        return animeBox;
    }
}
