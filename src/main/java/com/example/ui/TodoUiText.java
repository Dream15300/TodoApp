package com.example.ui;

/**
 * Hilfsklasse für UI-spezifische Textmanipulationen.
 *
 * Zweck:
 * - Verhindert Layout-Probleme bei sehr langen Strings ohne Leerzeichen
 * (z. B. lange IDs, URLs, durchgehende Wörter).
 *
 * Kontext:
 * - Wird z. B. in TasksController bei der Anzeige von Todo-Titeln verwendet.
 * - Relevant für JavaFX Labels mit wrapText=true.
 */
public final class TodoUiText {

    // Utility-Klasse: keine Instanzen erlaubt
    private TodoUiText() {
    }

    /**
     * Erzwingt Zeilenumbrüche an beliebigen Stellen.
     *
     * Implementierung:
     * - Fügt zwischen jedes Zeichen ein Zero-Width Space (U+200B) ein.
     * - Zero-Width Space ist unsichtbar, erlaubt aber Umbruch an dieser Stelle.
     *
     * Auswirkungen:
     * - JavaFX kann Text an jeder Position umbrechen
     * - Optisch unverändert, solange kein Umbruch nötig ist
     *
     * Performance:
     * - O(n) pro Aufruf
     * - Für UI-Texte mit begrenzter Länge unkritisch
     *
     * Einschränkungen:
     * - Cursor-/Textselektion (falls editierbar) würde dadurch verfälscht
     * - Daher nur für Anzeige-Texte (Label), nicht für Editoren
     * (TextField/TextArea)
     *
     * @param s Eingabetext (kann null sein)
     * @return Text mit eingefügten Zero-Width-Spaces oder leerer String bei null
     */
    public static String breakAnywhere(String s) {
        if (s == null)
            return "";

        // "" als Regex trennt zwischen jedem Zeichen → Insert von U+200B
        return s.replace("", "\u200B");
    }
}
