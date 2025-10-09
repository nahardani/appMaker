package com.company.appmaker.service;

import com.company.appmaker.model.Project;
import com.company.appmaker.model.ConstantsSettings;
import com.company.appmaker.model.externalApi.ExternalApisSettings;
import com.company.appmaker.model.l18n.I18nSettings;
import com.company.appmaker.model.logging.LoggingSettings;
import com.company.appmaker.model.profile.ProfileSettings;
import com.company.appmaker.model.security.SecuritySettings;
import com.company.appmaker.model.swagger.SwaggerSettings;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * مقداردهی هوشمند پیش‌فرض‌ها برای پروژه، بر اساس نسخهٔ جاوا و پکیج‌های انتخابی.
 * این کلاس فقط جاهای خالی را پر می‌کند (idempotent).
 */
@Service
public class DefaultProjectInitializer {

    /* ===== API ===== */

    /** همهٔ پیش‌فرض‌ها را در پروژه اعمال می‌کند (فقط اگر قبلاً خالی بوده باشند). */
    public void applyDefaults(Project p) {
        if (p == null) return;
        String java = (p.getJavaVersion()==null || p.getJavaVersion().isBlank()) ? "17" : p.getJavaVersion().trim();
        boolean boot3Like = isBoot3Line(java); // Jakarta در خروجی کُد تأثیر دارد، در تنظیمات کمتر.

        ensurePackagesList(p);               // تضمین لیست پکیج‌ها
        ensureProfiles(p);                   // dev/test/prod
        ensureConstants(p);                  // constants.properties (کلید/مقدار ساده)
        ensureI18n(p);                       // messages*.properties
        ensureLogging(p);                    // حداقل سطح روت
        ensureSwagger(p);                    // عنوان، نسخه، uiPath
        ensureSecurity(p);                   // AuthType NONE + لیست‌های خالی
        ensureExternalApis(p);               // لیست خالی

        // اگر خواستی بر اساس پکیج‌ها چیزهایی رو هم تزریق کنی، اینجا چک کن
        // مثلا اگر user "i18n" را برداشته باشد (ولی در طراحی‌مان جزو locked است)…
    }

    /* ===== Helpers ===== */

    private boolean isBoot3Line(String javaVersion) {
        // Boot 3 نیاز به Java 17+ دارد. Java 8/11 -> Boot 2.7.x
        try {
            int v = Integer.parseInt(javaVersion.replaceAll("[^0-9]", ""));
            return v >= 17;
        } catch (Exception ignore) { return true; }
    }

    private void ensurePackagesList(Project p) {
        if (p.getPackages() == null) p.setPackages(new ArrayList<>());
        // لیست پکیج‌ها جداگانه مدیریت می‌شود (در WizardController) ولی اینجا مطمئن می‌شویم Null نشود.
    }

    /* ---------- Profiles (dev/test/prod) ---------- */

    private void ensureProfiles(Project p) {
        if (p.getProfiles() == null) p.setProfiles(new ProfileSettings());
        // در پروژهٔ شما ProfileSettings می‌تواند به دو شکل نگهداری شود
        // (Map<String,Map<String,String>>) یا فیلدهای dev/test/prod از نوع Map.
        // این هِلپرها با Reflection هر دو حالت را پشتیبانی می‌کنند.
        Map<String, String> dev  = getOrCreateProfileMap(p.getProfiles(), "dev");
        Map<String, String> test = getOrCreateProfileMap(p.getProfiles(), "test");
        Map<String, String> prod = getOrCreateProfileMap(p.getProfiles(), "prod");

        // فقط کلیدهایی را که خالی هستند ست می‌کنیم:

        // --- DEV (H2 + port 8080) ---
        putIfAbsent(dev,  "server.port",                    "8080");
        putIfAbsent(dev,  "spring.datasource.url",          "jdbc:h2:mem:appdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        putIfAbsent(dev,  "spring.datasource.username",     "sa");
        putIfAbsent(dev,  "spring.datasource.password",     "");
        putIfAbsent(dev,  "spring.h2.console.enabled",      "true");
        putIfAbsent(dev,  "spring.jpa.hibernate.ddl-auto",  "none"); // یا update
        putIfAbsent(dev,  "spring.sql.init.mode",           "always");

        // --- TEST (H2 یا همان DEV) ---
        putIfAbsent(test, "server.port",                    "0");
        putIfAbsent(test, "spring.datasource.url",          "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        putIfAbsent(test, "spring.datasource.username",     "sa");
        putIfAbsent(test, "spring.datasource.password",     "");
        putIfAbsent(test, "spring.jpa.hibernate.ddl-auto",  "none");

        // --- PROD (جاهای خالی با TODO) ---
        putIfAbsent(prod, "server.port",                    "8080");
        putIfAbsent(prod, "spring.datasource.url",          "jdbc:postgresql://db:5432/appdb"); // ⇐ نمونه
        putIfAbsent(prod, "spring.datasource.username",     "app");
        putIfAbsent(prod, "spring.datasource.password",     "change-me");
        putIfAbsent(prod, "spring.jpa.hibernate.ddl-auto",  "none");

        // اگر ساختار ProfileSettings شما set دوباره لازم دارد:
        putBackProfileMap(p.getProfiles(), "dev",  dev);
        putBackProfileMap(p.getProfiles(), "test", test);
        putBackProfileMap(p.getProfiles(), "prod", prod);
    }

    private void putIfAbsent(Map<String, String> map, String key, String value) {
        if (!map.containsKey(key) || map.get(key) == null || map.get(key).isBlank()) {
            map.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String,String> getOrCreateProfileMap(Object profileSettings, String profile) {
        if (profileSettings == null) return new LinkedHashMap<>();

        // حالت 1: متدی مثل getProfiles(): Map<String,Map<String,String>>
        try {
            var m = profileSettings.getClass().getMethod("getProfiles");
            Object obj = m.invoke(profileSettings);
            if (obj instanceof Map) {
                Map<String, Map<String, String>> profiles = (Map<String, Map<String, String>>) obj;
                Map<String, String> kv = profiles.get(profile);
                if (kv == null) {
                    kv = new LinkedHashMap<>();
                    profiles.put(profile, kv);
                }
                return kv;
            }
        } catch (Exception ignored) {}

        // حالت 2: فیلد/متد dev/test/prod
        String getter = "get" + profile.substring(0,1).toUpperCase() + profile.substring(1).toLowerCase(Locale.ROOT);
        String setter = "set" + profile.substring(0,1).toUpperCase() + profile.substring(1).toLowerCase(Locale.ROOT);
        try {
            var mg = profileSettings.getClass().getMethod(getter);
            Object obj = mg.invoke(profileSettings);
            if (obj instanceof Map) {
                return (Map<String,String>) obj;
            } else if (obj == null) {
                var map = new LinkedHashMap<String,String>();
                try {
                    var ms = profileSettings.getClass().getMethod(setter, Map.class);
                    ms.invoke(profileSettings, map);
                } catch (Exception ignored2) { /* اگر ست‌تر نبود، بیخیال */ }
                return map;
            }
        } catch (Exception ignored) {}
        // fallback
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private void putBackProfileMap(Object profileSettings, String profile, Map<String,String> map) {
        if (profileSettings == null) return;
        // حالت 1: getProfiles()
        try {
            var m = profileSettings.getClass().getMethod("getProfiles");
            Object obj = m.invoke(profileSettings);
            if (obj instanceof Map) {
                ((Map<String, Map<String,String>>)obj).put(profile, map);
                return;
            }
        } catch (Exception ignored) {}
        // حالت 2: setDev/setTest/setProd
        String setter = "set" + profile.substring(0,1).toUpperCase() + profile.substring(1).toLowerCase(Locale.ROOT);
        try {
            var ms = profileSettings.getClass().getMethod(setter, Map.class);
            ms.invoke(profileSettings, map);
        } catch (Exception ignored) {}
    }

    /* ---------- Constants ---------- */

    private void ensureConstants(Project p) {
        if (p.getConstants() == null) p.setConstants(new ConstantsSettings());
        // اگر entries نال است خالی‌اش کن. (مقدار پیش‌فرض این بخش معمولاً خالی است)
        try {
            var g = p.getConstants().getClass().getMethod("getEntries");
            Object entries = g.invoke(p.getConstants());
            if (entries == null) {
                var s = p.getConstants().getClass().getMethod("setEntries", Map.class);
                s.invoke(p.getConstants(), new LinkedHashMap<String,String>());
            }
        } catch (Exception ignored) {}
    }

    /* ---------- i18n ---------- */

    private void ensureI18n(Project p) {
        if (p.getI18n() == null) p.setI18n(new I18nSettings());
        try {
            // baseName
            var gb = p.getI18n().getClass().getMethod("getBaseName");
            Object baseName = gb.invoke(p.getI18n());
            if (baseName == null || baseName.toString().isBlank()) {
                var sb = p.getI18n().getClass().getMethod("setBaseName", String.class);
                sb.invoke(p.getI18n(), "messages");
            }
            // locales
            var gl = p.getI18n().getClass().getMethod("getLocales");
            Object locs = gl.invoke(p.getI18n());
            if (!(locs instanceof List) || ((List<?>) locs).isEmpty()) {
                var sl = p.getI18n().getClass().getMethod("setLocales", List.class);
                sl.invoke(p.getI18n(), new ArrayList<>(List.of("fa","en")));
            }
            // defaultLocale
            try {
                var gd = p.getI18n().getClass().getMethod("getDefaultLocale");
                Object def = gd.invoke(p.getI18n());
                if (def == null || def.toString().isBlank()) {
                    var sd = p.getI18n().getClass().getMethod("setDefaultLocale", String.class);
                    sd.invoke(p.getI18n(), "fa");
                }
            } catch (NoSuchMethodException ignore) {
                // اگر مدل شما نام دیگری دارد (مثل defaultLang) همین‌جا ست کنید
                try {
                    var gd2 = p.getI18n().getClass().getMethod("getDefaultLang");
                    Object def2 = gd2.invoke(p.getI18n());
                    if (def2 == null || def2.toString().isBlank()) {
                        var sd2 = p.getI18n().getClass().getMethod("setDefaultLang", String.class);
                        sd2.invoke(p.getI18n(), "fa");
                    }
                } catch (Exception ignored2) {}
            }
            // keys (initial)
            try {
                var gk = p.getI18n().getClass().getMethod("getKeys");
                Object keys = gk.invoke(p.getI18n());
                if (!(keys instanceof List) || ((List<?>) keys).isEmpty()) {
                    // اگر مدل کلید/ترجمه دارید، می‌توانید یک نمونهٔ خالی ست کنید
                    var sk = p.getI18n().getClass().getMethod("setKeys", List.class);
                    sk.invoke(p.getI18n(), new ArrayList<>());
                }
            } catch (NoSuchMethodException ignored) {}
        } catch (Exception ignored) {}
    }

    /* ---------- Logging ---------- */

    private void ensureLogging(Project p) {
        if (p.getLogging() == null) p.setLogging(new LoggingSettings());
        try {
            var gl = p.getLogging().getClass().getMethod("getLevels");
            Object levels = gl.invoke(p.getLogging());
            if (!(levels instanceof Map) || ((Map<?,?>)levels).isEmpty()) {
                var sl = p.getLogging().getClass().getMethod("setLevels", Map.class);
                Map<String,String> mp = new LinkedHashMap<>();
                mp.put("ROOT", "INFO");
                sl.invoke(p.getLogging(), mp);
            }
        } catch (Exception ignored) {}
    }

    /* ---------- Swagger ---------- */

    private void ensureSwagger(Project p) {
        if (p.getSwagger() == null) p.setSwagger(new SwaggerSettings());
        try {
            var gEn = p.getSwagger().getClass().getMethod("getEnabled");
            Object en = gEn.invoke(p.getSwagger());
            if (!(en instanceof Boolean)) {
                var sEn = p.getSwagger().getClass().getMethod("setEnabled", Boolean.class);
                sEn.invoke(p.getSwagger(), Boolean.TRUE);
            }
            putIfBlank(p.getSwagger(), "getTitle", "setTitle", (p.getProjectName()==null?"Project":"%s API".formatted(p.getProjectName())));
            putIfBlank(p.getSwagger(), "getVersion", "setVersion", "v1");
            putIfBlank(p.getSwagger(), "getUiPath", "setUiPath", "/swagger-ui");
            // defaultGroup اختیاری
        } catch (Exception ignored) {}
    }

    private void putIfBlank(Object bean, String getter, String setter, String value) {
        try {
            var g = bean.getClass().getMethod(getter);
            Object v = g.invoke(bean);
            if (v == null || v.toString().isBlank()) {
                var s = bean.getClass().getMethod(setter, String.class);
                s.invoke(bean, value);
            }
        } catch (Exception ignored) {}
    }

    /* ---------- Security ---------- */

    private void ensureSecurity(Project p) {
        if (p.getSecurity() == null) p.setSecurity(new SecuritySettings());
        // به‌صورت پیش‌فرض NONE (ورود آزاد)؛ نقش‌ها/قوانین خالی.
        try {
            var gType = p.getSecurity().getClass().getMethod("getAuthType");
            Object type = gType.invoke(p.getSecurity());
            if (type == null) {
                var sType = p.getSecurity().getClass().getMethod("setAuthType", SecuritySettings.AuthType.class);
                sType.invoke(p.getSecurity(), SecuritySettings.AuthType.NONE);
            }
        } catch (Exception ignored) {}
        // بقیهٔ فیلدها را خالی می‌گذاریم؛ کاربر بعداً می‌تواند تنظیم کند.
    }

    /* ---------- External APIs ---------- */

    private void ensureExternalApis(Project p) {
        if (p.getExternalApis() == null) p.setExternalApis(new ExternalApisSettings());
        // clients خالی بماند تا بعداً اضافه شود.
    }
}
