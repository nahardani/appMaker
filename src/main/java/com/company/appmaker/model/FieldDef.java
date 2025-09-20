package com.company.appmaker.model;

public class FieldDef {
    private String name;      // e.g. orderId, customerName
    private String javaType;  // String, Long, Integer, Boolean, Double, UUID, LocalDate, ...
    private boolean required;

    public FieldDef() {}
    public FieldDef(String name, String javaType, boolean required) {
        this.name = name; this.javaType = javaType; this.required = required;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getJavaType() { return javaType; }
    public void setJavaType(String javaType) { this.javaType = javaType; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
}
