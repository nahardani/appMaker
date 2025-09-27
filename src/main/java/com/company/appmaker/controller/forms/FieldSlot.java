package com.company.appmaker.controller.forms;

public class FieldSlot {
    private String name;      // مثال: "id" یا "totalPrice"
    private String javaType;  // String/Long/...
    private Boolean required = false;

    public FieldSlot() {}
    public FieldSlot(String name, String javaType, Boolean required) {
        this.name = name; this.javaType = javaType; this.required = required;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getJavaType() { return javaType; }
    public void setJavaType(String javaType) { this.javaType = javaType; }

    public Boolean getRequired() { return required != null ? required : Boolean.FALSE; }
    public void setRequired(Boolean required) { this.required = required; }
}
