package com.example.ui;

import com.example.App;
import com.example.domain.Category;
import com.example.domain.TodoItem;
import com.example.service.TodoService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class PrimaryController {

    @FXML
    private ListView<Category> listsView;
    @FXML
    private ListView<TodoItem> tasksView;
    @FXML
    private TextField txtNewTaskTitle;
    @FXML
    private DatePicker dpNewTaskDueDate;

    private final TodoService service = new TodoService();

    public void openSecondary(ActionEvent e) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("secondary.fxml"));
        Scene scene = new Scene(loader.load(), 700, 400);

        Stage stage = new Stage();
        stage.setTitle("Secondary");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void initialize() {
        loadCategories();

        listsView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                loadTodos(newV.getId());
            } else {
                tasksView.getItems().clear();
            }
        });

        if (!listsView.getItems().isEmpty()) {
            listsView.getSelectionModel().selectFirst();
        }
    }

    private void loadCategories() {
        List<Category> cats = service.getCategories();
        listsView.getItems().setAll(cats);
    }

    private void loadTodos(int categoryId) {
        List<TodoItem> items = service.getTodosForCategory(categoryId);
        tasksView.getItems().setAll(items);
    }

    @FXML
    private void onAddTask() {
        Category cat = listsView.getSelectionModel().getSelectedItem();
        if (cat == null)
            return;

        service.addTodo(cat.getId(), txtNewTaskTitle.getText(), dpNewTaskDueDate.getValue());

        txtNewTaskTitle.clear();
        dpNewTaskDueDate.setValue(null);

        loadTodos(cat.getId());
    }

    @FXML
    private void onMarkSelectedTaskDone() {
        Category cat = listsView.getSelectionModel().getSelectedItem();
        TodoItem sel = tasksView.getSelectionModel().getSelectedItem();
        if (cat == null || sel == null)
            return;

        service.markDone(sel.getId());
        loadTodos(cat.getId());
    }

    @FXML
    private void onNewList() {
    }

    @FXML
    private void onEditSelectedList() {
    }
}
