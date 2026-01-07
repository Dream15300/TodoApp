package com.example.ui.controller;

import com.example.domain.Category;
import com.example.service.TodoService;
import com.example.ui.UiDialogs;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PopupControl;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.control.ListView;

import java.util.function.IntConsumer;

public class NewListPopupController {

    private final PopupControl popup = new PopupControl();
    private final TextField nameField = new TextField();

    private final ListView<Category> ownerListsView;
    private final TodoService service;

    // Callback: neue Kategorie-ID (damit caller reselect/refresh machen kann)
    private final IntConsumer onCreated;

    public NewListPopupController(ListView<Category> ownerListsView, TodoService service, IntConsumer onCreated) {
        this.ownerListsView = ownerListsView;
        this.service = service;
        this.onCreated = onCreated;
    }

    public void init() {
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        VBox card = new VBox(10);
        card.getStyleClass().add("category-popup-card");
        card.setFillWidth(true);

        Label lbl = new Label("Name der neuen Liste:");
        nameField.getStyleClass().add("category-popup-input");

        Button btnCancel = new Button("Abbrechen");
        btnCancel.getStyleClass().add("category-popup-btn-cancel");

        Button btnSave = new Button("Speichern");
        btnSave.getStyleClass().add("category-popup-btn-save");

        HBox buttons = new HBox(12, btnCancel, btnSave);
        buttons.setAlignment(Pos.CENTER);

        card.getChildren().addAll(lbl, nameField, buttons);

        popup.getScene().setRoot(card);
        popup.getScene().setFill(Color.TRANSPARENT);

        popup.setOnShown(e -> {
            var owner = ownerListsView.getScene();
            if (owner != null)
                popup.getScene().getStylesheets().setAll(owner.getStylesheets());
        });

        btnCancel.setOnAction(e -> popup.hide());
        btnSave.setOnAction(e -> commit());
        nameField.setOnAction(e -> commit());
    }

    public void toggleShowCentered() {
        if (ownerListsView == null || ownerListsView.getScene() == null)
            return;

        if (popup.isShowing()) {
            popup.hide();
            return;
        }

        nameField.clear();
        popup.show(ownerListsView, 0, 0);

        Platform.runLater(() -> {
            centerInOwnerScene();
            Platform.runLater(() -> {
                ownerListsView.getScene().getWindow().requestFocus();
                nameField.requestFocus();
                nameField.selectAll();
            });
        });
    }

    private void centerInOwnerScene() {
        var ownerScene = ownerListsView.getScene();
        double w = popup.getWidth();
        double h = popup.getHeight();

        if (w <= 0 || h <= 0) {
            popup.getScene().getRoot().applyCss();
            popup.getScene().getRoot().layout();
            w = popup.getScene().getRoot().prefWidth(-1);
            h = popup.getScene().getRoot().prefHeight(-1);
        }

        double x = ownerScene.getWindow().getX() + ownerScene.getX() + (ownerScene.getWidth() - w) / 2.0;
        double y = ownerScene.getWindow().getY() + ownerScene.getY() + (ownerScene.getHeight() - h) / 2.0;
        popup.setX(x);
        popup.setY(y);
    }

    private void commit() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty())
            return;

        try {
            int newId = service.createCategory(name);
            popup.hide();
            onCreated.accept(newId);
        } catch (Exception exception) {
            UiDialogs.error("Liste konnte nicht erstellt werden: " + exception.getMessage(), exception);
        }
    }
}
