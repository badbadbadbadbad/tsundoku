package com.github.badbadbadbadbad.tsundoku;

import com.github.badbadbadbadbad.tsundoku.models.AnimeAPIModel;
import com.github.badbadbadbadbad.tsundoku.views.MainWindowView;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class TsundokuApplication extends Application {


    @Override
    public void start(Stage stage) throws IOException {

        // Make sure data folder for settings and database exist
        Initializer.init();

        // Create MODEL then VIEW then CONTROLLER(MODEL, VIEW)
        // VIEWS and MODELS need to know about their parent CONTROLLER
        // CONTROLLERS need to know about the MODELS and VIEWS they control
        // EVENT LISTENERS are all part of VIEW and notify CONTROLLER for improved platform portability

        AnimeAPIModel model = new AnimeAPIModel();
        // model.getCurrentSeason();

        MainWindowView mainWindowView = new MainWindowView(stage);
        stage.setTitle("tsundoku.");
        stage.show();

    }

    public static void main(String[] args) {
        launch();
    }
}