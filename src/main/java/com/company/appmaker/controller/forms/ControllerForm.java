package com.company.appmaker.controller.forms;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

public class ControllerForm {

    @NotBlank
    private String name;              // مثال: OrderController
    @NotBlank
    private String basePath;          // مثال: /api/orders
    @NotBlank
    private String type = "REST";     // فعلاً REST
    private String description;       // اختیاری

    private Boolean editing = false;          // حالت صفحه: ویرایش یا ساخت جدید
    private String  originalControllerName;   // برای rename ایمن

    private List<EndpointSlot> endpoints = new ArrayList<>();

    // --- Getters/Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBasePath() { return basePath; }
    public void setBasePath(String basePath) { this.basePath = basePath; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getEditing() { return editing != null ? editing : Boolean.FALSE; }
    public void setEditing(Boolean editing) { this.editing = editing; }

    public String getOriginalControllerName() { return originalControllerName; }
    public void setOriginalControllerName(String originalControllerName) { this.originalControllerName = originalControllerName; }

    public List<EndpointSlot> getEndpoints() { return endpoints; }
    public void setEndpoints(List<EndpointSlot> endpoints) { this.endpoints = (endpoints != null) ? endpoints : new ArrayList<>(); }

    /** کمکی: افزودن یک اندپوینت خالی به فرم */
    public void addEmptyEndpoint() { this.endpoints.add(new EndpointSlot()); }
}
