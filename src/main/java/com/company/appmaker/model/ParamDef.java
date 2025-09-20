package com.company.appmaker.model;

public class ParamDef {
    private String name;       // مثال: id
    private String in;         // PATH | QUERY | HEADER
    private String javaType;   // String, Long, Integer, Double, Boolean, LocalDate, UUID, ...
    private boolean required;  // برای QUERY/HEADER معنی‌دارتر است

    public ParamDef() {
    }

    public ParamDef(String name, String in, String javaType, boolean required) {
        this.name = name;
        this.in = in;
        this.javaType = javaType;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIn() {
        return in;
    }

    public void setIn(String in) {
        this.in = in;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}
