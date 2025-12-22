# Projekt: To-Do-Liste ZbW / Aufgabenverwaltung

## 1. Ausgangslage

Es soll im Rahmen des Programmierprojekts an der ZbW eine To-Do-Liste (Aufgabenverwaltung) entwickelt werden. Das Programm dient der strukturierten Erfassung, Bearbeitung und Verwaltung von Aufgaben. Die Daten werden persistent in einer Datenbank gespeichert. Zusaetzlich sind UML, ERM, GUI-Entwurf sowie eine Prozessbeschreibung zu erstellen. Die Versionierung erfolgt mit GitLab, inklusive Exportmoeglichkeit der Datenbank zur Wiederherstellung.

---

## 2. Zielsetzung

Ziel ist die Entwicklung einer benutzerfreundlichen To-Do-Anwendung mit klarer Struktur, nachvollziehbarer Architektur und sauberem Datenmodell.

Ziele im Ueberblick:

- Verwaltung von Aufgaben (CRUD)
- Status-, Prioritaets- und Faelligkeitsverwaltung
- Persistente Datenspeicherung
- Grafische Benutzeroberflaeche (GUI)
- UML-Klassendiagramm und ERM
- Datenbank-Export und -Wiederherstellung
- Versionsverwaltung mit GitLab

---

## 3. Funktionsumfang

### 3.1 Muss-Funktionen

- Aufgabe erstellen
- Aufgabe anzeigen (Listenansicht)
- Aufgabe bearbeiten
- Aufgabe loeschen
- Status verwalten: Offen, In Arbeit, Erledigt
- Prioritaet setzen
- Faelligkeitsdatum erfassen
- Suche und Filter (Status, Prioritaet, Kategorie)

### 3.2 Kann-Funktionen

- Kategorien
- Tags (mehrere pro Aufgabe)
- Archiv fuer erledigte Aufgaben
- Export als CSV/JSON

---

## 4. GUI-Uebersicht (Konzept)

### Hauptfenster

**Linke Seite: Aufgabenliste**

- Suchfeld
- Filter (Status, Prioritaet, Kategorie)
- Aufgabenliste mit Spalten:

  - Titel
  - Prioritaet
  - Faelligkeitsdatum
  - Status

**Rechte Seite: Detailansicht**

- Titel (Pflichtfeld)
- Beschreibung
- Prioritaet (Low / Medium / High)
- Status
- Faelligkeitsdatum (DatePicker)
- Kategorie
- Tags

**Buttons:**

- Neu
- Speichern
- Bearbeiten
- Loeschen
- Abbrechen

### Menue

- Datei

  - Datenbank exportieren
  - Datenbank importieren

- Hilfe

  - About

---

## 5. UML – Klassendiagramm (Beschreibung)

### Klassen

#### TodoItem

**Attribute:**

- Id: int (Eindeutiger Primaerschluessel der Aufgabe in der Datenbank.)
- Title: string (Kurzbeschreibung der Aufgabe. Pflichtfeld, zentrales Identifikationsmerkmal fuer den Benutzer.)
- Description: string (Optionale Detailbeschreibung der Aufgabe.)
- DueDate: DateTime? (Faelligkeitsdatum der Aufgabe. Nullable, da Aufgaben auch ohne Termin existieren koennen.)
- Status: TodoStatus (Aktueller Bearbeitungszustand der Aufgabe. Typischer Enum: Open, InProgress, Done)
- Priority: Priority (Wichtigkeit der Aufgabe. Enum, z.B.: Low, Medium, High)
- CategoryId: int? (Fremdschluessel auf Category.id. Ordnet die Aufgabe optional genau einer Kategorie zu.)

**Methoden:**

- MarkDone() (Setzt den Status der Aufgabe auf Done.)
- SetStatus(status) (Aendert den Status gezielt auf einen definierten Wert (z.B. Open → InProgress).)
- IsOverdue(now): bool (Prueft, ob das Faelligkeitsdatum vor dem aktuellen Zeitpunkt liegt und der Status nicht Done ist.)

#### Category

**Attribute:**

- Id: int (Primaerschluessel der Kategorie.)
- Name: string (Bezeichnung der Kategorie (z.B. „Schule“, „Arbeit“, „Privat“). Muss eindeutig sein.)

**Methoden:**

- Rename(name) (Aendert den Namen der Kategorie.)

#### Tag

**Attribute:**

- Id: int (Primaerschluessel des Tags.)
- Name: string (Bezeichnung des Tags (z.B. „dringend“, „optional“). Eindeutig.)

**Methoden:**

- Rename(name) (Aendert den Namen des Tags.)

#### TodoService (Business-Logik-Schicht. Vermittelt zwischen GUI und Repository.(Fachlogik und Validierung))

**Methoden:**

- CreateTodo(dto) (Erstellt ein neues TodoItem aus GUI-Eingaben (DTO), validiert Daten und speichert es.)
- UpdateTodo(id, dto) (Aktualisiert ein bestehendes TodoItem anhand der ID und neuer Eingabedaten.)
- DeleteTodo(id) (Loescht eine Aufgabe inklusive zugehoeriger Tag-Zuordnungen.)
- GetTodos(filter) (Liefert Aufgaben fuer die GUI, delegiert die Abfrage an das Repository.)
- ToggleDone(id) (Wechselt den Status einer Aufgabe zwischen Done und Open/InProgress.)

#### TodoRepository (Zugriffsschicht auf die Datenbank (Persistence Layer).Keine Business-Logik.)

**Methoden:**

- InsertTodo(todo) (Speichert ein neues TodoItem in der Datenbank.)
- UpdateTodo(todo) (Aktualisiert ein bestehendes TodoItem in der Datenbank.)
- DeleteTodo(id) (Entfernt ein TodoItem anhand seiner ID aus der Datenbank.)
- QueryTodos(filter) (Liefert eine Liste von TodoItems basierend auf Filterkriterien (Status, Prioritaet, Textsuche, Kategorie).)

---

## 6. ERM – Entity-Relationship-Modell

### Entitaeten

#### TodoItems

- Id (PK)
- Title
- Description
- DueDate
- Status
- Priority
- CategoryId (FK)

#### Categories

- Id (PK)
- Name (UNIQUE)

#### Tags

- Id (PK)
- Name (UNIQUE)

#### TodoItemTags

- TodoItemId (FK)
- TagId (FK)
- Zusammengesetzter PK (TodoItemId, TagId)

### Beziehungen

- Category 1 : n TodoItems
- TodoItems n : m Tags

---

## 7. Prozessbeschreibung

### Aufgabe erfassen

1. Benutzer klickt auf "Neu"
2. Formular wird angezeigt
3. Pflichtfelder werden validiert
4. Aufgabe wird gespeichert
5. Aufgabenliste wird aktualisiert

### Aufgabe erledigen

1. Aufgabe auswaehlen
2. Status auf "Erledigt" setzen
3. Datenbank aktualisieren

### Filtern und Suchen

1. Filter setzen oder Suchtext eingeben
2. Aufgabenliste wird dynamisch aktualisiert

---

## 8. Datenbank-Export und Wiederherstellung

### Export

- Export der SQLite-Datenbank als Datei (Backup)
- Dateiname mit Zeitstempel

### Import

- Import einer bestehenden DB-Datei
- Wiederherstellung des gesamten Datenbestands

---

## 9. Versionierung mit GitLab

### Repository-Struktur

- src/ : Quellcode
- docs/ : Dokumentation (Markdown, Diagramme)
- db/ : Datenbank oder Migrationen

### Versionierung

- Regelmaessige Commits
- Saubere Commit-Messages
- Finaler Release-Tag fuer Abgabe

---

## 10. Zusammenfassung

Das Projekt To-Do-Liste erfuellt die Anforderungen der Aufgabenstellung durch eine klare Trennung von GUI, Logik und Datenhaltung. UML, ERM und Prozessbeschreibungen sorgen fuer Nachvollziehbarkeit und Wartbarkeit. Die Anwendung ist erweiterbar und praxisnah umgesetzt.

# **Klassendiagramm**

![[assets/UML-Klassendiagramm-ToDoListe.drawio.svg]]

# **ERM-Modell**

![[assets/ERM-Modell-ToDoListe.drawio.svg]]

# **Farbpalette**

https://paletton.com/#uid=13b0u0kllllaFw0g0qFqFg0w0aF
