module com.github.badbadbadbadbad.tsundoku {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;

    opens com.github.badbadbadbadbad.tsundoku to javafx.fxml;
    exports com.github.badbadbadbadbad.tsundoku;
}