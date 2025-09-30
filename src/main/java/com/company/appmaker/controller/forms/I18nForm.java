// src/main/java/com/company/appmaker/controller/forms/I18nForm.java
package com.company.appmaker.controller.forms;

import com.company.appmaker.model.l18n.I18nSettings;
import java.util.*;

public class I18nForm {
    private Boolean enabled = Boolean.TRUE;
    private String  baseName = "messages";
    private String  defaultLang = "fa";
    private List<String> languages = new ArrayList<>();
    private List<KeySlot> keys = new ArrayList<>();

    public static class KeySlot {
        private String key;
        private Map<String,String> translations = new LinkedHashMap<>();
        public String getKey(){ return key; }
        public void setKey(String key){ this.key = key; }
        public Map<String,String> getTranslations(){ return translations; }
        public void setTranslations(Map<String,String> translations){ this.translations = translations; }
    }

    /* -------- mapping -------- */
    public static I18nForm from(I18nSettings s){
        I18nForm f = new I18nForm();
        if (s == null) return f;
        f.setEnabled(s.getEnabled()==null? Boolean.TRUE : s.getEnabled());
        f.setBaseName(s.getBaseName()==null? "messages" : s.getBaseName());
        f.setDefaultLang(s.getDefaultLang()==null? "fa" : s.getDefaultLang());
        f.setLanguages(s.getLanguages()==null? new ArrayList<>() : new ArrayList<>(s.getLanguages()));
        if (s.getKeys()!=null){
            List<KeySlot> list = new ArrayList<>();
            for (I18nSettings.I18nKey k : s.getKeys()){
                if (k==null) continue;
                KeySlot slot = new KeySlot();
                slot.setKey(k.getKey());
                slot.setTranslations(k.getTranslations()==null? new LinkedHashMap<>() : new LinkedHashMap<>(k.getTranslations()));
                list.add(slot);
            }
            f.setKeys(list);
        }
        return f;
    }

    /** مقادیر فرم را روی مدل دیتابیس اعمال می‌کند */
    public void applyTo(I18nSettings t){
        if (t==null) return;
        t.setEnabled(this.enabled==null? Boolean.TRUE : this.enabled);
        t.setBaseName(this.baseName==null? "messages" : this.baseName.trim());
        t.setDefaultLang(this.defaultLang==null? "fa" : this.defaultLang.trim());
        t.setLanguages(this.languages==null? new ArrayList<>() : new ArrayList<>(this.languages));

        List<I18nSettings.I18nKey> out = new ArrayList<>();
        if (this.keys!=null){
            for (KeySlot ks : this.keys){
                if (ks==null || ks.getKey()==null || ks.getKey().isBlank()) continue;
                var m = ks.getTranslations()==null? new LinkedHashMap<String,String>() : new LinkedHashMap<>(ks.getTranslations());
                out.add(new I18nSettings.I18nKey(ks.getKey().trim(), m));
            }
        }
        t.setKeys(out);
    }

    /* -------- getters/setters -------- */
    public Boolean getEnabled(){ return enabled; }
    public void setEnabled(Boolean enabled){ this.enabled = enabled; }
    public String getBaseName(){ return baseName; }
    public void setBaseName(String baseName){ this.baseName = baseName; }
    public String getDefaultLang(){ return defaultLang; }
    public void setDefaultLang(String defaultLang){ this.defaultLang = defaultLang; }
    public List<String> getLanguages(){ return languages; }
    public void setLanguages(List<String> languages){ this.languages = languages; }
    public List<KeySlot> getKeys(){ return keys; }
    public void setKeys(List<KeySlot> keys){ this.keys = keys; }
}
