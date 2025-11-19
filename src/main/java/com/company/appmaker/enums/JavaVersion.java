package com.company.appmaker.enums;

public enum JavaVersion {
    V8("8"),
    V11("11"),
    V17("17"),
    V21("21");

    private final String value;

    JavaVersion(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
