package com.company.appmaker.controller.forms;

import com.company.appmaker.model.I18nSettings;

import java.util.*;

public class I18nForm {
    private String defaultLocale = "fa";
    /**
     * CSV زبان‌ها، مثل: "fa,en"
     */
    private String localesCsv = "fa,en";
    /**
     * basename: پیش‌فرض "messages"
     */
    private String baseName = "messages";

    /**
     * ردیف‌های کلید/ترجمه‌ها
     */
    private java.util.List<KeyRow> keys = new ArrayList<>();

    // === getters/setters ===
    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String v) {
        this.defaultLocale = v;
    }

    public String getLocalesCsv() {
        return localesCsv;
    }

    public void setLocalesCsv(String v) {
        this.localesCsv = v;
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String v) {
        this.baseName = v;
    }

    public java.util.List<KeyRow> getKeys() {
        return keys;
    }

    public void setKeys(java.util.List<KeyRow> rows) {
        this.keys = (rows != null ? rows : new ArrayList<>());
    }

    /**
     * یک ردیف فرم: code + map ترجمه‌ها
     */
    public static class KeyRow {
        private String code;
        private java.util.Map<String, String> translations = new LinkedHashMap<>();

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public java.util.Map<String, String> getTranslations() {
            return translations;
        }

        public void setTranslations(java.util.Map<String, String> m) {
            this.translations = (m != null ? m : new LinkedHashMap<>());
        }
    }

    // --- تبدیل‌ها ---
    public static I18nForm from(I18nSettings s) {
        I18nForm f = new I18nForm();
        if (s == null) return f;
        f.setDefaultLocale(nz(s.getDefaultLocale(), "fa"));
        var locs = (s.getLocales() == null || s.getLocales().isEmpty()) ? List.of("fa", "en") : s.getLocales();
        f.setLocalesCsv(String.join(",", locs));
        f.setBaseName(nz(s.getBaseName(), "messages"));

        if (s.getKeys() != null) {
            for (var k : s.getKeys()) {
                KeyRow r = new KeyRow();
                r.setCode(k.getCode());
                r.setTranslations(k.getTranslations() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(k.getTranslations()));
                f.getKeys().add(r);
            }
        }
        return f;
    }

    public void applyTo(I18nSettings s) {
        if (s == null) return;
        s.setDefaultLocale(nz(defaultLocale, "fa"));
        s.setBaseName(nz(baseName, "messages"));
        s.setLocales(parseCsv(localesCsv));

        var list = new ArrayList<I18nSettings.I18nKey>();
        if (keys != null) {
            for (var r : keys) {
                if (r == null || r.getCode() == null || r.getCode().isBlank()) continue;
                var map = (r.getTranslations() == null) ? new LinkedHashMap<String, String>() : new LinkedHashMap<>(r.getTranslations());
                list.add(new I18nSettings.I18nKey(r.getCode().trim(), map));
            }
        }
        s.setKeys(list);
    }

    private static java.util.List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return new ArrayList<>();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String nz(String v, String def) {
        return (v == null || v.isBlank()) ? def : v.trim();
    }


}
