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

/**
 * Verantwortlichkeiten:
 * - Popup-Control erstellen/konfigurieren
 * - Eingaben (Name, Icon) erfassen
 * - Kategorie via TodoService erstellen
 * - Callback (onCreated) mit neuer ID auslösen (UI kann danach reload/reselect)
 *
 * UI-Verhalten:
 * - Toggle: öffnet/schliesst das Popup zentriert
 * - Fokus beim Öffnen auf Name-Feld
 * - ENTER speichert, ESC schliesst (im Textfeld)
 */
public class NewListPopupController {

    // PopupControl = eigenes Window/Scene; gut für "zentriert" und nicht
    // abgeschnitten
    private final PopupControl popup = new PopupControl();

    // Eingabefeld für Listenname
    private final TextField nameField = new TextField();

    // Owner für Positionierung (Scene/Window) und als Styling-Quelle (Stylesheets)
    private final ListView<Category> ownerListsView;

    // Service-Schicht (Geschäftslogik + Persistenz)
    private final TodoService service;

    // Callback nach erfolgreicher Erstellung (liefert neue Kategorie-ID)
    private final IntConsumer onCreated;

    // aktuell gewähltes Icon (Default)
    private String selectedIcon = "📁";

    // Auswahl-Icons (muss visuell zum restlichen UI passen)
    private static final List<String> ICONS = List.of(
            "📁", "🛒", "💼", "🎓", "🏠",
            "⭐", "💡", "📌", "✅", "🍕", "🎾", "💘");

    /**
     * @param ownerListsView ListView als Owner/Anchor für Show + Zentrierung
     * @param service        TodoService
     * @param onCreated      Callback, der nach dem Insert mit neuer ID aufgerufen
     *                       wird
     */
    public NewListPopupController(ListView<Category> ownerListsView, TodoService service, IntConsumer onCreated) {
        this.ownerListsView = ownerListsView;
        this.service = service;
        this.onCreated = onCreated;
    }

    /**
     * Baut Popup-UI auf und registriert Event-Handler.
     *
     * Muss vor toggleShowCentered() aufgerufen werden.
     */
    public void init() {
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        VBox card = new VBox(12);
        card.getStyleClass().add("category-popup-card");
        card.setFillWidth(true);

        Label lblName = new Label("Name der neuen Liste:");
        lblName.getStyleClass().add("category-popup-label");

        nameField.getStyleClass().add("category-popup-input");

        /*
         * Icon-Auswahl:
         * - buildIconGrid() erzeugt Buttons und setzt "selected"-CSS
         * - iconGrid bekommt zusätzliche Klasse "icon-grid"
         * (Layout/Spacing/Breakpoints)
         */
        FlowPane iconGrid = buildIconGrid();
        iconGrid.getStyleClass().add("icon-grid");

        Button btnCancel = new Button("Abbrechen");
        btnCancel.getStyleClass().add("category-popup-btn-cancel");

        Button btnSave = new Button("Speichern");
        btnSave.getStyleClass().add("category-popup-btn-save");

        // Spacer drückt Buttons nach rechts
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttons = new HBox(6, spacer, btnCancel, btnSave);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        /*
         * Popup-Inhalt:
         * - aktuell: Name + IconGrid + Buttons
         */
        card.getChildren().addAll(lblName, nameField, iconGrid, buttons);

        popup.getScene().setRoot(card);

        popup.setOnShown(e -> {
            /*
             * Stylesheets übernehmen:
             * - sorgt für identisches Theme/Design wie Hauptfenster
             */
            var owner = ownerListsView.getScene();
            if (owner != null) {
                popup.getScene().getStylesheets().setAll(owner.getStylesheets());
            }

            /*
             * Wichtig: Zentrierung + Fokus in runLater,
             * damit w/h des Popups korrekt sind und Fokus zuverlässig greift.
             */
            Platform.runLater(() -> {
                centerInOwnerScene();

                // optional: Fokus aufs Popup-Window, dann Feld fokussieren
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

    /**
     * Toggle-Funktion:
     * - wenn offen: schliessen
     * - wenn geschlossen: initialisieren (clear/defaults) und öffnen
     *
     * Anzeige:
     * - popup.show(ownerListsView, 0, 0) zeigt initial, Zentrierung erfolgt im
     * onShown()
     */
    public void toggleShowCentered() {
        if (ownerListsView == null || ownerListsView.getScene() == null) {
            return;
        }

        if (popup.isShowing()) {
            popup.hide();
            return;
        }

        // Zustand zurücksetzen bei jedem Öffnen
        nameField.clear();
        selectedIcon = "📁";

        popup.show(ownerListsView, 0, 0);
    }

    /**
     * Erstellt das Icon-Gitter mit Buttons.
     *
     * Verhalten:
     * - Klick setzt selectedIcon
     * - "selected" CSS-Klasse wird im Grid aktualisiert
     */
    private FlowPane buildIconGrid() {
        FlowPane pane = new FlowPane();
        pane.setHgap(8);
        pane.setVgap(8);
        pane.setPadding(new Insets(2, 0, 2, 0));

        for (String icon : ICONS) {
            Button b = new Button(icon);
            b.getStyleClass().add("category-icon-btn");

            // Initialer Selected-State (wird beim ersten Aufbau gesetzt)
            if (icon.equals(selectedIcon)) {
                b.getStyleClass().add("selected");
            }

            b.setOnAction(e -> {
                selectedIcon = icon;

                // alle Buttons entmarkieren, dann aktuellen markieren
                pane.getChildren().forEach(n -> n.getStyleClass().remove("selected"));
                b.getStyleClass().add("selected");

                // optional: echter Fokus (Tastaturfokus) falls gewünscht
                // b.requestFocus();
            });

            pane.getChildren().add(b);
        }

        return pane;
    }

    /**
     * Zentriert das Popup relativ zur Owner-Scene im Fenster.
     *
     * Grössenproblem:
     * - popup.getWidth()/getHeight() kann 0 sein, wenn Layout noch nicht berechnet
     * ist.
     * - Dann wird CSS/Layout erzwungen und prefWidth/prefHeight verwendet.
     */
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

    /**
     * Validiert Eingabe und erstellt Kategorie.
     *
     * Validierung:
     * - Name darf nicht leer sein
     *
     * Ablauf:
     * - service.createCategory(...) liefert neue ID
     * - Popup wird geschlossen
     * - onCreated.accept(newId) triggert UI-Update (z. B. reload + select)
     *
     * Fehler:
     * - UI-Fehlerdialog via UiDialogs.error(...)
     */
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
