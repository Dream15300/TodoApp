// verantworlich für Datenzugriff (Persistence Layer, kapselt)

package com.example.persistence;

import com.example.domain.TodoItem;
import com.example.domain.TodoStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TodoRepository {

    public List<TodoItem> findByCategory(int categoryId) {
        String sql = """
                SELECT Id, CategoryId, Title, DueDate, Status
                FROM TodoItems
                WHERE CategoryId = ?
                ORDER BY CASE WHEN Status = 1 THEN 1 ELSE 0 END, DueDate IS NULL, DueDate, Id
                """;

        List<TodoItem> out = new ArrayList<>(); // Liste für gemappte Todo-Objekte

        try (Connection c = Db.open(); // Db.open() --> siehe Db.java-Datei
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, categoryId); // setzt ersten Platzhalter (?) und bindet Kategorie-ID sicher ins SQL

            try (ResultSet rs = ps.executeQuery()) { // Führt SQL-Query aus
                while (rs.next()) {
                    out.add(map(rs)); // Wandelt eine DB-Zeile in ein TodoItem (siehe Funktion map() unten)
                }
            }
            return out;

        } catch (Exception e) {
            throw new RuntimeException("Todos laden fehlgeschlagen", e);
        }
    }

    public int insert(TodoItem item) { // fügt neues TodoItem ein
        String sql = "INSERT INTO TodoItems (CategoryId, Title, DueDate, Status) VALUES (?, ?, ?, ?)";

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { // aktiviert Rückgabe
                                                                                                   // von Auto-ID

            ps.setInt(1, item.getCategoryId()); // setzt FK usw.
            ps.setString(2, item.getTitle());
            ps.setString(3, item.getDueDate() == null ? null : item.getDueDate().toString()); // wandelt Datum in
                                                                                              // ISO-String
            ps.setInt(4, toDbStatus(item.getStatus()));

            ps.executeUpdate(); // führt insert aus

            try (ResultSet keys = ps.getGeneratedKeys()) { // liest automatisch generierte IDs
                if (keys.next())
                    return keys.getInt(1);
            }
            throw new RuntimeException("Keine ID zurueckgegeben");

        } catch (Exception e) {
            throw new RuntimeException("Todo einfuegen fehlgeschlagen", e);
        }
    }

    public void updateStatus(int todoId, TodoStatus status) { // aktualisiert Status eines Todos
        String sql = "UPDATE TodoItems SET Status = ? WHERE Id = ?";

        try (Connection c = Db.open(); // Db.open() --> siehe Db.java-Datei
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, toDbStatus(status)); // Enum zu DB-Wert
            ps.setInt(2, todoId);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Todo-Status aktualisieren fehlgeschlagen", e);
        }
    }

    private TodoItem map(ResultSet rs) throws Exception { // Mapping-Methode, wandelt DB-Zeile zu Domain-Objekt
        int id = rs.getInt("Id"); // liest PK
        int catId = rs.getInt("CategoryId"); // liest FK usw
        String title = rs.getString("Title");
        String due = rs.getString("DueDate");
        int status = rs.getInt("Status");

        LocalDate dueDate = (due == null || due.isBlank()) ? null : LocalDate.parse(due); // wandelt String zu LocalDate
        TodoStatus st = fromDbStatus(status); // DB-Wert zu Enum

        return new TodoItem(id, catId, title, dueDate, st); // erzeugt vollstaendiges Domain-Objekt
    }

    private int toDbStatus(TodoStatus s) { // Enum zu DB-Wert
        // Annahme: 0 = OPEN, 1 = DONE
        return (s == TodoStatus.DONE) ? 1 : 0; // Mapping Definition
    }

    private TodoStatus fromDbStatus(int v) { // DB-Wert zu Enum
        return (v == 1) ? TodoStatus.DONE : TodoStatus.OPEN; // Rueckwandlung
    }
}
