package com.company.appmaker.controller.forms;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

public class EndpointSlot {

    @NotBlank
    private String httpMethod = "GET"; // GET/POST/PUT/PATCH/DELETE

    private Boolean useEndpointPath = false; // مسیر نسبت به basePath اختیاری
    private String endpointPath;             // "/{id}" یا "/search"
    private String endpointName;             // نام متد جاوا (اختیاری)

    private List<ParamSlot> params = new ArrayList<>();              // URL/HEADER
    private List<FieldSlot> requestFields = new ArrayList<>();       // Body برای POST/PUT/PATCH
    private List<ResponsePartSlot> responseParts = new ArrayList<>();// خروجی چندبخشی

    // --- Getters/Setters ---
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public Boolean getUseEndpointPath() { return useEndpointPath != null ? useEndpointPath : Boolean.FALSE; }
    public void setUseEndpointPath(Boolean useEndpointPath) { this.useEndpointPath = useEndpointPath; }

    public String getEndpointPath() { return endpointPath; }
    public void setEndpointPath(String endpointPath) { this.endpointPath = endpointPath; }

    public String getEndpointName() { return endpointName; }
    public void setEndpointName(String endpointName) { this.endpointName = endpointName; }

    public List<ParamSlot> getParams() { return params; }
    public void setParams(List<ParamSlot> params) { this.params = (params != null) ? params : new ArrayList<>(); }

    public List<FieldSlot> getRequestFields() { return requestFields; }
    public void setRequestFields(List<FieldSlot> requestFields) { this.requestFields = (requestFields != null) ? requestFields : new ArrayList<>(); }

    public List<ResponsePartSlot> getResponseParts() { return responseParts; }
    public void setResponseParts(List<ResponsePartSlot> responseParts) { this.responseParts = (responseParts != null) ? responseParts : new ArrayList<>(); }
}
