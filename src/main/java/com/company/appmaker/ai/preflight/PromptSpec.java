package com.company.appmaker.ai.preflight;


import java.util.*;

public class PromptSpec {
    public boolean needsDb;
    public String dbType; // "jpa" | "mongo" | null
    public boolean needsExternal;

    public ExternalSpec external; // null if not needed

    public String controllerName; // e.g., AccountController (یا خام)
    public String endpointName;   // e.g., getAccount
    public String basePath;       // e.g., /api/accounts

    public MethodSig serviceMethod; // نام/پارامترها/خروجی

    public String requestSchemaJson;  // optional
    public String responseSchemaJson; // optional

    /** کمبودهای مسدودکننده یا اختیاری برای UI */
    public final List<Gap> gaps = new ArrayList<>();

    public boolean hasBlockingGaps() {
        return gaps.stream().anyMatch(g -> g.required);
    }

    /** تبدیل به map برای تزریق در پرامپت‌ها */
    public Map<String, Object> toVars() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("needsDb", needsDb);
        if (dbType != null) m.put("dbType", dbType);

        m.put("needsExternal", needsExternal);
        if (external != null) {
            if (external.baseUrl != null)     m.put("externalBaseUrl", external.baseUrl);
            if (external.path != null)        m.put("externalPath", external.path);
            if (external.method != null)      m.put("externalHttpMethod", external.method);
            if (external.authType != null)    m.put("externalAuthType", external.authType);
            if (external.timeoutMs != null)   m.put("timeoutMs", external.timeoutMs);
            if (external.retryMax != null)    m.put("retryMax", external.retryMax);
            if (external.retryBackoffMs != null) m.put("retryBackoffMs", external.retryBackoffMs);
            if (!external.headers.isEmpty())  m.put("externalHeadersJson", external.headersJson());
        }

        if (controllerName != null) m.put("controllerName", controllerName);
        if (endpointName != null)   m.put("endpointName", endpointName);
        if (basePath != null)       m.put("basePath", basePath);

        if (serviceMethod != null) {
            m.put("serviceMethodName", serviceMethod.name);
            m.put("serviceMethodReturn", serviceMethod.returnType);
            m.put("serviceMethodParamsJson", serviceMethod.paramsJson());
        }

        if (requestSchemaJson != null)  m.put("requestSchemaJson", requestSchemaJson);
        if (responseSchemaJson != null) m.put("responseSchemaJson", responseSchemaJson);

        // کمک به تارگت‌های EXTERNAL_CLIENT
        if (needsExternal && (external == null || external.baseUrl == null || external.baseUrl.isBlank())) {
            // اجازه می‌دهیم پرامپت بفهمد هنوز baseUrl نداریم (برای پیام مناسب)
            m.put("externalBaseUrl", "");
        }

        return m;
    }

    public List<Question> questions() {
        List<Question> qs = new ArrayList<>();
        for (Gap g : gaps) {
            qs.add(new Question(g.key, g.question, g.required, g.hint));
        }
        return qs;
    }

    // ==== انواع کمکی ====
    public static class Gap {
        public final String key;
        public final String question;
        public final boolean required;
        public final String hint;

        public Gap(String key, String question, boolean required, String hint) {
            this.key = key;
            this.question = question;
            this.required = required;
            this.hint = hint;
        }
    }

    public static class Question {
        public final String key;
        public final String question;
        public final boolean required;
        public final String hint;

        public Question(String key, String question, boolean required, String hint) {
            this.key = key;
            this.question = question;
            this.required = required;
            this.hint = hint;
        }
    }
}
