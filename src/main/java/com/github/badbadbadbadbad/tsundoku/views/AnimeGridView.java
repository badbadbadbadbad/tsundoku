package com.github.badbadbadbadbad.tsundoku.views;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.badbadbadbadbad.tsundoku.util.FlowGridPane;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;


public class AnimeGridView {

    private static final int VGAP = 10;
    private static boolean filtersHidden = false;
    private JsonNode data;
    private List<Anime> animeList;

    public AnimeGridView(JsonNode animeData) {
        this.data = animeData;
        this.animeList = parseAnimeData(animeData);
    }

    public Region createGridView(Stage stage) {

        VBox root = new VBox();
        root.setStyle("-fx-background-color: #1e1e1e;");
        VBox.setVgrow(root, Priority.ALWAYS);
        HBox.setHgrow(root, Priority.ALWAYS);

        // WRAP THE TOP STUFF IN A "CONTROLS" CONTAINER AND PAD TOP, LEFT, RIGHT
        // MAKE THE SCROLLPANE ITS OWN CONTAINER FOR THE FLOWGRID SO THE SCROLLBAR CAN BE AT THE VERY RIGHT
        // NO PADDING FOR SCROLLPANE, PADDING FOR FLOWGRID (THAT IS DIFFERENT THAN CONTROLS PADDING)?
        // CHECK FOLLOWING POST TO MAKE SCROLLPANE NOT OVERFLOW TO CONTROLS:
        // https://stackoverflow.com/questions/75695060/make-scrollpane-use-remaining-space

        FlowGridPane filters = createFilters(stage);
        HBox buttonBox = createButtons();
        HBox searchAndFilterToggleBox = createSearchAndFilterToggle(filters);
        ScrollPane animeGrid = createAnimeGrid(stage);


        VBox controls = new VBox();
        controls.setPadding(new Insets(20));
        controls.setSpacing(15);
        controls.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #FFFFFF;");
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
        searchBar.setPromptText("Enter query..");
        searchBar.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.5);");
        searchBar.setPrefHeight(35);
        HBox.setHgrow(searchBar, Priority.ALWAYS);

        // Filter toggle button
        ToggleButton toggleFiltersButton = new ToggleButton("Hide filters");
        toggleFiltersButton.setStyle("-fx-background-color: #444; -fx-text-fill: white;");

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

            if (!filtersHidden) {
                filtersGrid.setMaxHeight(filtersGrid.prefHeight(filtersGrid.getWidth()));
            }
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
        ToggleButton browseButton = new ToggleButton("Browse");
        ToggleButton logButton = new ToggleButton("Log");

        browseButton.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        logButton.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        browseButton.setPrefHeight(35);
        logButton.setPrefHeight(35);

        // Create ToggleGroup to ensure only one can be selected at a time
        ToggleGroup modeGroup = new ToggleGroup();
        browseButton.setToggleGroup(modeGroup);
        logButton.setToggleGroup(modeGroup);

        // toggling styles
        modeGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle == browseButton) {
                browseButton.setStyle("-fx-background-color: #666; -fx-text-fill: white;");
                logButton.setStyle("-fx-background-color: #444; -fx-text-fill: white;");
            } else if (newToggle == logButton) {
                logButton.setStyle("-fx-background-color: #666; -fx-text-fill: white;");
                browseButton.setStyle("-fx-background-color: #444; -fx-text-fill: white;");
            }
        });

        // Needed to prevent button jumping upon toggling to active.
        browseButton.setFocusTraversable(false);
        logButton.setFocusTraversable(false);

        // Change later
        // browseButton.setSelected(true);

        // Treat them as one component
        HBox leftButtonBox = new HBox(browseButton, logButton);
        leftButtonBox.setAlignment(Pos.CENTER_LEFT);


        // Right buttons
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

        HBox rightButtonBox = new HBox(10, seasonButton, topButton, searchButton);
        rightButtonBox.setAlignment(Pos.CENTER_RIGHT);

        // Fill middle region with empty space
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);


        HBox buttonBox = new HBox(leftButtonBox, space, rightButtonBox);
        HBox.setHgrow(buttonBox, Priority.ALWAYS);

        return buttonBox;
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

        stage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double windowWidth = newWidth.doubleValue();

            int cols, rows;

            // Column amount
            // Row amount needs to scale too for FlowGridPane to not have extra bottom padding.
            // Hardcoded for now
            if (windowWidth < screenWidth * 0.6) {
                cols = 3;  // Minimum 2 columns
                rows = 9;
            } else if (windowWidth < screenWidth * 0.7) {
                cols = 4;
                rows = 7;
            } else if (windowWidth < screenWidth * 0.8) {
                cols = 5;
                rows = 5;
            } else if (windowWidth < screenWidth * 0.9) {
                cols = 6;  // Maximum 5 columns
                rows = 5;
            } else {
                cols = 7;  // Maximum 5 columns
                rows = 4;
            }

            animeGrid.setColsCount(cols);
            animeGrid.setRowsCount(rows);
        });

        ScrollPane scrollPane = new ScrollPane(animeGrid);
        scrollPane.setFitToWidth(true);
        // scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: #1e1e1e; -fx-border-color: #1e1e1e;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.NEVER);

        // return animeGrid;
        return scrollPane;
    }


    private List<Anime> parseAnimeData(JsonNode animeData) {
        List<Anime> animeList = new ArrayList<>();
        JsonNode dataArray = animeData.get("data");

        if (dataArray.isArray()) {
            for (JsonNode animeNode : dataArray) {
                int id = animeNode.get("mal_id").asInt();
                String title = animeNode.get("title").asText();
                // String imageUrl = animeNode.get("images").get("jpg").get("large_image_url").asText();
                String imageUrl = animeNode.get("images").get("jpg").get("image_url").asText();

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
        animeBox.setStyle(
                "-fx-background-image: url('" + anime.getImageUrl() + "');" +
                "-fx-background-size: cover;" +  // Image covers entire box and cuts off what hangs out
                "-fx-background-position: center;" +
                "-fx-background-repeat: no-repeat;"
        );


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
