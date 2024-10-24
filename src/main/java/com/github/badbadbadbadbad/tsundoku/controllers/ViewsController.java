package com.github.badbadbadbadbad.tsundoku.controllers;

import com.github.badbadbadbadbad.tsundoku.views.AnimeGridView;
import com.github.badbadbadbadbad.tsundoku.views.SidebarView;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class ViewsController implements SidebarListener {
    private final APIController apiController;
    private final ConfigController configController;
    public Region loadingBar;
    private final String contentType = "ANIME"; // Change later when sidebar listening is added
    private final String browseType = "BROWSE"; // Change later when sidebar listening and log are added

    public ViewsController(Stage stage, APIController apiController, ConfigController configController) {
        this.apiController = apiController;
        this.configController = configController;

        HBox root = new HBox();
        root.setId("main-root");

        Screen screen = Screen.getPrimary();
        double screenWidth = screen.getBounds().getWidth();
        double screenHeight = screen.getBounds().getHeight();

        SidebarView sidebarView = new SidebarView();
        Region sidebar = sidebarView.createSidebar();

        Region loadingSeparator = createLoadingSeparator();

        root.getChildren().addAll(sidebar, loadingSeparator);
        updateMainContent(root, stage);

        // AnimeGridView animeGridView = new AnimeGridView(apiController, loadingBar);
        // Region gridView = animeGridView.createGridView(stage);
        // root.getChildren().addAll(sidebar, loadingSeparator, gridView);

        // TODO REMOVE LATER WHEN VIEWS CONTROLLER IS IMPLEMENTED
        // configController.listenToAnimeGrid(animeGridView);
        // apiController.listenToAnimeGrid(animeGridView);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/CSS/styles.css").toExternalForm());
        scene.setFill(Color.rgb(35, 36, 42)); // To prevent white flicker on expanding resize

        stage.setWidth(screenWidth / 1.5);
        stage.setHeight(screenHeight / 1.5);
        stage.setMinWidth(screenWidth / 2);
        stage.setMinHeight(screenHeight / 1.7);

        stage.setScene(scene);
    }


    private Region createLoadingSeparator() {

        // Empty loading bar as separator between sidebar and content
        Region background = new Region();
        background.setMinWidth(2);
        background.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));

        // Loading bar overlaid on empty loading bar
        loadingBar = new Region();
        loadingBar.setMinWidth(2);
        loadingBar.setMinHeight(0);
        loadingBar.setMaxHeight(0);
        loadingBar.setBackground(new Background(new BackgroundFill(Color.GOLDENROD, CornerRadii.EMPTY, Insets.EMPTY)));

        StackPane separator = new StackPane(background, loadingBar);
        separator.setMinWidth(2);
        separator.setMaxWidth(2);
        separator.setPrefWidth(2);

        return separator;
    }


    private void updateMainContent(HBox root, Stage stage) {
        Region gridView = null;

        if (contentType.equals("ANIME")) {
            AnimeGridView animeGridView = new AnimeGridView(apiController, loadingBar);
            gridView = animeGridView.createGridView(stage);
            configController.listenToAnimeGrid(animeGridView);
        } else if (contentType.equals("MANGA")) {
            System.out.println("Manga clicked");
        } else if (contentType.equals("GAMES")) {
            System.out.println("Games clicked");
        }

        if (root.getChildren().size() > 2) { // Content pane exists, remove it and add new one
            root.getChildren().remove(2);
        }
        root.getChildren().add(gridView);
    }
}
