package com.company.appmaker.model.l18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class I18nConfig {
    private String defaultLocale = "fa";
    private List<String> supportedLocales = new ArrayList<>(); // مثلا fa,en
    // کلیدهای اولیه (اختیاری) برای seed فایل‌های messages
    private Map<String,String> baseKeys;

    public String getDefaultLocale(){return defaultLocale;}
    public void setDefaultLocale(String defaultLocale){this.defaultLocale=defaultLocale;}
    public List<String> getSupportedLocales(){return supportedLocales;}
    public void setSupportedLocales(List<String> supportedLocales){this.supportedLocales=supportedLocales;}
    public Map<String, String> getBaseKeys(){return baseKeys;}
    public void setBaseKeys(Map<String, String> baseKeys){this.baseKeys=baseKeys;}
}
