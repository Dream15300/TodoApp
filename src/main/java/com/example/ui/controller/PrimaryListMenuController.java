package com.example.ui.controller;

import com.example.domain.Category;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Zweck:
 * - Im Compact Mode wird statt der linken Listenpane ein Button (btnListMenu)
 * angezeigt.
 * - Klick auf den Button öffnet ein ContextMenu mit:
 * - allen Kategorien (Auswahl möglich)
 * - je Kategorie ein "⋯" Button, der den gleichen Edit-Popup öffnet wie in der
 * ListCell
 * - Eintrag "+ Neue Liste"
 *
 * UI-Details:
 * - ContextMenu wird dynamisch über rebuild() befüllt
 * - CustomMenuItem mit eigener HBox pro Zeile (Label + Spacer + Edit-Button)
 */
public class PrimaryListMenuController {

    private final Button btnListMenu;
    private final ListView<Category> listsView;
    private final CategoriesController categoriesController;

    /*
     * Stabiler Anchor für den Edit-Popup:
     * - wichtig, weil ContextMenu/CustomMenuItem teils instabile Koordinaten/Owner
     * haben
     * - z. B. tasksView oder ein dauerhaft sichtbarer Node
     */
    private final Node stableAnchorForEditPopup; // z.B. tasksView

    /*
     * Callback: "Neue Liste" (UI startet NewListPopupController o. ä.)
     */
    private final Consumer<Void> onNewList;

    private ContextMenu listMenu;

    /*
     * Wird genutzt, um Icon + Name pro Kategorie im Menü zu rendern.
     */
    private final java.util.function.Function<Category, String> iconForCategory;

    public PrimaryListMenuController(
            Button btnListMenu,
            ListView<Category> listsView,
            CategoriesController categoriesController,
            Node stableAnchorForEditPopup,
            Supplier<String> iconForSelectedDummy,
            java.util.function.Function<Category, String> iconForCategory,
            Runnable onNewList) {
        this.btnListMenu = btnListMenu;
        this.listsView = listsView;
        this.categoriesController = categoriesController;
        this.stableAnchorForEditPopup = stableAnchorForEditPopup;

        /*
         * Runnable → Consumer<Void> Adapter.
         * Alternative (sauberer): Feld als Runnable definieren und direkt ausführen.
         */
        this.onNewList = v -> {
            onNewList.run();
        };

        /*
         * iconForSelectedDummy wird nicht verwendet.
         * Das ist aktuell "toter Parameter" und kann entfernt werden, falls keine
         * zukünftige Nutzung geplant ist.
         * (z. B. könnte es für den Header-Button-Text/Icon gedacht gewesen sein.)
         */
        this.iconForCategory = iconForCategory;
    }

    /**
     * Initialisiert das ContextMenu und registriert Klick-Handler auf den Button.
     *
     * Verhalten:
     * - Linksklick toggelt Menü
     * - Vor dem Anzeigen wird rebuild() aufgerufen (aktuelle Liste/Selection)
     */
    public void init() {
        if (btnListMenu == null)
            return;

        listMenu = new ContextMenu();
        listMenu.getStyleClass().add("theme-menu");

        btnListMenu.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY)
                return;

            if (listMenu.isShowing()) {
                listMenu.hide();
            } else {
                rebuild();
                listMenu.show(btnListMenu, Side.BOTTOM, 0, 6);
            }
        });
    }

    /**
     * Schliesst das Menü, falls offen.
     */
    public void hide() {
        if (listMenu != null)
            listMenu.hide();
    }

    /**
     * @return true, falls das Menü aktuell angezeigt wird.
     */
    public boolean isShowing() {
        return listMenu != null && listMenu.isShowing();
    }

    /**
     * Baut die Menüeinträge neu auf.
     *
     * Inhalt:
     * - pro Kategorie eine Zeile:
     * - Label: Icon + Name
     * - Edit-Button "⋯" öffnet Edit-Popup
     * - Klick auf Zeile selektiert Kategorie (ausser Klick auf Edit-Button)
     * - Separator
     * - "+ Neue Liste" Eintrag
     *
     * Layout:
     * - fixe Breite MENU_W sorgt für konsistente Zeilenbreite
     */
    public void rebuild() {
        if (listMenu == null)
            return;

        listMenu.getItems().clear();

        Category selected = listsView.getSelectionModel().getSelectedItem();

        final double MENU_W = 260;
        final double EDIT_W = 36;

        for (Category c : listsView.getItems()) {
            // Label enthält Icon + Name; iconForCategory sollte bereits " " oder "" passend
            // liefern
            Label lbl = new Label(iconForCategory.apply(c) + c.getName());
            lbl.setMinWidth(0);
            lbl.setMaxWidth(Double.MAX_VALUE);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnEdit = new Button("⋯");
            btnEdit.getStyleClass().add("category-edit-btn");
            btnEdit.setFocusTraversable(false);
            btnEdit.setMinWidth(EDIT_W);
            btnEdit.setPrefWidth(EDIT_W);
            btnEdit.setMaxWidth(EDIT_W);

            HBox row = new HBox(10, lbl, spacer, btnEdit);
            row.setAlignment(Pos.CENTER_LEFT);

            // fixe Breite: verhindert "springendes" Menü
            row.setPrefWidth(MENU_W);
            row.setMinWidth(MENU_W);
            row.setMaxWidth(MENU_W);

            row.getStyleClass().add("list-menu-row");
            if (selected != null && c.getId() == selected.getId()) {
                row.getStyleClass().add("list-menu-row-selected");
            }

            /*
             * Klick auf Zeile:
             * - selektiert Kategorie und schliesst Menü
             *
             * Sonderfall:
             * - Klick auf Edit-Button soll NICHT selektieren/schliessen durch Row-Handler,
             * sondern eigenen Handler nutzen.
             * - Deshalb wird geprüft, ob Target im Parent-Chain von btnEdit liegt.
             */
            row.setOnMouseClicked(ev -> {
                if (ev.getButton() != MouseButton.PRIMARY)
                    return;
                if (isInParentChain((Node) ev.getTarget(), btnEdit))
                    return;

                listsView.getSelectionModel().select(c);
                listMenu.hide();
            });

            /*
             * Edit:
             * - selektiert Kategorie (damit UI konsistent ist)
             * - schliesst Menü
             * - öffnet Edit-Popup über CategoriesController an stabilem Anchor
             *
             * ev.consume() verhindert zusätzliche Event-Propagation.
             */
            btnEdit.setOnAction(ev -> {
                ev.consume();
                listsView.getSelectionModel().select(c);
                listMenu.hide();
                categoriesController.showEditFor(c, stableAnchorForEditPopup);
            });

            /*
             * CustomMenuItem:
             * - hideOnClick=false, damit Klicks nicht automatisch das Menü schliessen
             * (Schliessen wird kontrolliert über eigene Handler)
             */
            CustomMenuItem item = new CustomMenuItem(row, false);
            item.setHideOnClick(false);
            item.setMnemonicParsing(false);
            listMenu.getItems().add(item);
        }

        listMenu.getItems().add(new SeparatorMenuItem());

        MenuItem add = new MenuItem("+ Neue Liste");
        add.setOnAction(e -> {
            listMenu.hide();
            onNewList.accept(null);
        });
        listMenu.getItems().add(add);
    }

    /**
     * Prüft, ob target innerhalb der Parent-Kette von expected liegt.
     *
     * Zweck:
     * - Event-Target kann ein Child des Buttons sein (z. B. Text-Node).
     * - So kann man "Klick innerhalb des Edit-Buttons" zuverlässig erkennen.
     *
     * @param target   tatsächliches Event-Target
     * @param expected Node, der in der Parent-Kette vorkommen soll
     * @return true, wenn expected in der Parent-Kette enthalten ist
     */
    private boolean isInParentChain(Node target, Node expected) {
        Node n = target;
        while (n != null) {
            if (n == expected)
                return true;
            n = n.getParent();
        }
        return false;
    }
}
