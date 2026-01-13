package com.example.ui;

import javafx.scene.Scene;
import java.util.prefs.Preferences;

/**
 * Konzepte:
 * - Preferences: persistente Speicherung pro User (OS-abhängiger Speicherort)
 * - Scene Stylesheets: Reihenfolge ist relevant (spätere Styles können frühere
 * überschreiben)
 * - Base-CSS (style.css): Grundlayout/Komponenten-Styles
 * - Theme-CSS (theme-*.css): Farb-/Theme-Overrides
 */
public final class ThemeManager {

    // Utility-Klasse: keine Instanzen
    private ThemeManager() {
    }

    /*
     * Preferences-Node: unter dem Package-Namen com.example.ui
     * - userNodeForPackage sorgt für "per-user" Persistenz.
     */
    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);

    // Key im Preferences-Store
    private static final String KEY_THEME = "ui.theme";

    /**
     * Verfügbare Themes.
     *
     * cssFile:
     * - Dateiname der Theme-CSS im Ressourcenpfad /com/example/css/
     */
    public enum Theme {
        LIGHT("theme-light.css"),
        DIM("theme-dim.css"),
        BLUE("theme-blue.css"),
        GREEN("theme-green.css"),
        PURPLE("theme-purple.css"),
        HIGH_CONTRAST("theme-high-contrast.css");

        final String cssFile;

        Theme(String cssFile) {
            this.cssFile = cssFile;
        }

        /**
         * @return Classpath-Pfad zur Theme-CSS.
         *
         *         Beispiel:
         *         - "/com/example/css/theme-dim.css"
         */
        String getCssPath() {
            return "/com/example/css/" + cssFile;
        }
    }

    /*
     * =========================
     * Load / Save
     * =========================
     */

    /**
     * Lädt das gespeicherte Theme oder liefert Default.
     *
     * Default:
     * - Theme.DIM, wenn kein Wert gespeichert oder ungültig.
     *
     * Fehlerfall:
     * - Wenn Preferences einen unbekannten String enthält, wirft valueOf(...)
     * IllegalArgumentException.
     * Dann wird auf DIM zurückgefallen.
     */
    public static Theme loadThemeOrDefault() {
        String value = PREFS.get(KEY_THEME, Theme.DIM.name());
        try {
            return Theme.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return Theme.DIM;
        }
    }

    /**
     * Speichert das Theme in Preferences.
     *
     * Hinweis:
     * - PREFS.put schreibt asynchron/OS-abhängig. Für garantierte Persistenz könnte
     * PREFS.flush()
     * genutzt werden (kann aber Exception werfen).
     */
    public static void saveTheme(Theme theme) {
        if (theme != null) {
            PREFS.put(KEY_THEME, theme.name());
        }
    }

    /*
     * =========================
     * Apply Theme
     * =========================
     */

    /**
     * Wendet Base-CSS + Theme-CSS auf eine Scene an.
     *
     * Ablauf:
     * 1) Base-CSS (style.css) sicherstellen (wenn nicht vorhanden → add(0))
     * 2) alle bisherigen theme- Stylesheets entfernen (Filter:
     * "/com/example/css/theme-")
     * 3) neues Theme-CSS hinzufügen
     * 4) applyCss/layout erzwingen (visuelle Aktualisierung)
     *
     * Voraussetzungen:
     * - Ressourcen müssen existieren, sonst liefert getResource(...) null und
     * toExternalForm() NPE.
     * (Hier wird das nicht abgefangen.)
     */
    public static void apply(Scene scene, Theme theme) {
        if (scene == null || theme == null) {
            return;
        }

        /*
         * Base-CSS:
         * - Wird über com.example.App.class geladen (nur als "Classloader-Anker").
         * - Vorteil: funktioniert zuverlässig, solange App.class im gleichen Modul/JAR
         * ist.
         */
        String baseCss = com.example.App.class
                .getResource("/com/example/css/style.css")
                .toExternalForm();

        // Base-CSS sicherstellen (Layout, Komponenten)
        if (!scene.getStylesheets().contains(baseCss)) {
            // Index 0: Base-CSS soll vor Theme-CSS kommen (Theme überschreibt Base)
            scene.getStylesheets().add(0, baseCss);
        }

        /*
         * Alle Theme-CSS entfernen:
         * - removeIf(...) entfernt alle Einträge, deren URL den Theme-Prefix enthält.
         * - Dadurch kann man Theme wechseln ohne "Stacking" mehrerer Themes.
         */
        scene.getStylesheets().removeIf(s -> s.contains("/com/example/css/theme-"));

        // Neues Theme hinzufügen
        String themeCss = com.example.App.class
                .getResource(theme.getCssPath())
                .toExternalForm();

        scene.getStylesheets().add(themeCss);

        /*
         * Re-Apply erzwingen:
         * - applyCss(): CSS neu anwenden
         * - layout(): Layout neu berechnen
         */
        scene.getRoot().applyCss();
        scene.getRoot().layout();
    }

    /**
     * Lädt gespeichertes Theme und wendet es an.
     */
    public static void applySaved(Scene scene) {
        apply(scene, loadThemeOrDefault());
    }
}
