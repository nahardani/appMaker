package com.company.appmaker.enums;

public enum Category {
    ONE("1"),
    SPRING_SCAFFOLD("SPRING_SCAFFOLD");

    private final String value;

    Category(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

