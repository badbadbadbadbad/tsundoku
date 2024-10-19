module com.github.badbadbadbadbad.tsundoku {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires com.fasterxml.jackson.databind;
    requires java.sql;
    requires org.kordamp.ikonli.fluentui;
    requires org.kordamp.ikonli.dashicons;
    requires java.desktop;
    requires java.net.http;

    opens com.github.badbadbadbadbad.tsundoku to javafx.fxml;
    exports com.github.badbadbadbadbad.tsundoku;
    exports com.github.badbadbadbadbad.tsundoku.views;
    opens com.github.badbadbadbadbad.tsundoku.views to javafx.fxml;
    exports com.github.badbadbadbadbad.tsundoku.models;
    opens com.github.badbadbadbadbad.tsundoku.models to javafx.fxml;
}