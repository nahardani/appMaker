package com.company.appmaker.model;

import java.util.ArrayList;
import java.util.List;

public class ResponsePartDef {
    private String name;            // نام فیلد در DTO پاسخ (مثلاً items, summary, total)
    private String container;       // SINGLE | LIST
    private String kind;            // SCALAR | OBJECT

    // برای SCALAR:
    private String scalarType;      // String, Long, ...

    // برای OBJECT:
    private String objectName;      // نام DTO (اختیاری؛ خالی باشد از نام پیش‌فرض می‌سازیم)
    private List<FieldDef> fields = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContainer() { return container; }
    public void setContainer(String container) { this.container = container; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getScalarType() { return scalarType; }
    public void setScalarType(String scalarType) { this.scalarType = scalarType; }
    public String getObjectName() { return objectName; }
    public void setObjectName(String objectName) { this.objectName = objectName; }
    public List<FieldDef> getFields() { return fields; }
    public void setFields(List<FieldDef> fields) { this.fields = (fields!=null?fields:new ArrayList<>()); }
}
