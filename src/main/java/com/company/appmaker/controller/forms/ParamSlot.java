package com.company.appmaker.controller.forms;

public class ParamSlot {
    private String name;      // id / q / X-Trace
    private String in;        // PATH | QUERY | HEADER
    private String javaType;  // String, Long, ...
    private Boolean required = false;

    public ParamSlot() {}
    public ParamSlot(String name, String in, String javaType, Boolean required) {
        this.name = name; this.in = in; this.javaType = javaType; this.required = required;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIn() { return in; }
    public void setIn(String in) { this.in = in; }

    public String getJavaType() { return javaType; }
    public void setJavaType(String javaType) { this.javaType = javaType; }

    public Boolean getRequired() { return required != null ? required : Boolean.FALSE; }
    public void setRequired(Boolean required) { this.required = required; }
}
