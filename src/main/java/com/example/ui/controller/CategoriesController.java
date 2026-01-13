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

public class CategoriesController {

    private final ListView<Category> listsView;
    private final TodoService service;
    private ConfirmPopupController deleteConfirmPopup;

    // Edit-Popup (PopupControl statt ContextMenu -> kein Abschneiden)
    private final PopupControl editPopup = new PopupControl();
    private TextField nameEditor;
    private FlowPane iconGrid;
    private String selectedIcon;

    // Icon-Set
    private static final List<String> ICONS = List.of(
            "📁", "🛒", "💼", "🎓", "🏠",
            "⭐", "💡", "📌", "✅", "🍕", "🎾", "💘");

    public CategoriesController(ListView<Category> listsView, TodoService service) {
        this.listsView = listsView;
        this.service = service;
    }

    public void init() {
        setupEditPopup();
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

    /**
     * öffentliche API für compact Listen-Dropdown:
     * öffnet exakt denselben Edit-Popup wie der "…" Button in der ListCell.
     */
    public void showEditFor(Category category, Node anchor) {
        showEditPopup(anchor, category);
    }

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

        editPopup.getScene().setRoot(editorBox);
        editPopup.setOnShown(e -> {
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

            Platform.runLater(() -> {
                nameEditor.requestFocus();
                nameEditor.selectAll();
            });
        });

        btnCancel.setOnAction(e -> editPopup.hide());
        btnSave.setOnAction(e -> commitEdit());
        nameEditor.setOnAction(e -> commitEdit());

        btnDelete.setOnAction(e -> {
            Category category = (Category) editPopup.getUserData();
            if (category == null) {
                editPopup.hide();
                return;
            }
            editPopup.hide();
            confirmAndDelete(category);
        });
    }

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

    private void showEditPopup(Node ownerNode, Category category) {
        if (category == null)
            return;

        editPopup.setUserData(category);

        nameEditor.setText(category.getName());
        nameEditor.selectAll();

        String icon = category.getIcon();
        selectedIcon = (icon == null || icon.isBlank()) ? ICONS.getFirst() : icon.trim();
        applyIconSelection();

        if (editPopup.isShowing()) {
            editPopup.hide();
        }

        // --- ZENTRIERUNG ---
        Window ownerWindow = ownerNode.getScene().getWindow();

        editPopup.show(ownerWindow);

        Platform.runLater(() -> {
            Window popupWindow = editPopup.getScene().getWindow();

            double x = ownerWindow.getX()
                    + (ownerWindow.getWidth() - popupWindow.getWidth()) / 2;
            double y = ownerWindow.getY()
                    + (ownerWindow.getHeight() - popupWindow.getHeight()) / 2;

            popupWindow.setX(x);
            popupWindow.setY(y);

            nameEditor.requestFocus();
            nameEditor.selectAll();
        });
    }

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
