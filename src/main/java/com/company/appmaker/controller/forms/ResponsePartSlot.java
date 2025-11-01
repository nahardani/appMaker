package com.company.appmaker.controller.forms;

import java.util.ArrayList;
import java.util.List;

public class ResponsePartSlot {
    private String name;                 // summary/items/error/...
    private String container = "SINGLE"; // SINGLE | LIST
    private String kind = "SCALAR";      // SCALAR | OBJECT
    private String scalarType;           // اگر SCALAR
    private String objectName;           // اگر OBJECT
    private List<FieldSlot> fields = new ArrayList<>(); // برای OBJECT

    public ResponsePartSlot() {}
    public ResponsePartSlot(String name, String container, String kind) {
        this.name = name; this.container = container; this.kind = kind;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContainer() { return container != null ? container : "SINGLE"; }
    public void setContainer(String container) { this.container = container; }

    public String getKind() { return kind != null ? kind : "SCALAR"; }
    public void setKind(String kind) { this.kind = kind; }

    public String getScalarType() { return scalarType; }
    public void setScalarType(String scalarType) { this.scalarType = scalarType; }

    public String getObjectName() { return objectName; }
    public void setObjectName(String objectName) { this.objectName = objectName; }

    public List<FieldSlot> getFields() { return fields; }
    public void setFields(List<FieldSlot> fields) { this.fields = (fields != null) ? fields : new ArrayList<>(); }
}
