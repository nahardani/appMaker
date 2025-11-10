package com.company.appmaker.ai.preflight;

import java.util.*;

public class PreflightModels {

    public static final class Question {
        public String key;
        public String label;
        public String type; // text|number|select|textarea|boolean
        public boolean required;
        public List<String> options;
        public String placeholder;

        public Question(String key, String label, String type, boolean required) {
            this.key = key; this.label = label; this.type = type; this.required = required;
        }
        public Question opts(String... opts) { this.options = Arrays.asList(opts); return this; }
        public Question ph(String p) { this.placeholder = p; return this; }
    }

    public static final class Preflight {
        public boolean needsExternal;
        public boolean needsDb;

        public String controllerName;
        public String endpointName;
        public String basePath;

        // external
        public String externalBaseUrl;
        public String externalAuthType; // NONE|BASIC|BEARER|API_KEY
        public String externalHttpMethod; // GET|POST|...
        public String externalPathTemplate;

        public Integer timeoutMs;
        public Integer retryMaxAttempts;
        public Integer retryBackoffMs;

        // gaps & questions
        private final List<Question> questions = new ArrayList<>();
        public List<Question> questions(){ return questions; }

        public boolean hasBlockingGaps(){
            // اگر needsExternal=true و هر کدام از ۴ کلید اصلی خالی بود، بلاکینگ است
            if (needsExternal) {
                if (isBlank(externalBaseUrl) || isBlank(externalAuthType)
                        || isBlank(externalHttpMethod) || isBlank(externalPathTemplate)) {
                    return true;
                }
            }
            return false;
        }

        public Map<String,Object> toVars(){
            Map<String,Object> v = new LinkedHashMap<>();
            v.put("needsExternal", needsExternal);
            v.put("needsDb",       needsDb);
            if (controllerName != null) v.put("controllerName", controllerName);
            if (endpointName   != null) v.put("endpointName",   endpointName);
            if (basePath       != null) v.put("basePath",       basePath);
            if (externalBaseUrl     != null) v.put("externalBaseUrl", externalBaseUrl);
            if (externalAuthType    != null) v.put("externalAuthType", externalAuthType);
            if (externalHttpMethod  != null) v.put("externalHttpMethod", externalHttpMethod);
            if (externalPathTemplate!= null) v.put("externalPathTemplate", externalPathTemplate);
            if (timeoutMs != null)         v.put("timeoutMs", timeoutMs);
            if (retryMaxAttempts != null)  v.put("retryMaxAttempts", retryMaxAttempts);
            if (retryBackoffMs != null)    v.put("retryBackoffMs", retryBackoffMs);
            return v;
        }

        private static boolean isBlank(String s){ return s==null || s.trim().isEmpty(); }
    }
}
