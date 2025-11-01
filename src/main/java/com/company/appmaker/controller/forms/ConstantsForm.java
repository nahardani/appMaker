// src/main/java/com/company/appmaker/controller/forms/ConstantsForm.java
package com.company.appmaker.controller.forms;

import com.company.appmaker.model.ConstantsSettings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// فرم سبک برای باکس "ثوابت"
public class ConstantsForm {

    // یک لیست از سطرها برای UI
    private List<EntrySlot> entries = new ArrayList<>();

    public List<EntrySlot> getEntries() { return entries; }
    public void setEntries(List<EntrySlot> entries) { this.entries = (entries != null ? entries : new ArrayList<>()); }

    // تبدیل از مدل دامنه به فرم
    public static ConstantsForm from(com.company.appmaker.model.ConstantsSettings cs) {
        ConstantsForm f = new ConstantsForm();
        if (cs != null && cs.getEntries() != null) {
            cs.getEntries().forEach((k, v) -> f.entries.add(new EntrySlot(k, v)));
        }
        return f;
    }

    // اعمال فرم روی مدل دامنه
    public void applyTo(ConstantsSettings cs) {
        if (cs == null) return;
        Map<String, String> map = new LinkedHashMap<>();
        if (entries != null) {
            for (EntrySlot e : entries) {
                if (e == null) continue;
                String k = e.getKey() == null ? "" : e.getKey().trim();
                if (k.isEmpty()) continue;
                map.put(k, e.getValue() == null ? "" : e.getValue());
            }
        }
        cs.setEntries(map);
    }

    // اسلات هر ردیف فرم
    public static class EntrySlot {
        private String key;
        private String value;

        public EntrySlot() {}
        public EntrySlot(String key, String value) { this.key = key; this.value = value; }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}
