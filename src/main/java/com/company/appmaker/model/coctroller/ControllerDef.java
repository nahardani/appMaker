package com.company.appmaker.model.coctroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ControllerDef {
    private String name;
    private String basePath;
    private String type;

    // برای سازگاری با داده‌های قدیمی
    private List<String> methods = new ArrayList<>();

    //
    private List<EndpointDef> endpoints = new ArrayList<>();

    private String defaultHttpMethod; // NEW: برای نگه‌داشتن انتخاب کاربر در باکس کنترلر

    public String getDefaultHttpMethod() { return defaultHttpMethod; }
    public void setDefaultHttpMethod(String defaultHttpMethod) { this.defaultHttpMethod = defaultHttpMethod; }


    public ControllerDef() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getMethods() {
        return methods == null ? Collections.emptyList() : methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = (methods == null ? new ArrayList<>() : methods);
    }

    // 👇 این getter هرگز null برنمی‌گرداند تا Thymeleaf روی th:each ارور ندهد
    public List<EndpointDef> getEndpoints() {
        return endpoints == null ? Collections.emptyList() : endpoints;
    }

    public void setEndpoints(List<EndpointDef> endpoints) {
        this.endpoints = (endpoints == null ? new ArrayList<>() : endpoints);
    }
}
