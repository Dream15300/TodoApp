package com.example.ui.controller;

import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
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

    public void apply() {
        if (tasksAndDetailsContainer == null || tasksView == null || detailsPane == null)
            return;

        boolean detailsOpen = detailsPane.isManaged() && detailsPane.isVisible();

        if (detailsOpen) {
            if (!detailsPane.prefWidthProperty().isBound()) {
                detailsPane.prefWidthProperty().bind(tasksAndDetailsContainer.widthProperty().multiply(0.5));
            }
            if (!tasksView.prefWidthProperty().isBound()) {
                tasksView.prefWidthProperty().bind(tasksAndDetailsContainer.widthProperty().multiply(0.5));
            }
        } else {
            if (detailsPane.prefWidthProperty().isBound())
                detailsPane.prefWidthProperty().unbind();
            if (tasksView.prefWidthProperty().isBound())
                tasksView.prefWidthProperty().unbind();

            tasksView.setPrefWidth(Region.USE_COMPUTED_SIZE);
            detailsPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
        }
    }
}
