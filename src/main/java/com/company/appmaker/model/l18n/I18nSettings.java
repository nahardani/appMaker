// src/main/java/com/company/appmaker/model/I18nSettings.java
package com.company.appmaker.model.l18n;

import java.util.*;

public class I18nSettings {
    private Boolean enabled = Boolean.TRUE;
    private String  baseName = "messages";   // نام bundle
    private String  defaultLang = "fa";      // fa/en/...
    private String  defaultLocale = "fa";      // fa/en/...
    private List<String> languages = new ArrayList<>(List.of("fa","en"));

    private List<I18nKey> keys = new ArrayList<>();

    public static class I18nKey {
        private String key; // مثلا: app.title
        private Map<String,String> translations = new LinkedHashMap<>();
        public I18nKey() {}
        public I18nKey(String key, Map<String,String> translations){
            this.key = key;
            this.translations = translations;
        }
        // getters/setters
        public String getKey(){ return key; }
        public void setKey(String key){ this.key = key; }
        public Map<String,String> getTranslations(){ return translations; }
        public void setTranslations(Map<String,String> translations){ this.translations = translations; }
    }

    // getters/setters
    public Boolean getEnabled(){ return enabled; }
    public void setEnabled(Boolean enabled){ this.enabled = enabled; }
    public String getBaseName(){ return baseName; }
    public void setBaseName(String baseName){ this.baseName = baseName; }
    public String getDefaultLang(){ return defaultLang; }
    public void setDefaultLang(String defaultLang){ this.defaultLang = defaultLang; }
    public List<String> getLanguages(){ return languages; }
    public void setLanguages(List<String> languages){ this.languages = languages; }
    public List<I18nKey> getKeys(){ return keys; }
    public void setKeys(List<I18nKey> keys){ this.keys = keys; }
    public String getDefaultLocale() {
        return defaultLocale;
    }
    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }
}
