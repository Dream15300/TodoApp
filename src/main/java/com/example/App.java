package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import com.example.ui.ThemeManager;

public class App extends Application {

    @Override
    public void init() {
        DatabaseInitializer.init();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("primary.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);

        scene.getStylesheets().add(
                App.class.getResource("/com/example/style.css").toExternalForm());

        ThemeManager.applySaved(scene);

        stage.setTitle("To Do");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
