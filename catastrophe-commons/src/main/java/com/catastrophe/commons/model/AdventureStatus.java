package com.catastrophe.commons.model;

/**
 * Estados posibles de una aventura.
 * Sealed para garantizar exhaustividad en switch con pattern matching.
 */
public enum AdventureStatus {
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    FAILED("failed"),
    ABANDONED("abandoned");

    private final String dbValue;

    AdventureStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static AdventureStatus fromDb(String value) {
        return switch (value) {
            case "in_progress" -> IN_PROGRESS;
            case "completed"   -> COMPLETED;
            case "failed"      -> FAILED;
            case "abandoned"   -> ABANDONED;
            default -> throw new IllegalArgumentException("Estado de aventura desconocido: " + value);
        };
    }
}
