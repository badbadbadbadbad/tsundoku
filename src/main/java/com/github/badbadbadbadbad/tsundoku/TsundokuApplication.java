package com.github.badbadbadbadbad.tsundoku;

import com.fasterxml.jackson.databind.JsonNode;
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


        AnimeAPIModel model = new AnimeAPIModel();
        List<AnimeInfo> currentSeasonData = model.getCurrentSeason();

        MainWindowView mainWindowView = new MainWindowView(stage, currentSeasonData);
        stage.setTitle("tsundoku.");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}