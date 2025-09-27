package com.company.appmaker.model;

import java.util.*;

public class I18nSettings {
    /** زبان پیش‌فرض مثلاً "fa" یا "en" */
    private String defaultLocale = "fa";
    /** لیست زبان‌های فعال؛ مثلاً ["fa","en"] */
    private java.util.List<String> locales = new ArrayList<>();
    /** نام basename منابع (بدون پسوند)؛ پیش‌فرض: "messages" */
    private String baseName = "messages";
    /** کلیدهای اولیه (اختیاری) برای تولید فایل‌ها */
    private java.util.List<I18nKey> keys = new ArrayList<>();

    public String getDefaultLocale() { return defaultLocale; }
    public void setDefaultLocale(String defaultLocale) { this.defaultLocale = defaultLocale; }
    public java.util.List<String> getLocales() { return locales; }
    public void setLocales(java.util.List<String> locales) { this.locales = locales; }
    public String getBaseName() { return baseName; }
    public void setBaseName(String baseName) { this.baseName = baseName; }
    public java.util.List<I18nKey> getKeys() { return keys; }
    public void setKeys(java.util.List<I18nKey> keys) { this.keys = keys; }

    /** یک ردیف کلید و ترجمه‌هایش برای زبان‌های مختلف */
    public static class I18nKey {
        private String code; // مثل: "app.title"
        private java.util.Map<String,String> translations = new LinkedHashMap<>(); // locale -> text

        public I18nKey() {}
        public I18nKey(String code, Map<String,String> t){ this.code=code; this.translations=t; }

        public String getCode(){ return code; }
        public void setCode(String code){ this.code=code; }
        public Map<String, String> getTranslations() { return translations; }
        public void setTranslations(Map<String, String> translations) { this.translations = translations; }
    }


}
