package com.github.badbadbadbadbad.tsundoku;

import com.github.badbadbadbadbad.tsundoku.controllers.APIController;
import com.github.badbadbadbadbad.tsundoku.controllers.ConfigController;
import com.github.badbadbadbadbad.tsundoku.controllers.DatabaseController;
import com.github.badbadbadbadbad.tsundoku.controllers.ViewsController;
import com.github.badbadbadbadbad.tsundoku.models.AnimeAPIModel;
import com.github.badbadbadbadbad.tsundoku.models.ConfigModel;
import com.github.badbadbadbadbad.tsundoku.models.DatabaseModel;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class TsundokuApplication extends Application {


    @Override
    public void start(Stage stage) throws IOException {

        // Make sure data folder for settings and database exist
        Initializer.init();


        AnimeAPIModel animeAPImodel = new AnimeAPIModel();
        ConfigModel configModel = new ConfigModel();
        APIController apiController = new APIController(animeAPImodel, configModel);
        ConfigController configController = new ConfigController(configModel);
        DatabaseModel databaseModel = new DatabaseModel();
        DatabaseController databaseController = new DatabaseController(databaseModel);
        ViewsController viewsController = new ViewsController(stage, apiController, configController, configModel, databaseController);

        stage.setTitle("tsundoku.");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}