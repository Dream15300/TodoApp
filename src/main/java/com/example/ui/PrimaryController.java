package com.example.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.control.ListView;

import java.io.IOException;

import com.example.App;

public class PrimaryController {

    @FXML
    private ListView<?> listsView;
    @FXML
    private ListView<?> tasksView;
    @FXML
    private TextField txtNewTaskTitle;
    @FXML
    private DatePicker dpNewTaskDueDate;

    public void openSecondary(ActionEvent e) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("secondary.fxml"));
        Scene scene = new Scene(loader.load(), 700, 400);

        Stage stage = new Stage();
        stage.setTitle("Secondary");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void onNewList() {
    }

    @FXML
    private void onEditSelectedList() {
    }

    @FXML
    private void onAddTask() {
    }

    @FXML
    private void onMarkSelectedTaskDone() {
    }
}
