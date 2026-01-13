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

public class PrimaryListMenuController {

    private final Button btnListMenu;
    private final ListView<Category> listsView;
    private final CategoriesController categoriesController;
    private final Node stableAnchorForEditPopup; // z.B. tasksView

    private final Consumer<Void> onNewList; // callback

    private ContextMenu listMenu;

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
        this.onNewList = v -> {
            onNewList.run();
        };
        this.iconForCategory = iconForCategory;
    }

    private final java.util.function.Function<Category, String> iconForCategory;

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

    public void hide() {
        if (listMenu != null)
            listMenu.hide();
    }

    public boolean isShowing() {
        return listMenu != null && listMenu.isShowing();
    }

    public void rebuild() {
        if (listMenu == null)
            return;

        listMenu.getItems().clear();

        Category selected = listsView.getSelectionModel().getSelectedItem();

        final double MENU_W = 260;
        final double EDIT_W = 36;

        for (Category c : listsView.getItems()) {
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
            row.setPrefWidth(MENU_W);
            row.setMinWidth(MENU_W);
            row.setMaxWidth(MENU_W);

            row.getStyleClass().add("list-menu-row");
            if (selected != null && c.getId() == selected.getId()) {
                row.getStyleClass().add("list-menu-row-selected");
            }

            row.setOnMouseClicked(ev -> {
                if (ev.getButton() != MouseButton.PRIMARY)
                    return;
                if (isInParentChain((Node) ev.getTarget(), btnEdit))
                    return;

                listsView.getSelectionModel().select(c);
                listMenu.hide();
            });

            btnEdit.setOnAction(ev -> {
                ev.consume();
                listsView.getSelectionModel().select(c);
                listMenu.hide();
                categoriesController.showEditFor(c, stableAnchorForEditPopup);
            });

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
