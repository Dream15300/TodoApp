package com.example.ui.controller;

import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Ziel:
 * - Wenn Details offen: tasksView und detailsPane teilen sich den verfügbaren
 * Platz (50/50)
 * - Wenn Details geschlossen: Bindings lösen, Standardgrössen wiederherstellen
 *
 * Kontext:
 * - tasksAndDetailsContainer ist typischerweise ein HBox-Container, der
 * ListView + DetailsVBox enthält.
 * - detailsPane wird über managed/visible gesteuert (z. B.
 * DetailsController.open/close).
 */
public class PrimaryDetailsSizingController {

    private final HBox tasksAndDetailsContainer;
    private final ListView<?> tasksView;
    private final VBox detailsPane;

    /**
     * @param tasksAndDetailsContainer HBox, die tasksView und detailsPane enthält
     * @param tasksView                ListView für Todo-Items (wildcard, da Typ
     *                                 hier nicht relevant)
     * @param detailsPane              VBox für Detailansicht
     */
    public PrimaryDetailsSizingController(HBox tasksAndDetailsContainer, ListView<?> tasksView, VBox detailsPane) {
        this.tasksAndDetailsContainer = tasksAndDetailsContainer;
        this.tasksView = tasksView;
        this.detailsPane = detailsPane;
    }

    /**
     * Wendet die Sizing-Regeln an.
     *
     * Erkennung "Details offen":
     * - detailsPane.isManaged() && detailsPane.isVisible()
     * → entspricht typischer JavaFX-Praxis: managed=false entfernt aus Layoutfluss,
     * visible=false blendet aus.
     *
     * Umsetzung:
     * - Wenn offen: prefWidth von tasksView und detailsPane an Containerbreite
     * binden (0.5)
     * - Wenn zu: Bindings lösen und prefWidth auf USE_COMPUTED_SIZE zurücksetzen
     */
    public void apply() {
        if (tasksAndDetailsContainer == null || tasksView == null || detailsPane == null)
            return;

        boolean detailsOpen = detailsPane.isManaged() && detailsPane.isVisible();

        if (detailsOpen) {
            /*
             * Bindings nur setzen, wenn noch nicht gebunden:
             * - verhindert IllegalStateException (doppelte Bindings)
             * - verhindert unnötige Neu-Bindings
             */
            if (!detailsPane.prefWidthProperty().isBound()) {
                detailsPane.prefWidthProperty().bind(tasksAndDetailsContainer.widthProperty().multiply(0.5));
            }
            if (!tasksView.prefWidthProperty().isBound()) {
                tasksView.prefWidthProperty().bind(tasksAndDetailsContainer.widthProperty().multiply(0.5));
            }
        } else {
            // Bindings lösen, damit JavaFX wieder frei layouten kann
            if (detailsPane.prefWidthProperty().isBound())
                detailsPane.prefWidthProperty().unbind();
            if (tasksView.prefWidthProperty().isBound())
                tasksView.prefWidthProperty().unbind();

            /*
             * prefWidth auf Standard:
             * - USE_COMPUTED_SIZE: Layout-Engine berechnet Breite basierend auf
             * Inhalt/Constraints
             */
            tasksView.setPrefWidth(Region.USE_COMPUTED_SIZE);
            detailsPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
        }
    }

    public void apply(boolean compactMode) {
        if (tasksAndDetailsContainer == null || tasksView == null || detailsPane == null)
            return;

        boolean detailsOpen = detailsPane.isManaged() && detailsPane.isVisible();

        // --- COMPACT MODE ---
        if (compactMode) {
            if (detailsOpen) {
                // Details: 100%
                if (detailsPane.prefWidthProperty().isBound())
                    detailsPane.prefWidthProperty().unbind();
                if (tasksView.prefWidthProperty().isBound())
                    tasksView.prefWidthProperty().unbind();

                detailsPane.setPrefWidth(Region.USE_COMPUTED_SIZE);

                tasksView.setVisible(false);
                tasksView.setManaged(false);
            } else {
                // Keine Details -> Tasks 100%
                tasksView.setVisible(true);
                tasksView.setManaged(true);

                detailsPane.setVisible(false);
                detailsPane.setManaged(false);
            }
            return;
        }

        // --- NORMAL MODE ---
        tasksView.setVisible(true);
        tasksView.setManaged(true);

        if (detailsOpen) {
            if (!detailsPane.prefWidthProperty().isBound()) {
                detailsPane.prefWidthProperty()
                        .bind(tasksAndDetailsContainer.widthProperty().multiply(0.5));
            }
            if (!tasksView.prefWidthProperty().isBound()) {
                tasksView.prefWidthProperty()
                        .bind(tasksAndDetailsContainer.widthProperty().multiply(0.5));
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
