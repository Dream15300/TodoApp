package com.example;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class PrimaryController {

    public void openSecondary(ActionEvent e) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("secondary.fxml"));
        Scene scene = new Scene(loader.load(), 700, 400);

        Stage stage = new Stage();
        stage.setTitle("Secondary");
        stage.setScene(scene);
        stage.show();
    }
}
