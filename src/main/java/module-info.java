module com.example {
    requires javafx.controls;
    requires javafx.fxml;

    requires javafx.graphics;
    requires javafx.base;

    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.example.ui to javafx.fxml;

    exports com.example;
    exports com.example.ui;
}
