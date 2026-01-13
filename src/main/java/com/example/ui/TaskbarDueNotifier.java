package com.example.ui;

import com.example.service.TodoService;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Plattformverhalten:
 * - macOS: Dock Badge via java.awt.Taskbar#setIconBadge(...)
 * - Windows/Linux: SystemTray Icon + Tooltip + Balloon-Notification
 *
 * Designentscheidungen:
 * - Scheduler läuft in einem Daemon-Thread (blockiert App-Shutdown nicht).
 * - Tray ist optional: Fehler beim Setup werden geschluckt, App läuft weiter.
 * - Benachrichtigung (Balloon) maximal 1x pro Tag, solange fällige Todos
 * existieren.
 */
public final class TaskbarDueNotifier {

    private final TodoService service;

    /*
     * ScheduledExecutorService:
     * - periodisches Polling (alle 5 Minuten)
     * - Single-Thread reicht, da checkAndNotify kurz ist
     * - Daemon-Thread: verhindert "hängt beim Beenden", falls stop() nicht
     * aufgerufen wurde
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "due-notifier");
        t.setDaemon(true);
        return t;
    });

    /*
     * TrayIcon:
     * - nur gesetzt, wenn SystemTray unterstützt und Setup erfolgreich ist
     * - auf macOS kann SystemTray existieren, Dock Badge ist aber separat über
     * Taskbar
     */
    private TrayIcon trayIcon;

    /*
     * Merkt, an welchem Datum zuletzt eine Balloon-Notification gesendet wurde.
     * Zweck:
     * - "nur 1x pro Tag" implementieren.
     */
    private LocalDate lastNotifiedDay;

    /**
     * @param service TodoService, liefert Count der heute fälligen offenen Todos.
     */
    public TaskbarDueNotifier(TodoService service) {
        this.service = service;
    }

    /**
     * Startet den Notifier.
     *
     * Ablauf:
     * - Tray versuchen zu initialisieren (optional)
     * - Sofort prüfen (initial delay = 0), danach alle 5 Minuten
     *
     * Hinweis:
     * - scheduleAtFixedRate kann zu Overlap führen, falls checkAndNotify sehr lange
     * dauert.
     * Hier ist es kurz (DB-Count), daher ok.
     */
    public void start() {
        setupTrayIfSupported();

        // Sofort prüfen, dann alle 5 Minuten
        scheduler.scheduleAtFixedRate(this::checkAndNotify, 0, 5, TimeUnit.MINUTES);
    }

    /**
     * Stoppt den Notifier.
     *
     * Ablauf:
     * - Scheduler beenden (interrupt)
     * - TrayIcon entfernen (falls vorhanden)
     * - macOS Badge löschen
     */
    public void stop() {
        scheduler.shutdownNow();
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        clearMacBadge();
    }

    /**
     * Kernlogik:
     * - Count offene Todos mit DueDate == heute
     * - Badge/Tooltip aktualisieren
     * - Balloon-Notification max 1x pro Tag, solange fällige existieren
     */
    private void checkAndNotify() {
        int dueToday = service.countDueTodayOpen();
        boolean hasDue = dueToday > 0;

        // macOS Dock Badge
        setMacBadge(hasDue ? String.valueOf(dueToday) : null);

        // Tray Tooltip
        if (trayIcon != null) {
            trayIcon.setToolTip(hasDue
                    ? "Heute fällig: " + dueToday
                    : "Keine fälligen Todos heute");
        }

        // Balloon nur 1x pro Tag, wenn fällige existieren
        LocalDate today = LocalDate.now();
        if (hasDue && (lastNotifiedDay == null || !lastNotifiedDay.equals(today))) {
            lastNotifiedDay = today;

            if (trayIcon != null) {
                int count = dueToday;

                String message = (count == 1)
                        ? "Heute ist 1 Todo fällig."
                        : "Heute sind " + count + " Todos fällig.";

                trayIcon.displayMessage(
                        "TodoApp",
                        message,
                        TrayIcon.MessageType.WARNING);
            }
        }

        /*
         * Wenn heute nichts fällig:
         * - lastNotifiedDay resetten, damit bei späterem Hinzufügen am selben Tag
         * wieder notifiziert wird
         *
         * Hinweis:
         * - "späteres Hinzufügen" wird durch Polling erkannt (nächster 5-Minuten-Tick).
         */
        if (!hasDue) {
            lastNotifiedDay = null;
        }
    }

    /**
     * Initialisiert SystemTray (Windows/Linux typischer Use-Case).
     *
     * Verhalten:
     * - wenn SystemTray nicht unterstützt: return
     * - bei Fehlern: trayIcon=null (App läuft weiter)
     *
     * PopupMenu:
     * - "Beenden" ruft System.exit(0) (hartes Beenden)
     */
    private void setupTrayIfSupported() {
        if (!SystemTray.isSupported()) {
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = createSimpleTrayImage();

            trayIcon = new TrayIcon(image, "TodoApp");
            trayIcon.setImageAutoSize(true);

            PopupMenu menu = new PopupMenu();
            MenuItem exit = new MenuItem("Beenden");
            exit.addActionListener(e -> System.exit(0));
            menu.add(exit);

            trayIcon.setPopupMenu(menu);
            tray.add(trayIcon);

        } catch (Exception exception) {
            // Tray optional, App muss weiterlaufen
            trayIcon = null;
        }
    }

    /**
     * Erzeugt ein minimales Tray-Icon ohne externe Ressourcen.
     *
     * Implementation:
     * - 16x16 ARGB BufferedImage
     * - gefülltes Rechteck (3..12)
     */
    private Image createSimpleTrayImage() {
        // neutrales Standard-Icon (16x16), ohne externe Ressourcen
        int s = 16;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.fillRect(3, 3, 10, 10);
        g.dispose();
        return img;
    }

    /**
     * Setzt macOS Dock Badge (wenn unterstützt).
     *
     * @param badgeTextOrNull Text (z. B. "3") oder null zum Entfernen
     */
    private void setMacBadge(String badgeTextOrNull) {
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar tb = Taskbar.getTaskbar();

                // setIconBadge ist auf macOS relevant; auf anderen OS kann es
                // ignoriert/unsupported sein
                tb.setIconBadge(badgeTextOrNull);
            }
        } catch (Exception ignored) {
            // bewusst ignoriert: Badge ist optional, keine App-Funktionalität
        }
    }

    /**
     * Entfernt das macOS Dock Badge.
     */
    private void clearMacBadge() {
        setMacBadge(null);
    }
}
