package com.example.persistence;

import com.example.domain.TodoItem;
import com.example.domain.TodoStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Verantwortlichkeiten:
 * - Abfragen (Read) nach Kategorie/Status/Datum
 * - Einfügen (Create)
 * - Aktualisieren (Update) von Status und Feldern
 * - Löschen (Delete) bestimmter Datensätze
 *
 * Technische Hinweise:
 * - try-with-resources schliesst JDBC-Ressourcen deterministisch.
 * - PreparedStatements verhindern SQL-Injection und übernehmen
 * Typ-Konvertierung.
 */
public class TodoRepository {

    /**
     * Prüft, ob eine Kategorie mindestens ein Todo besitzt.
     *
     * SQL-Optimierung:
     * - SELECT 1 + LIMIT 1: minimale Datenmenge, schneller als COUNT(*)
     *
     * @param categoryId Kategorie-ID
     * @return true, falls mindestens ein Datensatz existiert
     */
    public boolean hasTodos(int categoryId) {
        String sql = "SELECT 1 FROM TodoItems WHERE CategoryId = ? LIMIT 1";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, categoryId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next(); // true, sobald ein Datensatz vorhanden ist
            }

        } catch (Exception exception) {
            throw new RuntimeException("Todo-Existenz prüfen fehlgeschlagen", exception);
        }
    }

    /**
     * Zählt Todos einer Kategorie nach Status.
     *
     * @param categoryId Kategorie-ID
     * @param status     TodoStatus (OPEN/DONE)
     * @return Anzahl Datensätze
     */
    public int countByCategoryAndStatus(int categoryId, TodoStatus status) {
        String sql = "SELECT COUNT(*) FROM TodoItems WHERE CategoryId = ? AND Status = ?";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, categoryId);
            preparedStatement.setInt(2, status.getDbValue()); // Enum → DB-Integer

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1); // COUNT(*) liefert genau eine Zeile
                }
                return 0; // defensiver Fallback
            }

        } catch (Exception exception) {
            throw new RuntimeException("Todo-Count laden fehlgeschlagen", exception);
        }
    }

    /**
     * Lädt offene Todos einer Kategorie.
     *
     * Sortierung:
     * - Priority DESC: hohe Priorität zuerst
     * - DueDate IS NULL: ohne Datum ans Ende (SQLite: false < true, daher NULLs
     * zuletzt)
     * - DueDate: frühestes Datum zuerst
     * - Id: stabile Reihenfolge
     */
    public List<TodoItem> findOpenByCategory(int categoryId) {
        String sql = """
                SELECT Id, CategoryId, Title, DueDate, Notes, Status, Priority
                FROM TodoItems
                WHERE CategoryId = ? AND Status = ?
                ORDER BY Priority DESC, DueDate IS NULL, DueDate, Id
                """;

        return queryByCategoryAndStatus(sql, categoryId, TodoStatus.OPEN);
    }

    /**
     * Lädt erledigte Todos einer Kategorie.
     *
     * Sortierung:
     * - DueDate IS NULL: ohne Datum ans Ende
     * - DueDate DESC: spätestes Datum zuerst
     * - Id DESC: neuere Einträge (höhere ID) zuerst
     */
    public List<TodoItem> findDoneByCategory(int categoryId) {
        String sql = """
                SELECT Id, CategoryId, Title, DueDate, Notes, Status, Priority
                FROM TodoItems
                WHERE CategoryId = ? AND Status = ?
                ORDER BY DueDate IS NULL, DueDate DESC, Id DESC
                """;

        return queryByCategoryAndStatus(sql, categoryId, TodoStatus.DONE);
    }

    /**
     * Gemeinsame Query-Logik für (Kategorie + Status) Abfragen.
     *
     * Vorteil:
     * - Vermeidet Code-Duplikation
     * - Einheitliches Mapping und Fehlerhandling
     */
    private List<TodoItem> queryByCategoryAndStatus(String sql, int categoryId, TodoStatus status) {
        List<TodoItem> output = new ArrayList<>();

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, categoryId);
            preparedStatement.setInt(2, status.getDbValue());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    output.add(map(resultSet)); // zentrale Row→Objekt Abbildung
                }
            }

            return output;

        } catch (Exception exception) {
            throw new RuntimeException("Todos laden fehlgeschlagen", exception);
        }
    }

    /**
     * Fügt ein neues Todo ein und liefert die generierte ID zurück.
     *
     * Hinweis zum Datentyp:
     * - DueDate wird als String (yyyy-MM-dd) gespeichert.
     *
     * @param item TodoItem (ohne ID oder mit Dummy-ID)
     * @return generierte ID
     */
    public int insert(TodoItem item) {
        String sql = """
                INSERT INTO TodoItems (CategoryId, Title, DueDate, Notes, Status, Priority)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setInt(1, item.getCategoryId());
            preparedStatement.setString(2, item.getTitle());

            // Null-handling: NULL in DB, falls kein Datum vorhanden
            preparedStatement.setString(3, item.getDueDate() == null ? null : item.getDueDate().toString());

            preparedStatement.setString(4, item.getNotes());
            preparedStatement.setInt(5, item.getStatus().getDbValue());
            preparedStatement.setInt(6, item.getPriority());

            preparedStatement.executeUpdate();

            // Generated Keys lesen (Primary Key)
            try (ResultSet keys = preparedStatement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }

            // Wenn keine ID geliefert wird, ist das ein technischer Fehlerzustand
            throw new RuntimeException("Keine ID zurückgegeben");

        } catch (Exception exception) {
            throw new RuntimeException("Todo einfügen fehlgeschlagen", exception);
        }
    }

    /**
     * Aktualisiert nur den Status eines Todos.
     *
     * Validierung:
     * - Wenn affected == 0: kein Datensatz mit dieser ID (z. B. bereits gelöscht)
     *
     * @param todoId ID des Todos
     * @param status neuer Status
     */
    public void updateStatus(int todoId, TodoStatus status) {
        String sql = "UPDATE TodoItems SET Status = ? WHERE Id = ?";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, status.getDbValue());
            preparedStatement.setInt(2, todoId);

            int affected = preparedStatement.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Todo nicht gefunden: Id=" + todoId);
            }

        } catch (Exception exception) {
            throw new RuntimeException("Todo-Status aktualisieren fehlgeschlagen", exception);
        }
    }

    /**
     * Aktualisiert Titel, DueDate und Notes eines Todos.
     *
     * Null/Blank-Strategie:
     * - dueDate == null → NULL in DB
     * - notes == null oder blank → NULL in DB (nicht leerer String)
     *
     * Typ-Hinweis:
     * - setNull(…, Types.VARCHAR) passt zur Speicherung als TEXT/VARCHAR.
     * - Wenn DueDate als DATE gespeichert wäre, müsste Types.DATE verwendet werden.
     */
    public void updateTodo(int todoId, String title, LocalDate dueDate, String notes) {
        String sql = "UPDATE TodoItems SET Title = ?, DueDate = ?, Notes = ? WHERE Id = ?";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, title);

            if (dueDate == null) {
                preparedStatement.setNull(2, Types.VARCHAR);
            } else {
                preparedStatement.setString(2, dueDate.toString());
            }

            if (notes == null || notes.isBlank()) {
                preparedStatement.setNull(3, Types.VARCHAR);
            } else {
                preparedStatement.setString(3, notes);
            }

            preparedStatement.setInt(4, todoId);

            int affected = preparedStatement.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Todo nicht gefunden: Id=" + todoId);
            }

        } catch (Exception exception) {
            throw new RuntimeException("Todo aktualisieren fehlgeschlagen", exception);
        }
    }

    /**
     * Löscht alle erledigten Todos einer Kategorie.
     *
     * Hinweis:
     * - Diese Operation ist "bulk delete" ohne Rückgabe der Anzahl.
     * - Optional könnte man affected rows zurückgeben (executeUpdate() Ergebnis).
     */
    public void deleteDoneByCategory(int categoryId) {
        String sql = """
                DELETE FROM TodoItems
                WHERE Status = ? AND CategoryId = ?
                """;

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, TodoStatus.DONE.getDbValue());
            preparedStatement.setInt(2, categoryId);

            preparedStatement.executeUpdate();

        } catch (Exception exception) {
            throw new RuntimeException("Erledigte Todos löschen fehlgeschlagen", exception);
        }
    }

    /**
     * Mapping-Funktion: ResultSet-Zeile → TodoItem.
     *
     * Konvertierungen:
     * - DueDate: String → LocalDate (oder null)
     * - Status: int → TodoStatus (über fromDbValue)
     *
     * @param resultSet aktuelles ResultSet (steht bereits auf einer Zeile)
     * @return TodoItem Domain-Objekt
     */
    private TodoItem map(ResultSet resultSet) throws Exception {
        int id = resultSet.getInt("Id");
        int catId = resultSet.getInt("CategoryId");
        String title = resultSet.getString("Title");
        String due = resultSet.getString("DueDate");
        String notes = resultSet.getString("Notes");
        int statusValue = resultSet.getInt("Status");
        int priority = resultSet.getInt("Priority");

        LocalDate dueDate = (due == null || due.isBlank()) ? null : LocalDate.parse(due);
        TodoStatus todoStatus = TodoStatus.fromDbValue(statusValue);

        return new TodoItem(id, catId, title, dueDate, notes, todoStatus, priority);
    }

    /**
     * Zählt Todos nach Fälligkeitsdatum und Status.
     *
     * Einschränkung:
     * - DueDate muss exakt übereinstimmen (keine Bereichsabfrage).
     * - dueDate darf hier nicht null sein, sonst NullPointerException bei
     * toString().
     *
     * @param dueDate Datum (LocalDate)
     * @param status  Status
     * @return Anzahl Datensätze
     */
    public int countByDueDateAndStatus(LocalDate dueDate, TodoStatus status) {
        String sql = "SELECT COUNT(*) FROM TodoItems WHERE DueDate = ? AND Status = ?";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, dueDate.toString()); // yyyy-MM-dd
            preparedStatement.setInt(2, status.getDbValue());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return 0;
            }

        } catch (Exception exception) {
            throw new RuntimeException("Todo-Count (DueDate/Status) laden fehlgeschlagen", exception);
        }
    }

}
