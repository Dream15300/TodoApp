package com.example.ui.controller;

import com.example.domain.Category;
import com.example.service.TodoService;
import com.example.ui.UiDialogs;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PopupControl;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.IntConsumer;

public class NewListPopupController {

    private final PopupControl popup = new PopupControl();

    private final TextField nameField = new TextField();

    private final ListView<Category> ownerListsView;
    private final TodoService service;
    private final IntConsumer onCreated;

    private String selectedIcon = "📁";

    private static final List<String> ICONS = List.of(
            "📁", "🛒", "💼", "🎓", "🏠",
            "⭐", "💡", "📌", "✅", "🍕", "🎾", "💘");

    public NewListPopupController(ListView<Category> ownerListsView, TodoService service, IntConsumer onCreated) {
        this.ownerListsView = ownerListsView;
        this.service = service;
        this.onCreated = onCreated;
    }

    public void init() {
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        VBox card = new VBox(12);
        card.getStyleClass().add("category-popup-card");
        card.setFillWidth(true);

        Label lblName = new Label("Name der neuen Liste:");
        lblName.getStyleClass().add("category-popup-label");

        nameField.getStyleClass().add("category-popup-input");

        // Label lblIcon = new Label("Icon:");
        // lblIcon.getStyleClass().add("category-popup-label");

        FlowPane iconGrid = buildIconGrid();
        iconGrid.getStyleClass().add("icon-grid");

        Button btnCancel = new Button("Abbrechen");
        btnCancel.getStyleClass().add("category-popup-btn-cancel");

        Button btnSave = new Button("Speichern");
        btnSave.getStyleClass().add("category-popup-btn-save");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttons = new HBox(6, spacer, btnCancel, btnSave);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(lblName, nameField, iconGrid, buttons); // lblIcon in Klammern setzen, wenn
                                                                          // Icon-Auswahl wieder rein soll

        popup.getScene().setRoot(card);

        popup.setOnShown(e -> {
            var owner = ownerListsView.getScene();
            if (owner != null) {
                popup.getScene().getStylesheets().setAll(owner.getStylesheets());
            }

            Platform.runLater(() -> {
                centerInOwnerScene();
                var popupWindow = popup.getScene().getWindow();
                if (popupWindow != null) {
                    popupWindow.requestFocus();
                }
                nameField.requestFocus();
                nameField.selectAll();
            });
        });

        btnCancel.setOnAction(e -> popup.hide());
        btnSave.setOnAction(e -> commit());

        // ENTER im Textfeld = speichern
        nameField.setOnAction(e -> commit());

        // ESC im Textfeld = schliessen
        nameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                popup.hide();
            }
        });
    }

    public void toggleShowCentered() {
        if (ownerListsView == null || ownerListsView.getScene() == null) {
            return;
        }

        if (popup.isShowing()) {
            popup.hide();
            return;
        }

        nameField.clear();
        selectedIcon = "📁";

        popup.show(ownerListsView, 0, 0);
    }

    private FlowPane buildIconGrid() {
        FlowPane pane = new FlowPane();
        pane.setHgap(8);
        pane.setVgap(8);
        pane.setPadding(new Insets(2, 0, 2, 0));

        for (String icon : ICONS) {
            Button b = new Button(icon);
            b.getStyleClass().add("icon-btn");
            if (icon.equals(selectedIcon)) {
                b.getStyleClass().add("icon-btn-selected");
            }

            b.setOnAction(e -> {
                selectedIcon = icon;

                // alle Buttons entmarkieren, dann aktuellen markieren
                pane.getChildren().forEach(n -> n.getStyleClass().remove("icon-btn-selected"));
                b.getStyleClass().add("icon-btn-selected");
            });

            pane.getChildren().add(b);
        }

        return pane;
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
        if (name.isEmpty()) {
            return;
        }

        try {
            int newId = service.createCategory(name, selectedIcon);
            popup.hide();
            onCreated.accept(newId);
        } catch (Exception exception) {
            UiDialogs.error("Liste konnte nicht erstellt werden: " + exception.getMessage(), exception);
        }
    }
}
