package com.example.ui.controller;

import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class PrimaryDetailsSizingController {

    private final HBox tasksAndDetailsContainer;
    private final ListView<?> tasksView;
    private final VBox detailsPane;

    public PrimaryDetailsSizingController(HBox tasksAndDetailsContainer, ListView<?> tasksView, VBox detailsPane) {
        this.tasksAndDetailsContainer = tasksAndDetailsContainer;
        this.tasksView = tasksView;
        this.detailsPane = detailsPane;
    }

    public void apply(boolean compactMode) {
        if (tasksAndDetailsContainer == null || tasksView == null || detailsPane == null)
            return;

        boolean detailsOpen = detailsPane.isManaged() && detailsPane.isVisible();

        // alte Bindings sauber entfernen
        if (detailsPane.prefWidthProperty().isBound())
            detailsPane.prefWidthProperty().unbind();
        if (tasksView.prefWidthProperty().isBound())
            tasksView.prefWidthProperty().unbind();

        if (!detailsOpen) {
            // Details zu -> Tasks immer voll
            detailsPane.setVisible(false);
            detailsPane.setManaged(false);

            tasksView.setVisible(true);
            tasksView.setManaged(true);

            HBox.setHgrow(tasksView, Priority.ALWAYS);
            HBox.setHgrow(detailsPane, Priority.NEVER);

            tasksView.setPrefWidth(Region.USE_COMPUTED_SIZE);
            detailsPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
            return;
        }

        // Details offen
        if (compactMode) {
            // Compact: Details 100%, Tasks ausblenden
            tasksView.setVisible(false);
            tasksView.setManaged(false);

            detailsPane.setVisible(true);
            detailsPane.setManaged(true);

            HBox.setHgrow(detailsPane, Priority.ALWAYS);
            HBox.setHgrow(tasksView, Priority.NEVER);

            detailsPane.prefWidthProperty().bind(tasksAndDetailsContainer.widthProperty());
            detailsPane.setMaxWidth(Double.MAX_VALUE);
        } else {
            // Normal: 50/50 Split
            tasksView.setVisible(true);
            tasksView.setManaged(true);

            detailsPane.setVisible(true);
            detailsPane.setManaged(true);

            HBox.setHgrow(tasksView, Priority.ALWAYS);
            HBox.setHgrow(detailsPane, Priority.ALWAYS);

            detailsPane.prefWidthProperty().bind(tasksAndDetailsContainer.widthProperty().multiply(0.5));
            tasksView.prefWidthProperty().bind(tasksAndDetailsContainer.widthProperty().multiply(0.5));
        }
    }

    public void apply() {
        apply(false);
    }
}
