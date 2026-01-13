package com.example.service;

import com.example.domain.Category;
import com.example.domain.TodoItem;
import com.example.domain.TodoStatus;
import com.example.persistence.CategoryRepository;
import com.example.persistence.TodoRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Zweck:
 * - Kapselt Geschäftslogik (Validierung, Regeln, Use-Cases)
 * - Orchestriert Repository-Aufrufe (CategoryRepository, TodoRepository)
 * - UI/Controller sollten nur diese Schicht konsumieren, nicht direkt
 * Repositories
 *
 * Architektur:
 * - typische 3-Schichten-Struktur: UI → Service → Persistence
 */
public class TodoService {

    /*
     * Der Service erzeugt seine Repositories direkt.
     * Hinweis:
     * - Für Tests/Erweiterbarkeit wäre Dependency Injection (Konstruktor-Injektion)
     * günstiger,
     * um Mock-Repositories einsetzen zu können.
     */
    private final CategoryRepository categoryRepo = new CategoryRepository();
    private final TodoRepository todoRepo = new TodoRepository();

    /**
     * Liefert alle Kategorien.
     *
     * @return Liste der Kategorien (sortiert gemäss Repository-Query)
     */
    public List<Category> getCategories() {
        return categoryRepo.findAll();
    }

    /**
     * Erstellt eine neue Kategorie.
     *
     * Validierung:
     * - Name ist Pflicht (nicht null, nicht leer nach trim)
     * - Icon wird getrimmt; null bleibt null
     *
     * @param name Kategorie-Name
     * @param icon Kategorie-Icon (optional)
     * @return generierte Kategorie-ID
     */
    public int createCategory(String name, String icon) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name ist Pflicht");
        }
        String trimmedName = name.trim();

        // Icon: optional, nur Whitespace entfernen
        String trimmedIcon = icon == null ? null : icon.trim();

        return categoryRepo.insert(trimmedName, trimmedIcon);
    }

    /**
     * Benennt eine Kategorie um (nur Name).
     *
     * @param id      Kategorie-ID
     * @param newName neuer Name (Pflicht)
     */
    public void renameCategory(int id, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Name ist Pflicht");
        }
        categoryRepo.updateName(id, newName.trim());
    }

    /**
     * Aktualisiert eine Kategorie (Name + Icon).
     *
     * Validierung:
     * - newName ist Pflicht
     * - newIcon: null → null, " " → null (durch trim + empty-check)
     *
     * @param id      Kategorie-ID
     * @param newName neuer Name
     * @param newIcon neues Icon (optional)
     */
    public void updateCategory(int id, String newName, String newIcon) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Name ist Pflicht");
        }

        String trimmedName = newName.trim();

        // Icon normalisieren: leere Eingabe wird als NULL gespeichert
        String trimmedIcon = (newIcon == null) ? null : newIcon.trim();
        if (trimmedIcon != null && trimmedIcon.isEmpty()) {
            trimmedIcon = null;
        }

        categoryRepo.updateName(id, trimmedName);
        categoryRepo.updateIcon(id, trimmedIcon);
    }

    /**
     * Löscht eine Kategorie.
     *
     * Geschäftsregel:
     * - Kategorie darf nur gelöscht werden, wenn keine Todos zugeordnet sind.
     *
     * @param categoryId Kategorie-ID
     */
    public void deleteCategory(int categoryId) {
        if (todoRepo.hasTodos(categoryId)) {
            throw new IllegalStateException("Liste enthält noch Todos. Erst Todos löschen/verschieben.");
        }
        categoryRepo.delete(categoryId);
    }

    /**
     * Liefert offene Todos einer Kategorie.
     *
     * @param categoryId Kategorie-ID
     * @return offene Todos
     */
    public List<TodoItem> getOpenTodosForCategory(int categoryId) {
        return todoRepo.findOpenByCategory(categoryId);
    }

    /**
     * Liefert erledigte Todos einer Kategorie.
     *
     * @param categoryId Kategorie-ID
     * @return erledigte Todos
     */
    public List<TodoItem> getDoneTodosForCategory(int categoryId) {
        return todoRepo.findDoneByCategory(categoryId);
    }

    /**
     * Zählt erledigte Todos einer Kategorie.
     *
     * @param categoryId Kategorie-ID
     * @return Anzahl DONE
     */
    public int countDoneTodosForCategory(int categoryId) {
        return todoRepo.countByCategoryAndStatus(categoryId, TodoStatus.DONE);
    }

    /**
     * Erstellt ein neues Todo in einer Kategorie.
     *
     * Validierung:
     * - Titel ist Pflicht
     *
     * Defaultwerte:
     * - notes = null
     * - status = OPEN
     * - priority = 0
     *
     * @param categoryId Kategorie-ID
     * @param title      Titel (Pflicht)
     * @param dueDate    Fälligkeitsdatum (optional)
     * @return generierte Todo-ID
     */
    public int addTodo(int categoryId, String title, LocalDate dueDate) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Titel ist Pflicht");
        }

        TodoItem item = new TodoItem(
                0, // ID wird von der DB generiert
                categoryId,
                title.trim(),
                dueDate,
                null, // Notes initial leer
                TodoStatus.OPEN,
                0 // Default-Priority
        );

        return todoRepo.insert(item);
    }

    /**
     * Aktualisiert Felder eines Todos.
     *
     * Validierung:
     * - newTitle darf nicht leer sein
     *
     * Delegation:
     * - Notes/Datum-Null-Handling wird im Repository umgesetzt (setNull / trim)
     *
     * @param todoId     Todo-ID
     * @param newTitle   neuer Titel
     * @param newDueDate neues Datum (optional)
     * @param notes      neue Notizen (optional)
     */
    public void updateTodo(int todoId, String newTitle, LocalDate newDueDate, String notes) {
        if (newTitle == null || newTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Titel darf nicht leer sein");
        }

        todoRepo.updateTodo(todoId, newTitle.trim(), newDueDate, notes);
    }

    /**
     * Löscht alle erledigten Todos einer Kategorie.
     *
     * @param categoryId Kategorie-ID
     */
    public void deleteDoneTodosByCategory(int categoryId) {
        todoRepo.deleteDoneByCategory(categoryId);
    }

    /**
     * Markiert ein Todo als erledigt.
     *
     * @param todoId Todo-ID
     */
    public void markDone(int todoId) {
        todoRepo.updateStatus(todoId, TodoStatus.DONE);
    }

    /**
     * Setzt ein Todo zurück auf offen.
     *
     * @param todoId Todo-ID
     */
    public void markOpen(int todoId) {
        todoRepo.updateStatus(todoId, TodoStatus.OPEN);
    }

    /**
     * Zählt offene Todos, die heute fällig sind.
     *
     * Zeitbezug:
     * - LocalDate.now() verwendet System-Default-Zeitzone.
     * - Wenn die App Zeitzonen-sensitiv sein soll: Clock injizieren.
     *
     * @return Anzahl offener Todos mit DueDate == heute
     */
    public int countDueTodayOpen() {
        return todoRepo.countByDueDateAndStatus(LocalDate.now(), TodoStatus.OPEN);
    }
}
