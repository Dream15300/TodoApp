package com.example.ui.controller;

import com.example.domain.TodoItem;
import com.example.service.TodoService;
import com.example.ui.UiDialogs;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

/**
 * Verantwortlichkeiten:
 * - Öffnen/Schliessen des Detail-Panels (Visibility/Managed)
 * - Befüllen der Felder mit Daten des selektierten TodoItem
 * - Speichern von Änderungen über TodoService
 *
 * UI-Pattern:
 * - detailsPane.setManaged(false) entfernt das Panel aus dem Layoutfluss (kein
 * Leerraum),
 * setVisible(false) blendet es aus. Beide zusammen sind üblich.
 */
public class DetailsController {

    // Container des Detailbereichs (wird ein-/ausgeblendet)
    private final VBox detailsPane;

    // UI-Felder des Detailbereichs
    private final TextField detailsTitle;
    private final DatePicker detailsDueDate;
    private final TextArea detailsNotes;

    // Service-Schicht für Persistenz/Businesslogik
    private final TodoService service;

    /*
     * Aktuell geöffnetes TodoItem (State des Controllers).
     * Hinweis:
     * - Wird beim open(...) gesetzt, beim close() wieder gelöscht.
     * - save() arbeitet auf dieser Referenz.
     */
    private TodoItem detailsItem;

    /**
     * Konstruktor mit UI-Referenzen und Service.
     *
     * Hinweis:
     * - UI-Nodes werden injiziert (z. B. aus PrimaryController/FXML-Setup),
     * damit dieser Controller testbarer und unabhängiger von FXML bleibt.
     */
    public DetailsController(VBox detailsPane, TextField detailsTitle, DatePicker detailsDueDate, TextArea detailsNotes,
            TodoService service) {
        this.detailsPane = detailsPane;
        this.detailsTitle = detailsTitle;
        this.detailsDueDate = detailsDueDate;
        this.detailsNotes = detailsNotes;
        this.service = service;
    }

    /**
     * Öffnet den Detailbereich für ein TodoItem und befüllt die Felder.
     *
     * Null-Handling:
     * - Notes kann null sein → UI bekommt dann "" (leer)
     *
     * Layout:
     * - managed=true und visible=true: Panel wird angezeigt und nimmt Platz im
     * Layout ein
     *
     * @param item TodoItem, das angezeigt/bearbeitet werden soll
     */
    public void open(TodoItem item) {
        detailsItem = item;

        detailsTitle.setText(item.getTitle());
        detailsDueDate.setValue(item.getDueDate());
        detailsNotes.setText(item.getNotes() == null ? "" : item.getNotes());

        detailsPane.setManaged(true);
        detailsPane.setVisible(true);
    }

    /**
     * Schliesst den Detailbereich.
     *
     * State:
     * - detailsItem wird auf null gesetzt, damit save() keine Änderungen mehr
     * ausführt
     *
     * Layout:
     * - visible=false blendet aus
     * - managed=false entfernt aus Layoutfluss (kein leerer Bereich)
     */
    public void close() {
        detailsItem = null;
        detailsPane.setVisible(false);
        detailsPane.setManaged(false);
    }

    /**
     * @return true, wenn der Detailbereich aktuell sichtbar ist.
     *
     *         Hinweis:
     *         - Das prüft nur Visible-Flag. managed kann separat abweichen (hier
     *         aber konsistent gesetzt).
     */
    public boolean isOpen() {
        return detailsPane.isVisible();
    }

    /**
     * Setzt das Fälligkeitsdatum im UI zurück (löscht Datumsauswahl).
     *
     * Hinweis:
     * - Das wirkt nur im UI; persistiert wird erst mit save().
     */
    public void clearDueDate() {
        detailsDueDate.setValue(null);
    }

    /**
     * Speichert die Änderungen über den Service.
     *
     * Rückgabe:
     * - true: Update erfolgreich (Caller kann danach z. B. Listen refreshen)
     * - false: kein Update (kein Item offen, Titel leer, oder Exception)
     *
     * Validierung:
     * - Titel darf nicht leer sein (trim → empty)
     */
    public boolean save() {
        if (detailsItem == null)
            return false;

        String newTitle = detailsTitle.getText() == null ? "" : detailsTitle.getText().trim();
        if (newTitle.isEmpty())
            return false;

        LocalDate newDue = detailsDueDate.getValue();
        String notes = detailsNotes.getText();

        try {
            service.updateTodo(detailsItem.getId(), newTitle, newDue, notes);
            return true;
        } catch (Exception exception) {
            UiDialogs.error("Aufgabe konnte nicht aktualisiert werden: " + exception.getMessage(), exception);
            return false;
        }
    }
}
