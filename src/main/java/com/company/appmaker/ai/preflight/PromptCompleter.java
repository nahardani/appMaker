package com.company.appmaker.ai.preflight;

import java.util.Locale;
import java.util.Map;

import static com.company.appmaker.ai.preflight.PreflightModels.*;

/**
 * تکمیل نهایی Spec با احترام به vars:
 * - نهایی‌سازی controllerName / endpointName / basePath
 * - پر کردن فیلدهای اکسترنال از vars اگر Analyzer پر نکرده بود
 * - ست‌کردن defaults (timeout/retry/auth/method)
 * - ساختن questions در صورت کمبود حیاتی و برگشت 422 از generate
 */
public class PromptCompleter {

    public Preflight complete(Preflight in, Map<String,Object> vars){
        if (in == null) in = new Preflight();
        Preflight out = in;

        // ===== تزریق/پرکردن از vars (اگر خالی است) =====
        if (isBlank(out.controllerName)) out.controllerName = str(vars.get("controllerName"));
        if (isBlank(out.endpointName))   out.endpointName   = str(vars.get("endpointName"));
        if (isBlank(out.basePath))       out.basePath       = str(vars.get("basePath"));

        if (isBlank(out.externalBaseUrl))      out.externalBaseUrl      = str(vars.get("externalBaseUrl"));
        if (isBlank(out.externalAuthType))     out.externalAuthType     = up(str(vars.get("externalAuthType")));
        if (isBlank(out.externalHttpMethod))   out.externalHttpMethod   = up(str(vars.get("externalHttpMethod")));
        if (isBlank(out.externalPathTemplate)) out.externalPathTemplate = str(vars.get("externalPathTemplate"));

        if (out.timeoutMs == null)        out.timeoutMs        = intval(vars.get("timeoutMs"));
        if (out.retryMaxAttempts == null) out.retryMaxAttempts = intval(vars.get("retryMaxAttempts"));
        if (out.retryBackoffMs == null)   out.retryBackoffMs   = intval(vars.get("retryBackoffMs"));

        // ===== Defaults =====
        if (out.needsExternal) {
            if (isBlank(out.externalHttpMethod))   out.externalHttpMethod   = "GET";
            if (isBlank(out.externalAuthType))     out.externalAuthType     = "NONE";
            if (out.timeoutMs == null)             out.timeoutMs            = 5000;
            if (out.retryMaxAttempts == null)      out.retryMaxAttempts     = 2;
            if (out.retryBackoffMs == null)        out.retryBackoffMs       = 200;
        }

        // ===== نرمال‌سازی نام‌ها =====
        if (!isBlank(out.controllerName)) out.controllerName = ensureControllerName(out.controllerName);
        if (!isBlank(out.endpointName))   out.endpointName   = camel(out.endpointName);
        // basePath را دست نمی‌زنیم—همان رشته ورودی

        // ===== ساخت سوالات در صورت کمبود حیاتی =====
        if (out.needsExternal) {
            if (isBlank(out.externalBaseUrl)) {
                out.questions().add(new Question("externalBaseUrl","External Base URL","text",true)
                        .ph("https://ext.example.com"));
            }
            if (isBlank(out.externalAuthType)) {
                out.questions().add(new Question("externalAuthType","Auth Type","select",true)
                        .opts("NONE","BASIC","BEARER","API_KEY"));
            }
            if (isBlank(out.externalHttpMethod)) {
                out.questions().add(new Question("externalHttpMethod","HTTP Method","select",true)
                        .opts("GET","POST","PUT","PATCH","DELETE"));
            }
            if (isBlank(out.externalPathTemplate)) {
                out.questions().add(new Question("externalPathTemplate","Path Template","text",true)
                        .ph("/facilities/{facilityId}"));
            }
        }

        return out;
    }

    // ===== Helpers =====
    private static String ensureControllerName(String name) {
        String base = toUpperCamel(stripSuffixIgnoreCase(name,"Controller"));
        return base + "Controller";
    }

    private static String camel(String s) {
        String uc = toUpperCamel(s);
        return Character.toLowerCase(uc.charAt(0)) + uc.substring(1);
    }

    private static String toUpperCamel(String raw) {
        if (isBlank(raw)) return "Generated";
        String[] parts = raw.replaceAll("[^A-Za-z0-9]+"," ").trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
        }
        return sb.length()==0 ? "Generated" : sb.toString();
    }

    private static String stripSuffixIgnoreCase(String s, String suffix) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT))
                ? s.substring(0, s.length()-suffix.length())
                : s;
    }

    private static String up(String s){ return s==null? null : s.trim().toUpperCase(Locale.ROOT); }
    private static String str(Object o){ return o==null? null : String.valueOf(o); }
    private static Integer intval(Object o){
        if (o==null) return null;
        try { return o instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(o)); }
        catch(Exception e){ return null; }
    }
    private static boolean isBlank(String s){ return s==null || s.trim().isEmpty(); }
}
