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

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, categoryId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // true, sobald ein Datensatz vorhanden ist
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

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, categoryId);
            ps.setInt(2, status.getDbValue()); // Enum → DB-Integer

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1); // COUNT(*) liefert genau eine Zeile
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
     * - DueDate IS NULL: ohne Datum ans Ende (SQLite: false < true, daher NULLs
     * zuletzt)
     * - DueDate: frühestes Datum zuerst
     * - Id: stabile Reihenfolge
     */
    public List<TodoItem> findOpenByCategory(int categoryId) {
        String sql = """
                SELECT Id, CategoryId, Title, DueDate, Notes, Status
                FROM TodoItems
                WHERE CategoryId = ? AND Status = ?
                ORDER BY DueDate IS NULL, DueDate, Id
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
                SELECT Id, CategoryId, Title, DueDate, Notes, Status
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

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, categoryId);
            ps.setInt(2, status.getDbValue());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    output.add(map(rs)); // zentrale Row→Objekt Abbildung
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
                INSERT INTO TodoItems (CategoryId, Title, DueDate, Notes, Status)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, item.getCategoryId());
            ps.setString(2, item.getTitle());

            // Null-handling: NULL in DB, falls kein Datum vorhanden
            ps.setString(3, item.getDueDate() == null ? null : item.getDueDate().toString());

            ps.setString(4, item.getNotes());
            ps.setInt(5, item.getStatus().getDbValue());

            ps.executeUpdate();

            // Generated Keys lesen (Primary Key)
            try (ResultSet keys = ps.getGeneratedKeys()) {
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

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, status.getDbValue());
            ps.setInt(2, todoId);

            int affected = ps.executeUpdate();
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

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, title);

            if (dueDate == null) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, dueDate.toString());
            }

            if (notes == null || notes.isBlank()) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, notes);
            }

            ps.setInt(4, todoId);

            int affected = ps.executeUpdate();
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

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, TodoStatus.DONE.getDbValue());
            ps.setInt(2, categoryId);

            ps.executeUpdate();

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
     * @param rs aktuelles ResultSet (steht bereits auf einer Zeile)
     * @return TodoItem Domain-Objekt
     */
    private TodoItem map(ResultSet rs) throws Exception {
        int id = rs.getInt("Id");
        int catId = rs.getInt("CategoryId");
        String title = rs.getString("Title");
        String due = rs.getString("DueDate");
        String notes = rs.getString("Notes");
        int statusValue = rs.getInt("Status");

        LocalDate dueDate = (due == null || due.isBlank()) ? null : LocalDate.parse(due);
        TodoStatus todoStatus = TodoStatus.fromDbValue(statusValue);

        return new TodoItem(id, catId, title, dueDate, notes, todoStatus);
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

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, dueDate.toString()); // yyyy-MM-dd
            ps.setInt(2, status.getDbValue());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }

        } catch (Exception exception) {
            throw new RuntimeException("Todo-Count (DueDate/Status) laden fehlgeschlagen", exception);
        }
    }

}
