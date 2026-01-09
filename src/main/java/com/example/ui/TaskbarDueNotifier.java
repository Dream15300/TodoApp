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
 */
public final class TaskbarDueNotifier {

    private final TodoService service;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "due-notifier");
        t.setDaemon(true);
        return t;
    });

    private TrayIcon trayIcon;
    private LocalDate lastNotifiedDay;

    public TaskbarDueNotifier(TodoService service) {
        this.service = service;
    }

    public void start() {
        setupTrayIfSupported();

        // Sofort prüfen, dann alle 5 Minuten
        scheduler.scheduleAtFixedRate(this::checkAndNotify, 0, 5, TimeUnit.MINUTES);
    }

    public void stop() {
        scheduler.shutdownNow();
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        clearMacBadge();
    }

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

        // Wenn heute nichts fällig: Reset, damit bei spaeterem Hinzufuegen am selben
        // Tag wieder notifiziert wird
        if (!hasDue) {
            lastNotifiedDay = null;
        }
    }

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

    private Image createSimpleTrayImage() {
        // neutrales Standard-Icon (16x16), ohne externe Ressourcen
        int s = 16;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.fillRect(3, 3, 10, 10);
        g.dispose();
        return img;
    }

    private void setMacBadge(String badgeTextOrNull) {
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar tb = Taskbar.getTaskbar();
                // setIconBadge ist auf macOS relevant; auf anderen OS kann es
                // ignoriert/unsupported sein
                tb.setIconBadge(badgeTextOrNull);
            }
        } catch (Exception ignored) {
        }
    }

    private void clearMacBadge() {
        setMacBadge(null);
    }
}
