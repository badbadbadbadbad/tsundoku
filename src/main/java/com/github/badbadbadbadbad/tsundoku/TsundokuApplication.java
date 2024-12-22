package com.github.badbadbadbadbad.tsundoku;

import com.github.badbadbadbadbad.tsundoku.controllers.APIController;
import com.github.badbadbadbadbad.tsundoku.controllers.ConfigController;
import com.github.badbadbadbadbad.tsundoku.controllers.DatabaseController;
import com.github.badbadbadbadbad.tsundoku.controllers.ViewsController;
import com.github.badbadbadbadbad.tsundoku.models.AnimeAPIModel;
import com.github.badbadbadbadbad.tsundoku.models.ConfigModel;
import com.github.badbadbadbadbad.tsundoku.models.DatabaseModel;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class TsundokuApplication extends Application {

    private ViewsController viewsController;

    /**
     * JavaFX main application entrypoint.
     *
     * @param stage JavaFX default. The stage of the application.
     * @throws IOException JavaFX default.
     */
    @Override
    public void start(Stage stage) throws IOException {

        // Make sure data folder for settings and database exist
        Initializer.init();


        AnimeAPIModel animeAPImodel = new AnimeAPIModel();
        ConfigModel configModel = new ConfigModel();
        APIController apiController = new APIController(animeAPImodel, configModel);
        ConfigController configController = new ConfigController(configModel);
        DatabaseModel databaseModel = new DatabaseModel(apiController);
        DatabaseController databaseController = new DatabaseController(databaseModel);
        this.viewsController = new ViewsController(stage, apiController, configController, configModel, databaseController);

        // Window top left icon(s)
        stage.getIcons().add(
                new Image(getClass().getResourceAsStream("/assets/linux/32_crop.png"))
        );

        // Window top bar name
        stage.setTitle("tsundoku.");


        stage.show();
    }

    /**
     * Shuts down active threads in use for background loading on program close.
     */
    @Override
    public void stop() {
        viewsController.shutdownLazyLoader();
    }

    /**
     * JavaFX default. Main entry point (in case default entry point does not work).
     * @param args JavaFX default
     */
    public static void main(String[] args) {
        launch();
    }
}