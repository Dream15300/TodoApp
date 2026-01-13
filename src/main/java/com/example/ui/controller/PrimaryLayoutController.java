package com.example.ui.controller;

import com.example.domain.Category;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

/**
 * Ziel:
 * - Ab einer bestimmten Breite (compactBreakpoint) in "Compact Mode" wechseln:
 * - Listenpane links ausblenden (visible/managed + Breiten auf 0)
 * - SplitPane Divider auf 0.0 setzen
 * - Header umstellen: statt tasksTitleLabel wird btnListMenu angezeigt
 *
 * - In Normal Mode:
 * - Listenpane wieder einblenden und ursprüngliche Breiten wiederherstellen
 * - SplitPane Divider zurücksetzen
 */
public class PrimaryLayoutController {

    private final VBox listsPane;
    private final SplitPane rootSplit;
    private final Label tasksTitleLabel;
    private final Button btnListMenu;

    // Schwellwert (Fensterbreite), unterhalb wird Compact Mode aktiviert
    private final double compactBreakpoint;

    private boolean compactMode = false;

    /*
     * Originalwerte der Listenpane:
     * - werden beim Wechsel zurück in Normal Mode wiederhergestellt
     */
    private final double listsPrefW;
    private final double listsMinW;
    private final double listsMaxW;

    /*
     * Divider-Position merken, um nach Compact Mode wieder auf die vorherige
     * Position zu gehen.
     */
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

        // Initialbreiten sichern
        this.listsPrefW = listsPane.getPrefWidth();
        this.listsMinW = listsPane.getMinWidth();
        this.listsMaxW = listsPane.getMaxWidth();

        /*
         * Divider-Position aus SplitPane übernehmen, falls vorhanden.
         * - Diese Position ist der Startwert; Änderungen zur Laufzeit werden hier nicht
         * automatisch mitgetrackt.
         */
        if (rootSplit != null && !rootSplit.getDividers().isEmpty()) {
            this.splitDividerPos = rootSplit.getDividerPositions()[0];
        }
    }

    /**
     * Initialisiert Responsive-Listener.
     *
     * Ablauf:
     * - SplitPane.setResizableWithParent(listsPane, false): verhindert, dass
     * SplitPane die Listenpane "streckt"
     * - runLater: erst nach Scene-Attach, damit scene.getWidth() gültig ist
     * - widthListener: bei jeder Breitenänderung Compact Mode neu prüfen/anwenden
     */
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

    /**
     * @return aktueller Zustand (true = Compact Mode aktiv)
     */
    public boolean isCompactMode() {
        return compactMode;
    }

    /**
     * Schaltet Compact Mode abhängig von der aktuellen Breite.
     *
     * Implementierungsdetails:
     * - "shouldCompact" anhand width < compactBreakpoint
     * - Nur bei Zustandswechsel (shouldCompact != compactMode) werden
     * UI-Eigenschaften verändert
     *
     * Listenpane:
     * - In Compact: visible/managed false + pref/min/max auf 0
     * - In Normal: visible/managed true + Originalwerte wiederherstellen
     *
     * SplitPane:
     * - In Compact: dividerPositions = 0.0 und CSS-Klasse "compact" hinzufügen
     * - In Normal: dividerPositions = splitDividerPos und CSS-Klasse entfernen
     *
     * Header:
     * - tasksTitleLabel wird im Compact Mode ausgeblendet
     * - btnListMenu wird im Compact Mode eingeblendet
     */
    public void applyCompactMode(double width) {
        boolean shouldCompact = width < compactBreakpoint;
        if (shouldCompact == compactMode)
            return;
        compactMode = shouldCompact;

        // Listenpanel ein-/ausblenden
        listsPane.setVisible(!compactMode);
        listsPane.setManaged(!compactMode);

        // Breitensteuerung: in Compact Mode effektiv auf 0 setzen
        if (!compactMode) {
            listsPane.setPrefWidth(listsPrefW);
            listsPane.setMinWidth(listsMinW);
            listsPane.setMaxWidth(listsMaxW);
        } else {
            listsPane.setPrefWidth(0);
            listsPane.setMinWidth(0);
            listsPane.setMaxWidth(0);
        }

        // SplitPane Divider und CSS-Klasse setzen
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

        // Header-Elemente umschalten
        if (tasksTitleLabel != null) {
            tasksTitleLabel.setVisible(!compactMode);
            tasksTitleLabel.setManaged(!compactMode);
        }
        if (btnListMenu != null) {
            btnListMenu.setVisible(compactMode);
            btnListMenu.setManaged(compactMode);
        }
    }

    /**
     * Aktualisiert die Header-Texte abhängig von der aktuell selektierten
     * Kategorie.
     *
     * Parameter:
     * - selected wird aktuell nicht verwendet (nur titleText).
     * Das ist ok, aber redundant.
     *
     * Wirkung:
     * - In Normal Mode: tasksTitleLabel zeigt titleText
     * - In Compact Mode: btnListMenu zeigt titleText
     *
     * @param selected  aktuell selektierte Kategorie (derzeit ungenutzt)
     * @param titleText Text für Header/Menu
     */
    public void updateHeaderTexts(Category selected, String titleText) {
        if (tasksTitleLabel != null)
            tasksTitleLabel.setText(titleText);
        if (btnListMenu != null)
            btnListMenu.setText(titleText);
    }
}
