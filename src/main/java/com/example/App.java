package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

import com.example.ui.ThemeManager;
import com.example.service.TodoService;
import com.example.ui.TaskbarDueNotifier;

/**
 * Ablauf:
 * - init(): Datenbank initialisieren (Schema/Seed)
 * - start(): UI laden, Styles/Themes anwenden, Stage konfigurieren, Notifier
 * starten
 */
public class App extends Application {

    // Hintergrund-Notifier (Tray/Dock Badge) wird beim App-Start gestartet und beim
    // Schliessen gestoppt
    private TaskbarDueNotifier dueNotifier;

    /**
     * Wird vor start(...) aufgerufen (nicht auf dem JavaFX Application Thread).
     *
     * Zweck:
     * - Datenbankinitialisierung (z. B. Tabellen erstellen, Defaults einfügen)
     */
    @Override
    public void init() {
        DatabaseInitializer.init();
    }

    /**
     * Startet die UI (läuft auf dem JavaFX Application Thread).
     *
     * Ablauf:
     * 1) FXML laden (primary.fxml) und Scene erzeugen
     * 2) Base-CSS hinzufügen (style.css)
     * 3) Gespeichertes Theme anwenden (ThemeManager.applySaved)
     * 4) Stage-Icon setzen
     * 5) Stage konfigurieren und anzeigen
     * 6) TaskbarDueNotifier starten
     * 7) OnCloseRequest: Notifier sauber stoppen
     *
     * @param stage Primary Stage
     * @throws IOException wenn FXML nicht geladen werden kann
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("primary.fxml"));

        // Scene mit initialer Grösse
        Scene scene = new Scene(loader.load(), 900, 600);

        /*
         * Base-CSS hinzufügen:
         * - Enthält Layout/Komponenten-Styles
         * - ThemeManager.applySaved(...) stellt zusätzlich sicher, dass Base-CSS
         * vorhanden ist
         */
        scene.getStylesheets().add(
                App.class.getResource("/com/example/css/style.css").toExternalForm());

        // Theme aus Preferences laden und anwenden (Theme-CSS wird hinzugefügt, alte
        // Theme-CSS entfernt)
        ThemeManager.applySaved(scene);

        /*
         * Stage Icon:
         */
        stage.getIcons().add(new Image(App.class.getResource("/com/example/Haken.png").toExternalForm()));

        // Stage konfigurieren
        stage.setTitle("To Do");
        stage.setScene(scene);
        stage.show();

        /*
         * Notifier:
         * - Erst nach show() gestartet (nicht zwingend nötig, aber ok)
         * - Verwendet hier eine neue TodoService-Instanz (zweite Instanz neben
         * PrimaryController)
         */
        TodoService service = new TodoService();
        dueNotifier = new TaskbarDueNotifier(service);

        // verzögert starten, aber nur wenn initialisiert
        javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(javafx.util.Duration.millis(800));
        pt.setOnFinished(ev -> {
            if (dueNotifier != null) {
                dueNotifier.start();
            }
        });
        pt.play();

        /*
         * Schliessen:
         * - stoppt Notifier, damit Scheduler/Tray sauber beendet werden
         */
        stage.setOnCloseRequest(e -> {
            if (dueNotifier != null) {
                dueNotifier.stop();
            }
        });
    }

    /**
     * Standard main().
     * launch(...) startet den JavaFX Application Lifecycle.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
