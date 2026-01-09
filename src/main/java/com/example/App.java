package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

import com.example.ui.ThemeManager;
import com.example.service.TodoService;
import com.example.ui.TaskbarDueNotifier;

public class App extends Application {

    private TaskbarDueNotifier dueNotifier;

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

        stage.getIcons().add(new Image(App.class.getResource("/com/example/Haken.png").toExternalForm()));

        stage.setTitle("To Do");
        stage.setScene(scene);
        stage.show();

        TodoService service = new TodoService();
        dueNotifier = new TaskbarDueNotifier(service);
        dueNotifier.start();

        stage.setOnCloseRequest(e -> {
            if (dueNotifier != null) {
                dueNotifier.stop();
            }
        });

    }

    public static void main(String[] args) {
        launch(args);
    }
}
