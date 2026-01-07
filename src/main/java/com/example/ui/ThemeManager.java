package com.example.ui;

import javafx.scene.Scene;
import java.util.prefs.Preferences;

public final class ThemeManager {

    private ThemeManager() {
    }

    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String KEY_THEME = "ui.theme"; // "LIGHT" | "DIM"

    public enum Theme {
        LIGHT("theme-light.css"),
        DIM("theme-dim.css");

        final String css;

        Theme(String css) {
            this.css = css;
        }
    }

    public static Theme loadThemeOrDefault() {
        String v = PREFS.get(KEY_THEME, Theme.DIM.name());
        try {
            return Theme.valueOf(v);
        } catch (Exception ex) {
            return Theme.DIM;
        }
    }

    public static void saveTheme(Theme theme) {
        if (theme != null) {
            PREFS.put(KEY_THEME, theme.name());
        }
    }

    public static void apply(Scene scene, Theme theme) {
        if (scene == null || theme == null)
            return;

        String base = com.example.App.class.getResource("/com/example/style.css").toExternalForm();
        String light = com.example.App.class.getResource("/com/example/theme-light.css").toExternalForm();
        String dim = com.example.App.class.getResource("/com/example/theme-dim.css").toExternalForm();

        // base sicherstellen (optional, falls du base via FXML einbindest)
        if (!scene.getStylesheets().contains(base)) {
            scene.getStylesheets().add(0, base);
        }

        // alte Themes entfernen
        scene.getStylesheets()
                .removeIf(s -> s.contains("/com/example/theme-light.css") || s.contains("/com/example/theme-dim.css"));

        // Theme setzen
        scene.getStylesheets().add(theme == Theme.LIGHT ? light : dim);

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
