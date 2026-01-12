CREATE TABLE IF NOT EXISTS Categories (
  Id    INTEGER PRIMARY KEY AUTOINCREMENT,
  Name  TEXT NOT NULL UNIQUE,
  Icon  TEXT
);

CREATE TABLE IF NOT EXISTS TodoItems (
  Id          INTEGER PRIMARY KEY AUTOINCREMENT,
  Title       TEXT NOT NULL,
  Description TEXT,
  DueDate     TEXT,
  Notes       TEXT,
  Status      INTEGER NOT NULL,
  CategoryId  INTEGER,
  FOREIGN KEY (CategoryId) REFERENCES Categories(Id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS IX_TodoItems_Status     ON TodoItems(Status);
CREATE INDEX IF NOT EXISTS IX_TodoItems_CategoryId ON TodoItems(CategoryId);
CREATE INDEX IF NOT EXISTS IX_TodoItems_DueDate    ON TodoItems(DueDate);
