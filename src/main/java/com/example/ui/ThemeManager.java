package com.example.ui;

import javafx.scene.Scene;
import java.util.prefs.Preferences;

public final class ThemeManager {

    private ThemeManager() {
    }

    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);

    private static final String KEY_THEME = "ui.theme";

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

        String getCssPath() {
            return "/com/example/css/" + cssFile;
        }
    }

    /*
     * =========================
     * Load / Save
     * =========================
     */

    public static Theme loadThemeOrDefault() {
        String value = PREFS.get(KEY_THEME, Theme.DIM.name());
        try {
            return Theme.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return Theme.DIM;
        }
    }

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

    public static void apply(Scene scene, Theme theme) {
        if (scene == null || theme == null) {
            return;
        }

        String baseCss = com.example.App.class
                .getResource("/com/example/css/style.css")
                .toExternalForm();

        // Base-CSS sicherstellen (Layout, Komponenten)
        if (!scene.getStylesheets().contains(baseCss)) {
            scene.getStylesheets().add(0, baseCss);
        }

        // Alle Theme-CSS entfernen
        scene.getStylesheets().removeIf(s -> s.contains("/com/example/css/theme-"));

        // Neues Theme hinzufuegen
        String themeCss = com.example.App.class
                .getResource(theme.getCssPath())
                .toExternalForm();

        scene.getStylesheets().add(themeCss);

        // Re-Apply erzwingen
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        System.out.println("Theme applied: " + theme);
        System.out.println(scene.getStylesheets());
    }

    public static void applySaved(Scene scene) {
        apply(scene, loadThemeOrDefault());
    }
}
