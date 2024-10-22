package com.github.badbadbadbadbad.tsundoku;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.badbadbadbadbad.tsundoku.controllers.APIController;
import com.github.badbadbadbadbad.tsundoku.controllers.ConfigController;
import com.github.badbadbadbadbad.tsundoku.models.AnimeAPIModel;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import com.github.badbadbadbadbad.tsundoku.models.ConfigModel;
import com.github.badbadbadbadbad.tsundoku.views.MainWindowView;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.util.List;

public class TsundokuApplication extends Application {


    @Override
    public void start(Stage stage) throws IOException {

        // Make sure data folder for settings and database exist
        Initializer.init();


        AnimeAPIModel animeAPImodel = new AnimeAPIModel();
        ConfigModel configModel = new ConfigModel();
        APIController apiController = new APIController(animeAPImodel, configModel);
        ConfigController configController = new ConfigController(configModel);
        MainWindowView mainWindowView = new MainWindowView(stage, apiController, configController);
        stage.setTitle("tsundoku.");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}