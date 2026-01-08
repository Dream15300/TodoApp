package com.example.ui.controller;

import com.example.domain.Category;
import com.example.service.TodoService;
import com.example.ui.UiDialogs;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class CategoriesController {

    private final ListView<Category> listsView;
    private final TodoService service;
    private ConfirmPopupController deleteConfirmPopup;

    public CategoriesController(ListView<Category> listsView, TodoService service) {
        this.listsView = listsView;
        this.service = service;
    }

    public void init() {
        setupCategoryCells();
        loadCategories();

        deleteConfirmPopup = new ConfirmPopupController(listsView);
        deleteConfirmPopup.init();

        if (!listsView.getItems().isEmpty()) {
            listsView.getSelectionModel().selectFirst();
        }
    }

    public void loadCategories() {
        List<Category> categories = service.getCategories();
        listsView.getItems().setAll(categories);
    }

    public void reselectById(int id) {
        listsView.getItems().stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .ifPresent(c -> listsView.getSelectionModel().select(c));
    }

    private void setupCategoryCells() {
        listsView.setCellFactory(lv -> new ListCell<>() {

            private final Label nameLabel = new Label();
            private final Button btnEdit = new Button("✎");
            private final Region spacer = new Region();
            private final HBox root = new HBox(8, nameLabel, spacer, btnEdit);

            private final ContextMenu editMenu = new ContextMenu();
            private final TextField nameEditor = new TextField();
            private final Button btnSave = new Button("Speichern");
            private final Button btnCancel = new Button("Abbrechen");
            private final Button btnDelete = new Button("Löschen");

            private final VBox editorBox = new VBox(6);
            private final HBox buttonsRow = new HBox(6);
            private final CustomMenuItem editorItem = new CustomMenuItem();

            {
                root.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(spacer, Priority.ALWAYS);

                btnEdit.getStyleClass().add("category-edit-btn");
                nameEditor.getStyleClass().add("category-popup-input");
                nameEditor.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(nameEditor, Priority.ALWAYS);

                btnSave.getStyleClass().add("category-popup-btn-save");
                btnCancel.getStyleClass().add("category-popup-btn-cancel");
                btnDelete.getStyleClass().add("category-popup-btn-danger");

                buttonsRow.getChildren().setAll(btnDelete, btnCancel, btnSave);
                buttonsRow.setAlignment(Pos.CENTER_RIGHT);

                editorBox.getStyleClass().add("category-popup-box");
                editorBox.getChildren().setAll(new Label("Name:"), nameEditor, buttonsRow);

                editorItem.setContent(editorBox);
                editorItem.setHideOnClick(false);
                editorItem.setMnemonicParsing(false);
                editorItem.getStyleClass().add("category-popup-item");

                editMenu.getItems().setAll(editorItem);
                editMenu.getStyleClass().add("category-edit-menu");

                btnEdit.setOnAction(e -> {
                    Category category = getItem();
                    if (category == null)
                        return;

                    nameEditor.setText(category.getName());
                    nameEditor.selectAll();

                    if (editMenu.isShowing())
                        editMenu.hide();
                    editMenu.show(btnEdit, Side.BOTTOM, 0, 6);

                    nameEditor.requestFocus();
                });

                btnCancel.setOnAction(e -> editMenu.hide());
                btnSave.setOnAction(e -> commitRename());
                nameEditor.setOnAction(e -> commitRename());

                btnDelete.setOnAction(e -> {
                    Category category = getItem();
                    if (category == null)
                        return;
                    editMenu.hide();
                    confirmAndDelete(category);
                });

                itemProperty().addListener((obs, oldV, newV) -> {
                    if (editMenu.isShowing())
                        editMenu.hide();
                });
            }

            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    if (editMenu.isShowing())
                        editMenu.hide();
                    return;
                }

                nameLabel.setText(item.getName());
                setText(null);
                setGraphic(root);
            }

            private void commitRename() {
                Category category = getItem();
                if (category == null) {
                    editMenu.hide();
                    return;
                }

                String newName = nameEditor.getText() == null ? "" : nameEditor.getText().trim();
                if (newName.isEmpty()) {
                    editMenu.hide();
                    return;
                }

                try {
                    service.renameCategory(category.getId(), newName);
                    loadCategories();
                    reselectById(category.getId());
                    editMenu.hide();
                } catch (Exception exception) {
                    editMenu.hide();
                    UiDialogs.error("Bearbeiten fehlgeschlagen: " + exception.getMessage(), exception);
                }
            }
        });
    }

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
                com.example.ui.UiDialogs.error(
                        "Löschen fehlgeschlagen: " + exception.getMessage(),
                        exception);
            }
        });
    }

}
