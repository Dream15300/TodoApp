package com.example.ui.controller;

import com.example.domain.Category;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class PrimaryLayoutController {

    private final VBox listsPane;
    private final SplitPane rootSplit;
    private final Label tasksTitleLabel;
    private final Button btnListMenu;

    private final double compactBreakpoint;

    private boolean compactMode = false;

    private final double listsPrefW;
    private final double listsMinW;
    private final double listsMaxW;

    private double splitDividerPos = 0.28;

    public PrimaryLayoutController(
            VBox listsPane,
            SplitPane rootSplit,
            Label tasksTitleLabel,
            Button btnListMenu,
            double compactBreakpoint) {
        this.listsPane = listsPane;
        this.rootSplit = rootSplit;
        this.tasksTitleLabel = tasksTitleLabel;
        this.btnListMenu = btnListMenu;
        this.compactBreakpoint = compactBreakpoint;

        this.listsPrefW = listsPane.getPrefWidth();
        this.listsMinW = listsPane.getMinWidth();
        this.listsMaxW = listsPane.getMaxWidth();

        if (rootSplit != null && !rootSplit.getDividers().isEmpty()) {
            this.splitDividerPos = rootSplit.getDividerPositions()[0];
        }
    }

    public void init() {
        SplitPane.setResizableWithParent(listsPane, false);

        Platform.runLater(() -> {
            var scene = listsPane.getScene();
            if (scene == null)
                return;

            applyCompactMode(scene.getWidth());
            scene.widthProperty().addListener((o, oldW, newW) -> applyCompactMode(newW.doubleValue()));
        });
    }

    public boolean isCompactMode() {
        return compactMode;
    }

    public void applyCompactMode(double width) {
        boolean shouldCompact = width < compactBreakpoint;
        if (shouldCompact == compactMode)
            return;
        compactMode = shouldCompact;

        listsPane.setVisible(!compactMode);
        listsPane.setManaged(!compactMode);

        if (!compactMode) {
            listsPane.setPrefWidth(listsPrefW);
            listsPane.setMinWidth(listsMinW);
            listsPane.setMaxWidth(listsMaxW);
        } else {
            listsPane.setPrefWidth(0);
            listsPane.setMinWidth(0);
            listsPane.setMaxWidth(0);
        }

        if (rootSplit != null) {
            if (compactMode) {
                rootSplit.setDividerPositions(0.0);
                if (!rootSplit.getStyleClass().contains("compact")) {
                    rootSplit.getStyleClass().add("compact");
                }
            } else {
                rootSplit.setDividerPositions(splitDividerPos);
                rootSplit.getStyleClass().remove("compact");
            }
        }

        if (tasksTitleLabel != null) {
            tasksTitleLabel.setVisible(!compactMode);
            tasksTitleLabel.setManaged(!compactMode);
        }
        if (btnListMenu != null) {
            btnListMenu.setVisible(compactMode);
            btnListMenu.setManaged(compactMode);
        }
    }

    public void updateHeaderTexts(Category selected, String titleText) {
        if (tasksTitleLabel != null)
            tasksTitleLabel.setText(titleText);
        if (btnListMenu != null)
            btnListMenu.setText(titleText);
    }
}
