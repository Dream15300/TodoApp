package com.example.ui.controller;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PopupControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Controller für ein generisches Bestätigungs-Popup (z. B. "Löschen?").
 *
 * Eigenschaften:
 * - PopupControl (eigene Scene/Window), autoHide und ESC schliesst
 * - Zentrierung relativ zur Owner-Scene (nicht nur relativ zu einer
 * Node-Position)
 * - Optionaler Titel (ein-/ausblendbar, ohne Layout-Lücke)
 *
 * Verwendung:
 * - init() einmal aufrufen
 * - showCentered(message, onConfirm) oder showCentered(title, message,
 * onConfirm)
 */
public class ConfirmPopupController {

    // PopupControl erzeugt ein separates Popup-Window mit eigener Scene
    private final PopupControl popup = new PopupControl();

    // UI-Elemente (Labels/Buttons) werden einmal erstellt und wiederverwendet
    private final Label titleLabel = new Label();
    private final Label messageLabel = new Label();

    private final Button btnCancel = new Button("Abbrechen");
    private final Button btnConfirm = new Button("Löschen");

    // Owner-Node als Anker zur Bestimmung von Scene/Window für Positionierung und
    // CSS
    private final Node ownerNode;

    /*
     * Callback wird beim Confirm ausgelöst.
     * Default: no-op, damit run() nie null ist.
     */
    private Runnable onConfirm = () -> {
    };

    /**
     * @param ownerNode Node innerhalb der Owner-Scene (z. B. ListView),
     *                  damit das Popup korrekt zentriert und gestylt werden kann.
     */
    public ConfirmPopupController(Node ownerNode) {
        this.ownerNode = ownerNode;
    }

    /**
     * Baut UI-Struktur auf und registriert Event-Handler.
     *
     * Wichtig:
     * - Muss vor showCentered(...) aufgerufen werden.
     * - Wiederverwendet dieselbe Popup-Instanz für mehrere Aufrufe.
     */
    public void init() {
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        // Karte (Container des Popups)
        VBox card = new VBox(8);
        card.getStyleClass().addAll("category-popup-card", "confirm-popup-card");
        card.setFillWidth(true);

        /*
         * Inhalt separat:
         * - titleLabel sitzt direkt im card-Container
         * - contentBox enthält Message + Buttons
         * Vorteil:
         * - Wenn Titel ausgeblendet wird, entsteht keine zusätzliche "Leerzeile"
         */
        VBox contentBox = new VBox(10);
        contentBox.setFillWidth(true);

        HBox buttons = new HBox(12, btnCancel, btnConfirm);
        buttons.setAlignment(Pos.CENTER);

        // CSS-Hooks
        messageLabel.getStyleClass().add("confirm-message");
        titleLabel.getStyleClass().add("confirm-title");

        btnCancel.getStyleClass().add("category-popup-btn-cancel");
        btnConfirm.getStyleClass().add("category-popup-btn-danger");

        // Aufbau der Node-Hierarchie
        contentBox.getChildren().addAll(messageLabel, buttons);
        card.getChildren().addAll(titleLabel, contentBox);

        // PopupRoot setzen (PopupControl nutzt eigene Scene)
        popup.getScene().setRoot(card);

        popup.setOnShown(e -> {
            /*
             * Stylesheets vom Owner übernehmen:
             * - stellt sicher, dass Theme (CSS) identisch aussieht wie Hauptfenster
             * - hier wird setAll genutzt → ersetzt vollständig
             */
            var scene = ownerNode.getScene();
            if (scene != null) {
                popup.getScene().getStylesheets().setAll(scene.getStylesheets());
            }
        });

        // Abbrechen: Popup schliessen
        btnCancel.setOnAction(e -> popup.hide());

        // Bestätigen: Popup schliessen, danach Callback ausführen
        btnConfirm.setOnAction(e -> {
            popup.hide();
            onConfirm.run();
        });
    }

    /**
     * Overload ohne Titel.
     *
     * @param message   Text im Popup
     * @param onConfirm Callback bei "Löschen"
     */
    public void showCentered(String message, Runnable onConfirm) {
        showCentered(null, message, onConfirm);
    }

    /**
     * Zeigt das Popup zentriert im Owner-Fenster.
     *
     * Layout-Handling:
     * - Popup wird zuerst gezeigt (damit ein Window existiert)
     * - Zentrierung passiert in Platform.runLater, damit Grössen berechnet sind
     *
     * @param title     optionaler Titel (null/blank → ausgeblendet)
     * @param message   Nachricht (null → "")
     * @param onConfirm Callback (null → no-op)
     */
    public void showCentered(String title, String message, Runnable onConfirm) {
        if (ownerNode == null || ownerNode.getScene() == null)
            return;

        // Null-sicherer Callback
        this.onConfirm = (onConfirm == null) ? () -> {
        } : onConfirm;

        // Titel ein-/ausblenden ohne Layout-Reserve
        boolean hasTitle = title != null && !title.isBlank();
        titleLabel.setText(hasTitle ? title : "");
        titleLabel.setVisible(hasTitle);
        titleLabel.setManaged(hasTitle);

        // Message null-sicher
        messageLabel.setText(message == null ? "" : message);

        // Bei Wiederaufruf altes Popup sicher schliessen
        if (popup.isShowing())
            popup.hide();

        /*
         * show(ownerNode, 0, 0):
         * - Popup wird an einer Initialposition angezeigt
         * - Danach wird es in centerInOwnerScene() exakt zentriert
         */
        popup.show(ownerNode, 0, 0);

        // Zentrierung nach dem Render/Layout
        Platform.runLater(this::centerInOwnerScene);
    }

    /**
     * Zentriert das Popup relativ zur Owner-Scene im zugehörigen Window.
     *
     * Problem:
     * - Direkt nach popup.show() sind popup.getWidth()/getHeight() oft 0.
     *
     * Lösung:
     * - Wenn w/h <= 0: CSS anwenden + layout durchführen + prefWidth/prefHeight
     * berechnen
     * - Danach absolute Bildschirmkoordinaten berechnen und via popup.setX/Y setzen
     */
    private void centerInOwnerScene() {
        var ownerScene = ownerNode.getScene();
        double w = popup.getWidth();
        double h = popup.getHeight();

        if (w <= 0 || h <= 0) {
            // Erzwingt CSS/Layout, um korrekte Preferred-Grössen zu erhalten
            popup.getScene().getRoot().applyCss();
            popup.getScene().getRoot().layout();
            w = popup.getScene().getRoot().prefWidth(-1);
            h = popup.getScene().getRoot().prefHeight(-1);
        }

        /*
         * Koordinatensystem:
         * - ownerScene.getWindow().getX/Y: Fenster-Position auf dem Bildschirm
         * - ownerScene.getX/Y: Scene-Offset im Fenster (meist 0, aber nicht garantiert)
         * - ownerScene.getWidth/Height: sichtbare Scene-Fläche
         */
        double x = ownerScene.getWindow().getX() + ownerScene.getX() + (ownerScene.getWidth() - w) / 2.0;
        double y = ownerScene.getWindow().getY() + ownerScene.getY() + (ownerScene.getHeight() - h) / 2.0;

        popup.setX(x);
        popup.setY(y);
    }
}
