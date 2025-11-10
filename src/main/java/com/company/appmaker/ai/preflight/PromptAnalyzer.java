package com.company.appmaker.ai.preflight;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.company.appmaker.ai.preflight.PreflightModels.Preflight;

/**
 * تحلیل سبک متن کاربر و vars:
 * - تشخیص needsExternal / needsDb
 * - استخراج URL و متد HTTP در صورت وجود
 * - تشخیص نوع احراز هویت (Bearer/Basic/API Key)
 * - دست نزدن به نام‌ها/مسیر مگر برای پرکردن اولیه
 */
public class PromptAnalyzer {

    // الگوهای ساده استخراج
    private static final Pattern URL_METHOD = Pattern.compile("(?i)\\b(GET|POST|PUT|PATCH|DELETE)\\s+(https?://[^\\s\"'\\)]+)");
    private static final Pattern BARE_URL   = Pattern.compile("(?i)\\bhttps?://[^\\s\"'\\)]+");
    private static final Pattern BEARER     = Pattern.compile("(?i)\\bBearer\\b");
    private static final Pattern BASIC      = Pattern.compile("(?i)\\bBasic\\b");
    private static final Pattern API_KEY    = Pattern.compile("(?i)\\bAPI[_\\s-]?KEY\\b");

    public Preflight analyze(String userPrompt, Map<String,Object> vars){
        Preflight p = new Preflight();
        String text = userPrompt == null ? "" : userPrompt.trim();

        // از UI اگر needsExternal/needsDb آمده، احترام بگذار
        Object ne = vars.get("needsExternal");
        if (ne instanceof Boolean) p.needsExternal = (Boolean) ne;

        Object nd = vars.get("needsDb");
        if (nd instanceof Boolean) p.needsDb = (Boolean) nd;

        // تحلیل خیلی ساده متن کاربر
        if (!text.isEmpty()) {
            String low = text.toLowerCase(Locale.ROOT);
            if (containsAny(low, "سرویس خارجی", "external service", "external api", "api خارجی", "استعلام", "external")) {
                p.needsExternal = true;
            }
            if (containsAny(low, "استور پروسیجر", "stored procedure", "repository", "jpa", "بانک اطلاعاتی", "دیتابیس", "database", "db")) {
                p.needsDb = true;
            }
        }

        // پر کردن اولیه نام‌ها از vars (نرمال‌سازی نکن؛ Completer انجام می‌دهد)
        p.controllerName = str(vars.get("controllerName"));
        p.endpointName   = str(vars.get("endpointName"));
        p.basePath       = str(vars.get("basePath"));

        // اگر سرویس خارجی لازم است، تلاش برای استخراج URL/Method/Auth
        if (p.needsExternal) {
            // اگر از قبل vars چیزی داده، Analyzer override نکند (Completer تصمیم می‌گیرد)
            String givenUrl    = str(vars.get("externalBaseUrl"));
            String givenAuth   = str(vars.get("externalAuthType"));
            String givenMethod = str(vars.get("externalHttpMethod"));
            String givenPath   = str(vars.get("externalPathTemplate"));

            // فقط وقتی مقدار نداریم از متن استخراج می‌کنیم
            String method = null, baseUrl = null, path = null, auth = null;

            // 1) METHOD + URL در یک جمله
            Matcher um = URL_METHOD.matcher(text);
            if (um.find()) {
                method = up(um.group(1));
                String full = um.group(2);
                String[] split = splitUrl(full);
                baseUrl = split[0];
                path    = split[1];
            } else {
                // 2) URL تنها
                Matcher bu = BARE_URL.matcher(text);
                if (bu.find()) {
                    String full = bu.group();
                    String[] split = splitUrl(full);
                    baseUrl = split[0];
                    path    = split[1];
                    method  = guessMethodByContext(text);
                }
            }

            // 3) نوع احراز هویت از متن
            if (BEARER.matcher(text).find())      auth = "BEARER";
            else if (BASIC.matcher(text).find())  auth = "BASIC";
            else if (API_KEY.matcher(text).find()) auth = "API_KEY";
            else                                   auth = "NONE";

            // اعمال به Preflight فقط اگر از vars نیامده باشد
            if (isBlank(givenUrl))    p.externalBaseUrl      = baseUrl;
            if (isBlank(givenPath))   p.externalPathTemplate = path;
            if (isBlank(givenMethod)) p.externalHttpMethod   = method;
            if (isBlank(givenAuth))   p.externalAuthType     = auth;
        }

        return p;
    }

    // ===== Helpers =====
    private static boolean containsAny(String low, String... keys) {
        for (String k : keys) {
            if (low.contains(k.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    /** full → [baseUrl, path] */
    private static String[] splitUrl(String full) {
        if (full == null || full.isBlank()) return new String[]{"", ""};
        int schemeIdx = full.indexOf("://");
        int startHost = schemeIdx >= 0 ? schemeIdx + 3 : 0;
        int idx = full.indexOf("/", startHost);
        if (idx > 0) {
            return new String[]{ full.substring(0, idx), full.substring(idx) };
        }
        return new String[]{ full, "/" };
    }

    private static String guessMethodByContext(String text) {
        String low = text.toLowerCase(Locale.ROOT);
        if (low.contains("استعلام") || low.contains("query") || low.contains("اطلاعات") || low.contains("get"))
            return "GET";
        if (low.contains("ثبت") || low.contains("ایجاد") || low.contains("create") || low.contains("post"))
            return "POST";
        return "GET";
    }

    private static String up(String s){ return s==null? null : s.trim().toUpperCase(Locale.ROOT); }
    private static String str(Object o){ return o==null ? null : String.valueOf(o); }
    private static boolean isBlank(String s){ return s==null || s.trim().isEmpty(); }
}
