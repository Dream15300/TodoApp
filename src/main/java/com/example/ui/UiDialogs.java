package com.example.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Zentrale Hilfsklasse für einfache Standard-Dialoge (JavaFX Alert).
 *
 * Ziel:
 * - Einheitliches Error- und Confirm-Dialog-Verhalten
 * - Vermeidung von dupliziertem Alert-Code in Controllern
 *
 * Einschränkung:
 * - Blockierend (showAndWait)
 * - Muss auf dem JavaFX Application Thread aufgerufen werden
 */
public final class UiDialogs {

    // Utility-Klasse: keine Instanzen
    private UiDialogs() {
    }

    /**
     * Zeigt einen Fehlerdialog an.
     *
     * Verhalten:
     * - Gibt Exception (falls vorhanden) auf stderr aus (printStackTrace)
     * - Öffnet modalen Error-Alert mit Message
     *
     * UI:
     * - Titel: "Fehler"
     * - Kein Header
     *
     * @param msg       Fehlermeldung für den Benutzer
     * @param exception Exception zur Diagnose (kann null sein)
     */
    public static void error(String msg, Exception exception) {
        if (exception != null)
            exception.printStackTrace();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Fehler");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * Zeigt einen Bestätigungsdialog (OK / Abbrechen) an.
     *
     * Rückgabewert:
     * - true: Benutzer hat OK gewählt
     * - false: Abbrechen, Schliessen oder kein Resultat
     *
     * UI:
     * - Titel: frei wählbar
     * - Kein Header
     *
     * @param title   Titel des Dialogs
     * @param content Inhaltstext
     * @return true bei Bestätigung, sonst false
     */
    public static boolean confirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        Optional<ButtonType> res = alert.showAndWait();
        return res.isPresent() && res.get() == ButtonType.OK;
    }
}
