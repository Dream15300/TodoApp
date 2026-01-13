package com.example.ui.controller;

import com.example.domain.Category;
import com.example.service.TodoService;
import com.example.ui.UiDialogs;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.util.List;

/**
 * Verantwortlichkeiten:
 * - Laden/Anzeigen von Kategorien in einer ListView
 * - Öffnen eines Edit-Popups (Name/Icon ändern, Kategorie löschen)
 * - Delegation an TodoService für Geschäftslogik (Update/Delete)
 *
 * UI-Technik:
 * - Custom ListCell mit "⋯" Button pro Zeile
 * - Edit-Popup als PopupControl (statt ContextMenu), damit es nicht
 * abgeschnitten wird
 */
public class CategoriesController {

    private final ListView<Category> listsView;
    private final TodoService service;
    private ConfirmPopupController deleteConfirmPopup;

    // Edit-Popup (PopupControl statt ContextMenu → kein Abschneiden am Fensterrand)
    private final PopupControl editPopup = new PopupControl();
    private TextField nameEditor;
    private FlowPane iconGrid;
    private String selectedIcon;

    // Icon-Set (UI-Optionen für Kategorie-Icon)
    private static final List<String> ICONS = List.of(
            "📁", "🛒", "💼", "🎓", "🏠",
            "⭐", "💡", "📌", "✅", "🍕", "🎾", "💘");

    /**
     * Konstruktor mit Abhängigkeiten.
     */
    public CategoriesController(ListView<Category> listsView, TodoService service) {
        this.listsView = listsView;
        this.service = service;
    }

    /**
     * Initialisiert die UI-Logik.
     *
     * Reihenfolge:
     * - Popup aufbauen
     * - CellFactory setzen
     * - Kategorien laden
     * - Delete-Confirm-Popup initialisieren
     * - Default-Selection: erste Kategorie auswählen (falls vorhanden)
     */
    public void init() {
        setupEditPopup();
        setupCategoryCells();

        deleteConfirmPopup = new ConfirmPopupController(listsView);
        deleteConfirmPopup.init();
    }

    /**
     * Lädt Kategorien aus dem Service und ersetzt die komplette
     * ListView-Items-Liste.
     */
    public void loadCategories() {
        List<Category> categories = service.getCategories();
        listsView.getItems().setAll(categories);
    }

    /**
     * Selektiert eine Kategorie anhand ID (nach Reload sinnvoll).
     *
     * Implementierung:
     * - lineares Suchen über items; bei kleinen Listen ok
     * - Bei sehr vielen Elementen: Map<Id, Category> oder Lookup-Struktur
     */
    public void reselectById(int id) {
        listsView.getItems().stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .ifPresent(c -> listsView.getSelectionModel().select(c));
    }

    /**
     * Öffentliche API für compact Listen-Dropdown:
     * Öffnet exakt denselben Edit-Popup wie der "⋯" Button in der ListCell.
     *
     * @param category Kategorie, die bearbeitet werden soll
     * @param anchor   UI-Node als Bezugspunkt (hier: nur für
     *                 ownerNode-Szene/Fenster)
     */
    public void showEditFor(Category category, Node anchor) {
        showEditPopup(anchor, category);
    }

    /**
     * Baut den Inhalt des Edit-Popups auf und setzt die Event-Handler.
     *
     * Wichtige UI-Details:
     * - AutoHide: Klick ausserhalb schliesst Popup
     * - OnShown: Stylesheets/Themes übernehmen + Fokus setzen (Platform.runLater
     * wegen Layout)
     */
    private void setupEditPopup() {
        editPopup.setAutoHide(true);
        editPopup.setHideOnEscape(true);

        nameEditor = new TextField();
        nameEditor.getStyleClass().add("category-popup-input");
        nameEditor.setMaxWidth(Double.MAX_VALUE);

        Label lblName = new Label("Name der Liste:");

        iconGrid = buildIconGrid();

        Button btnSave = new Button("Speichern");
        Button btnCancel = new Button("Abbrechen");
        Button btnDelete = new Button("Löschen");

        // CSS-Klassen für konsistentes Styling
        btnSave.getStyleClass().add("category-popup-btn-save");
        btnCancel.getStyleClass().add("category-popup-btn-cancel");
        btnDelete.getStyleClass().add("category-popup-btn-danger");

        HBox buttonsRow = new HBox(6, btnDelete, btnCancel, btnSave);
        buttonsRow.setAlignment(Pos.CENTER_RIGHT);

        VBox editorBox = new VBox(10);
        editorBox.getStyleClass().addAll("category-popup-box", "category-edit-popup-card");

        editorBox.setFillWidth(true);
        editorBox.setPadding(new Insets(12));
        editorBox.getChildren().setAll(lblName, nameEditor, iconGrid, buttonsRow);

        // PopupControl nutzt eine eigene Scene → Root setzen
        editPopup.getScene().setRoot(editorBox);

        editPopup.setOnShown(e -> {
            /*
             * Styling/Theme-Synchronisierung:
             * - übernimmt Stylesheets der Haupt-Scene
             * - kopiert Theme-Klassen ("dim"/"light") vom Main-Root auf Popup-Root
             */
            var owner = listsView.getScene();
            if (owner != null) {
                editPopup.getScene().getStylesheets().setAll(owner.getStylesheets());

                var mainRoot = owner.getRoot();
                var popupRoot = editPopup.getScene().getRoot();

                popupRoot.getStyleClass().removeIf(c -> c.equals("dim") || c.equals("light"));
                if (mainRoot.getStyleClass().contains("dim"))
                    popupRoot.getStyleClass().add("dim");
                if (mainRoot.getStyleClass().contains("light"))
                    popupRoot.getStyleClass().add("light");
            }

            /*
             * Fokus erst nach dem Anzeigen/Layout:
             * - requestFocus() direkt im Handler kann zu früh sein, wenn das Popup noch
             * nicht "ready" ist
             * - runLater stellt sicher, dass die Node im Scenegraph finalisiert ist
             */
            Platform.runLater(() -> {
                nameEditor.requestFocus();
                nameEditor.selectAll();
            });
        });

        btnCancel.setOnAction(e -> editPopup.hide());

        // commitEdit() kapselt Validierung + Service-Call + Reload/Reselect
        btnSave.setOnAction(e -> commitEdit());

        // Enter im Textfeld speichert ebenfalls
        nameEditor.setOnAction(e -> commitEdit());

        btnDelete.setOnAction(e -> {
            /*
             * UserData enthält die aktuell bearbeitete Kategorie (gesetzt in
             * showEditPopup).
             * Vorteil:
             * - Kein zusätzlicher Controller-State nötig.
             */
            Category category = (Category) editPopup.getUserData();
            if (category == null) {
                editPopup.hide();
                return;
            }
            editPopup.hide();
            confirmAndDelete(category);
        });
    }

    /**
     * Baut das Icon-Auswahlgitter (Buttons).
     *
     * UI-Verhalten:
     * - FocusTraversable=false verhindert Fokus-Ring/Tab-Fokus auf Icon-Buttons
     * - Klick setzt selectedIcon und markiert visuell über CSS-Klasse "selected"
     */
    private FlowPane buildIconGrid() {
        FlowPane pane = new FlowPane();
        pane.getStyleClass().add("category-icon-grid");
        pane.setHgap(8);
        pane.setVgap(8);

        for (String icon : ICONS) {
            Button b = new Button(icon);
            b.getStyleClass().add("category-icon-btn");
            b.setFocusTraversable(false);
            b.setOnAction(e -> {
                selectedIcon = icon;
                applyIconSelection();
            });
            pane.getChildren().add(b);
        }
        return pane;
    }

    /**
     * Setzt/entfernt die CSS-Klasse "selected" auf Icon-Buttons je nach
     * selectedIcon.
     */
    private void applyIconSelection() {
        for (Node n : iconGrid.getChildren()) {
            if (n instanceof Button b) {
                boolean isSelected = b.getText().equals(selectedIcon);
                if (isSelected) {
                    if (!b.getStyleClass().contains("selected"))
                        b.getStyleClass().add("selected");
                } else {
                    b.getStyleClass().remove("selected");
                }
            }
        }
    }

    /**
     * Öffnet den Edit-Popup, initialisiert Felder und zentriert ihn im
     * Owner-Fenster.
     *
     * Zentrierung:
     * - Popup wird zuerst gezeigt (damit Breite/Höhe berechnet sind)
     * - danach in runLater: Position anhand OwnerWindow + PopupWindow Dimensionen
     * setzen
     *
     * @param ownerNode Anchor-Node, um das Owner-Fenster zu bestimmen
     * @param category  zu bearbeitende Kategorie
     */
    private void showEditPopup(Node ownerNode, Category category) {
        if (category == null)
            return;

        // Category im Popup speichern (für commitEdit/delete)
        editPopup.setUserData(category);

        // Eingabefelder initialisieren
        nameEditor.setText(category.getName());
        nameEditor.selectAll();

        /*
         * Icon-Initialisierung:
         * - Falls Kategorie kein Icon hat, wird erstes Icon aus Liste als Default
         * gewählt.
         */
        String icon = category.getIcon();
        selectedIcon = (icon == null || icon.isBlank()) ? ICONS.getFirst() : icon.trim();
        applyIconSelection();

        // Falls bereits offen: schliessen, um Zustand sauber zu resetten
        if (editPopup.isShowing()) {
            editPopup.hide();
        }

        // --- ZENTRIERUNG ---
        Window ownerWindow = ownerNode.getScene().getWindow();

        // Popup zeigen, damit es eine Window-Instanz + Dimensionen hat
        editPopup.show(ownerWindow);

        Platform.runLater(() -> {
            Window popupWindow = editPopup.getScene().getWindow();

            double x = ownerWindow.getX()
                    + (ownerWindow.getWidth() - popupWindow.getWidth()) / 2;
            double y = ownerWindow.getY()
                    + (ownerWindow.getHeight() - popupWindow.getHeight()) / 2;

            popupWindow.setX(x);
            popupWindow.setY(y);

            // Fokus explizit nach Positionierung/Render
            nameEditor.requestFocus();
            nameEditor.selectAll();
        });
    }

    /**
     * Setzt die CellFactory für die Category-ListView.
     *
     * UI:
     * - Label zeigt Icon + Name (falls Icon vorhanden)
     * - "⋯" Button öffnet Edit-Popup
     *
     * Verhalten:
     * - itemProperty Listener schliesst Popup, falls Item wechselt
     * (z. B. wenn Auswahl wechselt oder Zelle recycled wird)
     *
     * Hinweis:
     * - ListCell wird recycelt; updateItem muss immer vollständig setzen
     * (setGraphic etc.)
     */
    private void setupCategoryCells() {
        listsView.setCellFactory(lv -> new ListCell<>() {

            private final Label nameLabel = new Label();
            private final Button btnEdit = new Button("⋯");
            private final Region spacer = new Region();
            private final HBox root = new HBox(8, nameLabel, spacer, btnEdit);

            {
                root.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(spacer, Priority.ALWAYS);

                btnEdit.getStyleClass().add("category-edit-btn");
                nameLabel.getStyleClass().add("category-name");

                btnEdit.setOnAction(e -> {
                    Category category = getItem();
                    if (category == null)
                        return;
                    showEditPopup(btnEdit, category);
                });

                itemProperty().addListener((obs, oldV, newV) -> {
                    // Popup schliessen, wenn Zelle ein anderes Item bekommt (Recycling/Refresh)
                    if (editPopup.isShowing())
                        editPopup.hide();
                });
            }

            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setContentDisplay(ContentDisplay.TEXT_ONLY);
                    return;
                }

                String icon = item.getIcon();
                if (icon != null && !icon.isBlank()) {
                    nameLabel.setText(icon.trim() + " " + item.getName());
                } else {
                    nameLabel.setText(item.getName());
                }

                setText(null);
                setGraphic(root);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });
    }

    /**
     * Speichert Änderungen aus dem Edit-Popup.
     *
     * Ablauf:
     * - UserData → Kategorie lesen
     * - Name validieren (nicht leer)
     * - Service.updateCategory(...) ausführen
     * - Kategorien neu laden + vorherige Kategorie wieder selektieren
     *
     * Hinweis:
     * - Bei leerem Namen wird aktuell einfach geschlossen (kein Fehlerdialog).
     * Falls UX gewünscht: UiDialogs.warn + Fokus zurück.
     */
    private void commitEdit() {
        Category category = (Category) editPopup.getUserData();
        if (category == null) {
            editPopup.hide();
            return;
        }

        String newName = nameEditor.getText() == null ? "" : nameEditor.getText().trim();
        if (newName.isEmpty()) {
            editPopup.hide();
            return;
        }

        try {
            service.updateCategory(category.getId(), newName, selectedIcon);

            loadCategories();
            reselectById(category.getId());
            editPopup.hide();
        } catch (Exception exception) {
            editPopup.hide();
            UiDialogs.error("Bearbeiten fehlgeschlagen: " + exception.getMessage(), exception);
        }
    }

    /**
     * Zeigt ein Bestätigungs-Popup und löscht danach die Kategorie.
     *
     * Geschäftsregel:
     * - Service wirft IllegalStateException, wenn noch Todos existieren (siehe
     * TodoService.deleteCategory)
     *
     * UI:
     * - Nach Löschen wird Liste neu geladen und erste Kategorie selektiert (falls
     * vorhanden).
     */
    private void confirmAndDelete(Category category) {
        String msg = "Liste \"" + category.getName() + "\" wirklich löschen?";

        deleteConfirmPopup.showCentered(msg, () -> {
            try {
                service.deleteCategory(category.getId());
                loadCategories();
                if (!listsView.getItems().isEmpty()) {
                    listsView.getSelectionModel().selectFirst();
                }
            } catch (Exception exception) {
                UiDialogs.error("Löschen fehlgeschlagen: " + exception.getMessage(), exception);
            }
        });
    }
}
