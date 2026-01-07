package com.example.ui.controller;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PopupControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class ConfirmPopupController {

    private final PopupControl popup = new PopupControl();

    private final Label titleLabel = new Label();
    private final Label messageLabel = new Label();

    private final Button btnCancel = new Button("Abbrechen");
    private final Button btnConfirm = new Button("Löschen");

    private final Node ownerNode;

    private Runnable onConfirm = () -> {
    };

    public ConfirmPopupController(Node ownerNode) {
        this.ownerNode = ownerNode;
    }

    public void init() {
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        // Karte
        VBox card = new VBox(8);
        card.getStyleClass().addAll("category-popup-card", "confirm-popup-card");
        card.setFillWidth(true);

        // Inhalt (separat, damit kein Spacing "oben" entsteht, wenn Titel fehlt)
        VBox contentBox = new VBox(10);
        contentBox.setFillWidth(true);

        HBox buttons = new HBox(12, btnCancel, btnConfirm);
        buttons.setAlignment(Pos.CENTER);

        messageLabel.getStyleClass().add("confirm-message");
        titleLabel.getStyleClass().add("confirm-title");

        btnCancel.getStyleClass().add("category-popup-btn-cancel");
        btnConfirm.getStyleClass().add("category-popup-btn-danger");

        contentBox.getChildren().addAll(messageLabel, buttons);
        card.getChildren().addAll(titleLabel, contentBox);

        popup.getScene().setRoot(card);
        popup.getScene().setFill(Color.TRANSPARENT);

        popup.setOnShown(e -> {
            var scene = ownerNode.getScene();
            if (scene != null) {
                popup.getScene().getStylesheets().setAll(scene.getStylesheets());
            }
        });

        btnCancel.setOnAction(e -> popup.hide());
        btnConfirm.setOnAction(e -> {
            popup.hide();
            onConfirm.run();
        });
    }

    // Overload: ohne Titel
    public void showCentered(String message, Runnable onConfirm) {
        showCentered(null, message, onConfirm);
    }

    public void showCentered(String title, String message, Runnable onConfirm) {
        if (ownerNode == null || ownerNode.getScene() == null)
            return;

        this.onConfirm = (onConfirm == null) ? () -> {
        } : onConfirm;

        boolean hasTitle = title != null && !title.isBlank();
        titleLabel.setText(hasTitle ? title : "");
        titleLabel.setVisible(hasTitle);
        titleLabel.setManaged(hasTitle);

        messageLabel.setText(message == null ? "" : message);

        if (popup.isShowing())
            popup.hide();
        popup.show(ownerNode, 0, 0);

        Platform.runLater(this::centerInOwnerScene);
    }

    private void centerInOwnerScene() {
        var ownerScene = ownerNode.getScene();
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
}
