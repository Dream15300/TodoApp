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

public class TodoRepository { // Repository-Klasse für CRUD-/Query-Operationen auf TodoItems

    public boolean hasTodos(int categoryId) { // Prüft, ob Todos für eine bestimmte Kategorie existieren
        String sql = "SELECT 1 FROM TodoItems WHERE CategoryId = ? LIMIT 1";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, categoryId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }

        } catch (Exception exception) {
            throw new RuntimeException("Todo-Existenz prüfen fehlgeschlagen", exception);
        }
    }

    public List<TodoItem> findOpenByCategory(int categoryId) { // Findet alle offenen Todos für eine bestimmte Kategorie
        String sql = """
                SELECT Id, CategoryId, Title, DueDate, Notes, Status, Priority
                FROM TodoItems
                WHERE CategoryId = ? AND Status = 0
                ORDER BY Priority DESC, DueDate IS NULL, DueDate, Id
                    """;
        return queryList(sql, categoryId);
    }

    public List<TodoItem> findDoneByCategory(int categoryId) { // Findet alle erledigten Todos für eine bestimmte
                                                               // Kategorie
        String sql = """
                SELECT Id, CategoryId, Title, DueDate, Notes, Status, Priority
                FROM TodoItems
                WHERE CategoryId = ? AND Status = 1
                ORDER BY DueDate IS NULL, DueDate DESC, Id DESC
                    """;
        return queryList(sql, categoryId);
    }

    private List<TodoItem> queryList(String sql, int categoryId) { // Hilfsmethode zur Abfrage von TodoItems, mappt
                                                                   // (Datenübertragung)
                                                                   // ResultSet zu Liste von TodoItems
        List<TodoItem> outputedList = new ArrayList<>();

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, categoryId); // Setzt Kategorie-ID im PreparedStatement an erster Stelle

            try (ResultSet resultSet = preparedStatement.executeQuery()) { // Führt Abfrage aus und erhält ResultSet
                while (resultSet.next())
                    outputedList.add(map(resultSet));
            }

            return outputedList;

        } catch (Exception exception) {
            throw new RuntimeException("Todos laden fehlgeschlagen", exception);
        }
    }

    public int insert(TodoItem item) { // Fügt ein neues TodoItem in die Datenbank ein und gibt die generierte DB-ID
                                       // zurück
        String sql = "INSERT INTO TodoItems (CategoryId, Title, DueDate, Notes, Status, Priority) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql,
                        Statement.RETURN_GENERATED_KEYS)) { // PreparedStatement zum Einfügen mit Rückgabe der
                                                            // generierten Schlüssel

            preparedStatement.setInt(1, item.getCategoryId());
            preparedStatement.setString(2, item.getTitle());
            preparedStatement.setString(3, item.getDueDate() == null ? null : item.getDueDate().toString());
            preparedStatement.setString(4, item.getNotes());
            preparedStatement.setInt(5, toDbStatus(item.getStatus()));
            preparedStatement.setInt(6, item.getPriority());

            preparedStatement.executeUpdate();

            try (ResultSet keys = preparedStatement.getGeneratedKeys()) {
                if (keys.next())
                    return keys.getInt(1);
            }
            throw new RuntimeException("Keine ID zurückgegeben");

        } catch (Exception exception) {
            throw new RuntimeException("Todo einfügen fehlgeschlagen", exception);
        }
    }

    public void updateStatus(int todoId, TodoStatus status) { // Aktualisiert den Status eines TodoItems anhand seiner
                                                              // ID
        String sql = "UPDATE TodoItems SET Status = ? WHERE Id = ?";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, toDbStatus(status));
            preparedStatement.setInt(2, todoId);
            preparedStatement.executeUpdate();

        } catch (Exception exception) {
            throw new RuntimeException("Todo-Status aktualisieren fehlgeschlagen", exception);
        }
    }

    private TodoItem map(ResultSet resultSet) throws Exception { // Mapped eine ResultSet-Zeile zu einem TodoItem-Objekt
        int id = resultSet.getInt("Id");
        int catId = resultSet.getInt("CategoryId");
        String title = resultSet.getString("Title");
        String due = resultSet.getString("DueDate");
        String notes = resultSet.getString("Notes");
        int status = resultSet.getInt("Status");
        int priority = resultSet.getInt("Priority");

        LocalDate dueDate = (due == null || due.isBlank()) ? null : LocalDate.parse(due); // Konvertiert DueDate-String
                                                                                          // zu LocalDate
        TodoStatus todoStatus = fromDbStatus(status);

        return new TodoItem(id, catId, title, dueDate, notes, todoStatus, priority);
    }

    private int toDbStatus(TodoStatus converStatus) { // Konvertiert TodoStatus-Enum zu DB-kompatiblen Integer-Wert
        return (converStatus == TodoStatus.DONE) ? 1 : 0;
    }

    private TodoStatus fromDbStatus(int reverseConvertStatus) { // Konvertiert DB-kompatiblen Integer-Wert zu
                                                                // TodoStatus-Enum
        return (reverseConvertStatus == 1) ? TodoStatus.DONE : TodoStatus.OPEN;
    }
}
