package com.example.persistence;

import com.example.domain.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Aufgabe:
 * - Kapselt sämtliche CRUD-Operationen (Create, Read, Update, Delete)
 * - Trennt Persistenzlogik klar von UI- und Domain-Schicht
 *
 * Entwurfsmuster:
 * - Repository Pattern
 * → Zentrale Stelle für Datenzugriffe
 * → Erleichtert Wartung, Tests und Austausch der Datenquelle
 */
public class CategoryRepository {

    /**
     * Lädt alle Kategorien aus der Datenbank.
     *
     * SQL:
     * - Selektiert Id, Name und Icon
     * - Sortierung nach Name für stabile, benutzerfreundliche Anzeige
     *
     * @return Liste aller Kategorien als Domain-Objekte
     */
    public List<Category> findAll() {
        String sql = "SELECT Id, Name, Icon FROM Categories ORDER BY Name";

        // Ergebnisliste, die schrittweise aus dem ResultSet aufgebaut wird
        List<Category> outputedList = new ArrayList<>();

        /*
         * try-with-resources:
         * - Connection, PreparedStatement und ResultSet werden automatisch geschlossen
         * - Verhindert Resource-Leaks
         */
        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ResultSet resultSet = preparedStatement.executeQuery()) {

            // Iteration über alle Datensätze des ResultSets
            while (resultSet.next()) {

                /*
                 * Mapping:
                 * - Relationale Daten (Zeile) → Domain-Objekt (Category)
                 * - Spaltennamen entsprechen den Attributen der Tabelle
                 */
                outputedList.add(new Category(
                        resultSet.getInt("Id"),
                        resultSet.getString("Name"),
                        resultSet.getString("Icon")));
            }

            return outputedList;

        } catch (Exception exception) {
            /*
             * Fehlerbehandlung:
             * - Fängt alle Exceptions ab (inkl. SQLException)
             * - Kapselt technische Fehler in RuntimeException
             * - Vereinfachung für Aufrufer (keine Checked Exceptions)
             */
            throw new RuntimeException("Kategorien laden fehlgeschlagen", exception);
        }
    }

    /**
     * Fügt eine neue Kategorie in die Datenbank ein.
     *
     * @param name Name der Kategorie
     * @param icon Icon (z. B. Unicode-Zeichen oder String)
     * @return Generierte Datenbank-ID der neuen Kategorie, oder -1 bei Fehler
     */
    public int insert(String name, String icon) {
        String sql = "INSERT INTO Categories (Name, Icon) VALUES (?, ?)";

        /*
         * Statement.RETURN_GENERATED_KEYS:
         * - Ermöglicht den Zugriff auf automatisch generierte IDs (Primary Key)
         */
        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {

            // Platzhalter werden sicher befüllt (Schutz vor SQL-Injection)
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, icon);

            // Führt INSERT aus, Rückgabewert (int) wird hier nicht benötigt
            preparedStatement.executeUpdate();

            /*
             * Lesen des generierten Primärschlüssels
             * - ResultSet enthält die neu erzeugte ID
             */
            try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Kategorie konnte nicht eingefügt werden", exception);
        }

        // Fallback, falls keine ID generiert wurde
        return -1;
    }

    /**
     * Aktualisiert den Namen einer bestehenden Kategorie.
     *
     * @param id      ID der Kategorie
     * @param newName Neuer Name
     */
    public void updateName(int id, String newName) {
        String sql = "UPDATE Categories SET Name = ? WHERE Id = ?";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, newName);
            preparedStatement.setInt(2, id);

            // Führt UPDATE-Anweisung aus
            preparedStatement.executeUpdate();

        } catch (SQLException exception) {
            throw new RuntimeException("Kategorie umbenennen fehlgeschlagen", exception);
        }
    }

    /**
     * Aktualisiert das Icon einer bestehenden Kategorie.
     *
     * @param id      ID der Kategorie
     * @param newIcon Neues Icon
     */
    public void updateIcon(int id, String newIcon) {
        String sql = "UPDATE Categories SET Icon = ? WHERE Id = ?";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, newIcon);
            preparedStatement.setInt(2, id);
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Kategorie Icon ändern fehlgeschlagen", exception);
        }
    }

    /**
     * Löscht eine Kategorie anhand ihrer ID.
     *
     * @param id ID der zu löschenden Kategorie
     */
    public void delete(int id) {
        String sql = "DELETE FROM Categories WHERE Id = ?";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);

            // Führt DELETE aus
            preparedStatement.executeUpdate();

        } catch (SQLException exception) {
            throw new RuntimeException("Kategorie löschen fehlgeschlagen", exception);
        }
    }
}
