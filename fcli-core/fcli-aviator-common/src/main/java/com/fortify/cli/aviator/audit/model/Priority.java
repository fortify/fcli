package com.fortify.cli.aviator.audit.model;


public enum Priority {
    Critical,
    High,
    Medium,
    Low;

    public static Priority fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Priority value cannot be null.");
        }
        for (Priority p : values()) {
            if (p.name().equalsIgnoreCase(value)) {
                return p;
            }
        }
        throw new IllegalArgumentException("No Priority constant with name '" + value + "' found.");
    }
}