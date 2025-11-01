// src/main/java/com/company/appmaker/model/ConstantsSettings.java
package com.company.appmaker.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConstantsSettings {
    // نگه‌داری جفت‌کلید/مقدار با ترتیب ورود
    private Map<String, String> entries = new LinkedHashMap<>();

    public Map<String, String> getEntries() { return entries; }
    public void setEntries(Map<String, String> entries) {
        this.entries = (entries != null ? new LinkedHashMap<>(entries) : new LinkedHashMap<>());
    }
}
