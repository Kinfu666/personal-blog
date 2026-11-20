package com.example.personalblog.exception;

public class RuleViolation extends RuntimeException {
    private final String field;

    public RuleViolation(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
