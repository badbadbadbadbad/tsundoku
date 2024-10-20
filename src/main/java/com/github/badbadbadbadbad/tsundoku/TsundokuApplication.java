package com.github.badbadbadbadbad.tsundoku;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.badbadbadbadbad.tsundoku.controllers.APIController;
import com.github.badbadbadbadbad.tsundoku.models.AnimeAPIModel;
import com.github.badbadbadbadbad.tsundoku.models.AnimeInfo;
import com.github.badbadbadbadbad.tsundoku.views.MainWindowView;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class TsundokuApplication extends Application {


    @Override
    public void start(Stage stage) throws IOException {

        // Make sure data folder for settings and database exist
        Initializer.init();


        AnimeAPIModel animeAPImodel = new AnimeAPIModel();
        APIController apiController = new APIController(animeAPImodel);

        MainWindowView mainWindowView = new MainWindowView(stage, apiController);
        stage.setTitle("tsundoku.");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}