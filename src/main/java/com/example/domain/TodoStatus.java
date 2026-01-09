package com.example.domain;

public enum TodoStatus {
    OPEN(0),
    DONE(1);

    private final int dbValue;

    TodoStatus(int dbValue) {
        this.dbValue = dbValue;
    }

    public int getDbValue() {
        return dbValue;
    }

    public static TodoStatus fromDbValue(int dbValue) {
        for (TodoStatus status : values()) {
            if (status.dbValue == dbValue) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown TodoStatus dbValue: " + dbValue);
    }
}
