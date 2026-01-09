module com.example {
    requires transitive javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;

    requires javafx.base;

    requires java.desktop;

    requires java.sql;
    requires java.prefs;
    requires org.xerial.sqlitejdbc;

    opens com.example to javafx.fxml;
    opens com.example.ui to javafx.fxml;

    exports com.example;
    exports com.example.ui;
    exports com.example.service;
    exports com.example.domain;

}
