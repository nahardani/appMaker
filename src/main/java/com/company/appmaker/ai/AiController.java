package com.company.appmaker.ai;

import com.company.appmaker.ai.draft.AiDraftStore;
import com.company.appmaker.ai.dto.ApplyRequest;
import com.company.appmaker.ai.dto.CodeFile;
import com.company.appmaker.ai.dto.CodeMeta;
import com.company.appmaker.ai.dto.GenerateRequest;
import com.company.appmaker.ai.dto.GenerateResult;
import com.company.appmaker.ai.preflight.PreflightModels;
import com.company.appmaker.ai.preflight.PromptAnalyzer;
import com.company.appmaker.ai.preflight.PromptCompleter;
import com.company.appmaker.util.Utils;
import com.company.appmaker.enums.PromptTarget;
import com.company.appmaker.service.DomainPropertyService;
import com.company.appmaker.service.PromptRenderer;
import com.company.appmaker.service.PromptService;
import com.company.appmaker.service.ValueObjectService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.company.appmaker.util.Utils.camel;
import static com.company.appmaker.util.Utils.stripControllerSuffix;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiFacade aiFacade;
    private final PlanParser planParser;
    private final CodeWriterService codeWriterService;
    private final AiDraftStore aiDraftStore;
    private final PromptService promptService;
    private final PromptRenderer promptRenderer;
    private final DomainPropertyService domainPropertyService;
    private final ValueObjectService valueObjectService;
    private final ObjectMapper om = new ObjectMapper();

    // ============================
    //  A) GENERATE — ساده و پایدار
    // ============================
    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody GenerateRequest req) {
        try {
            // Gate
            if (req.controllerName() == null || req.controllerName().isBlank()
                    || req.endpointName() == null || req.endpointName().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "controllerName/endpointName is required before AI generation"
                ));
            }

            // 1) vars پایه
            Map<String, Object> vars = new HashMap<>();
            putIfNotBlank(vars, "basePackage", req.basePackage());
            putIfNotBlank(vars, "basePath", req.basePath());
            putIfNotBlank(vars, "projectId", req.projectId());
            vars.put("javaVersion", req.javaVersionSafe());
            vars.put("controllerName", req.controllerName().trim());
            vars.put("endpointName", req.endpointName().trim());
            vars.put("feature", stripControllerSuffix(req.controllerName().trim()));
            vars.put("basePackage|dotToSlash", req.basePackage().replace('.', '/'));
            vars.put("feature|UpperCamel", camel(stripControllerSuffix(req.controllerName().trim())));
            vars.put("endpointName|camelCase", camel(req.endpointName()));

            // واژگان دامنه
            vars.put("propsJson", toJson(domainPropertyService.listActiveForPrompt()));
            vars.put("voJson", toJson(valueObjectService.listActiveForPrompt()));

            // 2) ساخت sequence حداقلی و پرامپت پایه (بدون preflight)
            String javaVerStr = String.valueOf(req.javaVersionSafe());
            List<PromptTarget> seq = new ArrayList<>(List.of(
                    PromptTarget.CONTROLLER_METHOD,
                    PromptTarget.SERVICE_INTERFACE,
                    PromptTarget.SERVICE_IMPL
            ));

            String basePrompt = promptService.composeForTargets(
                    req.projectId(), javaVerStr, vars, seq, "ai-method-only"
            );
            if (basePrompt == null || basePrompt.isBlank()) {
                basePrompt = promptService.composeSpringScaffold(req.projectId(), javaVerStr, vars, "SPRING_SCAFFOLD");
            }

            // 3) الحاق پرامپت ذخیره‌شده/کاربر (اختیاری)
            if (req.promptId() != null && !req.promptId().isBlank()) {
                String savedRendered = promptService.renderBody(req.promptId(), vars);
                if (savedRendered != null && !savedRendered.isBlank()) {
                    basePrompt += "\n\n### EXTRA (Saved Prompt)\n" + savedRendered.trim() + "\n";
                }
            }
            if (req.prompt() != null && !req.prompt().isBlank()) {
                basePrompt += "\n\n### USER NOTE\n" + req.prompt().trim() + "\n";
            }

            // 4) فراخوانی مدل
            String raw = aiFacade.generate(req.provider(), req.model(), basePrompt);
            raw = PlanParser.cleanMarkdown(raw);

            var parsed = planParser.parse(raw);

            // 5) جمع‌آوری فایل‌ها
            List<CodeFile> files = new ArrayList<>();
            if (parsed != null && parsed.files() != null) files.addAll(parsed.files());

            if (files.isEmpty()) {
                files.addAll(extractFromRawJson(raw));
            }
            if (files.isEmpty()) {
                return ResponseEntity.status(422).body(Map.of(
                        "error", "No files found in model output",
                        "raw", raw
                ));
            }

            // فقط جاواها را به مرحلهٔ ترمیم بدهیم
            files.removeIf(f -> f == null || f.path() == null || !f.path().endsWith(".java"));
            if (files.isEmpty()) {
                return ResponseEntity.status(422).body(Map.of(
                        "error", "No Java files found after parsing",
                        "raw", raw
                ));
            }

            // 6) نرمال‌سازی
            var deterministic = Utils.run(
                    files,
                    raw,
                    req.basePackage() != null ? req.basePackage() : "com.example.app",
                    req.controllerName() != null && !req.controllerName().isBlank() ? req.controllerName() : "GeneratedController",
                    req.basePath(),
                    req.javaVersionSafe()
            );

            // 7) متادیتا + خروجی
            var old = (parsed != null ? parsed.meta() : null);
            int jv = req.javaVersionSafe();
            var meta = (old != null)
                    ? new CodeMeta("java", old.framework() != null ? old.framework() : "spring-boot",
                    old.javaVersion() > 0 ? old.javaVersion() : jv, old.module())
                    : new CodeMeta("java", "spring-boot", jv, null);

            var finalResult = new GenerateResult(
                    meta,
                    deterministic.files(),
                    (parsed != null && parsed.raw() != null) ? parsed.raw() : raw
            );

            if (finalResult.files() == null || finalResult.files().isEmpty()) {
                return ResponseEntity.status(422).body(Map.of(
                        "error", "No Java files found after normalization",
                        "raw", raw
                ));
            }

            // 8) stash
            if (req.projectId() != null) {
                aiDraftStore.put(req.projectId(), finalResult.files());
            }

            return ResponseEntity.ok(finalResult);

        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "AI generate failed",
                    "details", ex.getMessage()
            ));
        }
    }

    // =========================================
    //  B) PREFLIGHT — فقط بازنویسی/تکمیل پرامپت
    // =========================================
    @PostMapping("/preflight")
    public ResponseEntity<?> preflight(@RequestBody GenerateRequest req) {
        try {
            // Gate
            if (req.controllerName() == null || req.controllerName().isBlank()
                    || req.endpointName() == null || req.endpointName().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "controllerName/endpointName is required for preflight"
                ));
            }

            // vars پایه برای تحلیل
            Map<String, Object> vars = new HashMap<>();
            putIfNotBlank(vars, "basePackage", req.basePackage());
            putIfNotBlank(vars, "basePath", req.basePath());
            putIfNotBlank(vars, "projectId", req.projectId());
            vars.put("javaVersion", req.javaVersionSafe());
            vars.put("controllerName", req.controllerName().trim());
            vars.put("endpointName", req.endpointName().trim());
            vars.put("feature", stripControllerSuffix(req.controllerName().trim()));

            vars.put("propsJson", toJson(domainPropertyService.listActiveForPrompt()));
            vars.put("voJson", toJson(valueObjectService.listActiveForPrompt()));

            // ---- قبل از analyze/complete، هرچه کاربر فرستاده را داخل vars بریزیم
            if (req.needsExternal() != null) vars.put("needsExternal", req.needsExternal());
            if (req.needsDb() != null) vars.put("needsDb", req.needsDb());
            if (req.externalBaseUrl() != null) vars.put("externalBaseUrl", req.externalBaseUrl());
            if (req.externalPathTemplate() != null) vars.put("externalPathTemplate", req.externalPathTemplate());
            if (req.externalHttpMethod() != null) vars.put("externalHttpMethod", req.externalHttpMethod());
            if (req.externalAuthType() != null) vars.put("externalAuthType", req.externalAuthType());
            if (req.timeoutMs() != null) vars.put("timeoutMs", req.timeoutMs());
            if (req.retryMaxAttempts() != null) vars.put("retryMaxAttempts", req.retryMaxAttempts());
            if (req.retryBackoffMs() != null) vars.put("retryBackoffMs", req.retryBackoffMs());

// 2) آنالیز و تکمیل
            var analyzer = new PromptAnalyzer();
            var completer = new PromptCompleter();

            var pre = analyzer.analyze(req.prompt(), vars);
            var completed = completer.complete(pre, vars);

//// اگر هنوز گپ حیاتی داریم، ولی کاربر واقعاً مقداری از فیلدهای لازم را داده، دیگر 422 نده
//            boolean suppliedExternal =
//                    req.externalBaseUrl() != null ||
//                            req.externalPathTemplate() != null ||
//                            req.externalHttpMethod() != null ||
//                            req.externalAuthType() != null;
//
//            if (completed.hasBlockingGaps() && !suppliedExternal) {
//                return ResponseEntity.status(422).body(Map.of(
//                        "error", "Missing required inputs before prompt synthesis",
//                        "questions", completed.questions()
//                ));
//            }


            String synthesizedUserNote = synthesizeUserNoteOnly(req, completed);

            // این متنِ بازنویسی‌شده را برمی‌گردانیم تا در UI جایگزینِ textarea شود
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "prompt", synthesizedUserNote,
                    "vars", completed.toVars() // اگر خواستی نگه‌داری
            ));



        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "preflight failed",
                    "details", ex.getMessage()
            ));
        }
    }

    // ============================
    //  C) APPLY (بدون تغییر)
    // ============================
    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestBody ApplyRequest req) {
        try {
            int written = codeWriterService.writeAll(req.projectRoot(), req.files());
            return ResponseEntity.ok("✅ " + written + " file(s) written");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ write failed: " + ex.getMessage());
        }
    }

    // ============================
    //  Helpers
    // ============================
    private static void putIfNotBlank(Map<String, Object> m, String k, String v) {
        if (v != null && !v.isBlank()) m.put(k, v.trim());
    }

    private String toJson(Object o) {
        try {
            return om.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String buildSynthesizedPromptOnlyText(
            GenerateRequest req,
            com.company.appmaker.ai.preflight.PreflightModels.Preflight completed,
            Map<String, Object> vars
    ) {
        String controllerName = completed.controllerName != null && !completed.controllerName.isBlank()
                ? completed.controllerName
                : String.valueOf(vars.getOrDefault("controllerName", "GeneratedController"));
        String feature = stripControllerSuffix(controllerName);

        StringBuilder p = new StringBuilder();
        p.append("""
                You are an expert Spring Boot Java code generator.
                Follow each section's instructions to produce consistent, layered code.
                Output only Java files using the agreed file-splitting format.
                """);

        p.append("\n### NAMING & WIRING CONSTRAINTS\n")
                .append("- Base package: com.behsazan.customer\n")
                .append("- Controller simple name: ").append(feature).append("Controller\n")
                .append("- Controller base path: /api/customer\n")
                .append("- Add endpoint method: getFacilityInfo\n")
                .append("- Service interface FQN: com.behsazan.customer.service.").append(feature).append("Service\n")
                .append("- Service impl FQN: com.behsazan.customer.service.").append(feature).append("ServiceImpl\n")
                .append("- DTO package: com.behsazan.customer.dto\n");

        if (Boolean.TRUE.equals(completed.needsExternal)) {
            String extBase = completed.externalBaseUrl != null ? completed.externalBaseUrl : "http://api.example.com";
            String extMethod = completed.externalHttpMethod != null ? completed.externalHttpMethod : "GET";
            String extPath = completed.externalPathTemplate != null ? completed.externalPathTemplate : "/facilities/{loanId}";
            String extAuth = completed.externalAuthType != null ? completed.externalAuthType : "NONE";
            int timeout = completed.timeoutMs != null ? completed.timeoutMs : 5000;
            int retries = completed.retryMaxAttempts != null ? completed.retryMaxAttempts : 2;
            int backoffMs = completed.retryBackoffMs != null ? completed.retryBackoffMs : 200;

            p.append("\n### EXTERNAL CLIENT REQUIREMENTS\n")
                    .append("- needsExternal=true\n")
                    .append("- Client interface: ").append(feature).append("ExternalClient under com.behsazan.customer.client\n")
                    .append("- Impl: ").append(feature).append("ExternalClientImpl using Spring RestClient (Boot 3.2+)\n")
                    .append("- Provide RestClient bean in com.behsazan.customer.config.RestClientConfig\n")
                    .append("- Base URL: ").append(extBase).append("\n")
                    .append("- HTTP Method: ").append(extMethod).append("\n")
                    .append("- Path Template: ").append(extPath).append("\n")
                    .append("- Auth Type: ").append(extAuth).append("\n")
                    .append("- Timeout (ms): ").append(timeout).append("\n")
                    .append("- Retry: maxAttempts=").append(retries).append(", backoffMs=").append(backoffMs).append("\n")
                    .append("- On non-2xx: throw com.behsazan.customer.exception.")
                    .append(feature).append("ExternalException with useful context.\n");
        }

        if (Boolean.TRUE.equals(completed.needsDb)) {
            p.append("\n### DATABASE / STORED PROCEDURE\n")
                    .append("- needsDb=true\n")
                    .append("- Use JPA repository under com.behsazan.customer.repository (").append(feature).append("Repository)\n")
                    .append("- Read facility info via a stored procedure with inputs: (customerId, loanId)\n")
                    .append("- Return DTO: ").append(feature).append("Dto under com.behsazan.customer.dto\n")
                    .append("- ServiceImpl flow: call repository/SP → call ExternalClient to validate loanId → if valid, return data.\n");
        }

        if (req.prompt() != null && !req.prompt().isBlank()) {
            p.append("\n### USER NOTE\n").append(req.prompt().trim()).append("\n");
        }
        return p.toString();
    }

    private List<CodeFile> extractFromRawJson(String rawJson) {
        List<CodeFile> out = new ArrayList<>();
        if (rawJson == null || rawJson.isBlank()) return out;
        try {
            JsonNode root = om.readTree(rawJson);
            if (root.isObject() && root.has("files") && root.get("files").isArray()) {
                for (JsonNode n : root.get("files")) {
                    String path = n.hasNonNull("path") ? n.get("path").asText() : null;
                    String content = n.hasNonNull("content") ? n.get("content").asText() : null;
                    String lang = n.hasNonNull("language") ? n.get("language").asText() : guessLangByPath(path);
                    if (path != null && content != null) out.add(new CodeFile(path, lang, content));
                }
            }
        } catch (Exception ignore) {
            // no-op
        }
        return out;
    }

    private String guessLangByPath(String p) {
        if (p == null) return "txt";
        String low = p.toLowerCase();
        if (low.endsWith(".java")) return "java";
        if (low.endsWith(".md")) return "md";
        return "txt";
    }


    /**
     * فقط متنِ کاربر را به‌صورت یک پاراگراف فارسی بازنویسی/تکمیل می‌کند.
     * ورودی: متن خام کاربر + مقادیر تکمیلی (external/db) که از فرم گرفته‌ایم.
     * خروجی: فقط یک پاراگراف فارسی. بدون Markdown/کد/JSON.
     */
    private String synthesizeUserNoteOnly(
            GenerateRequest req,
            PreflightModels.Preflight completed
    ) {
        final String userNote = (req.prompt() != null) ? req.prompt().trim() : "";

        final boolean needsExternal = Boolean.TRUE.equals(completed.needsExternal);
        final boolean needsDb = Boolean.TRUE.equals(completed.needsDb);

        final String extBase = (completed.externalBaseUrl != null) ? completed.externalBaseUrl : "";
        final String extPath = (completed.externalPathTemplate != null) ? completed.externalPathTemplate : "";
        final String extMethod = (completed.externalHttpMethod != null) ? completed.externalHttpMethod : "";
        final String extAuth = (completed.externalAuthType != null) ? completed.externalAuthType : "";
        final Integer timeoutMs = completed.timeoutMs;
        final Integer retryMax = completed.retryMaxAttempts;
        final Integer retryBackoffMs = completed.retryBackoffMs;

        // دستور مینیمال: فقط همان متن کاربر را با جزئیات تکمیلی قلاب کن، خروجی = یک پاراگراف فارسی
        String instruction = "System:\n" +
                "تو یک «ویراستار و بازنویس\u200Cکنندهٔ فنی» هستی که به\u200Cصورت خاص در طراحی APIهای وب و مستندسازی بک\u200Cاند (Java / Spring Boot) تخصص داری. نقش تو: دریافت یک توصیف/پرامپت فنی فارسی، شناسایی نواقص، پیشنهاد اصلاحات دقیق و تولید نسخهٔ کامل و قابل\u200Cاجرا از متن با لحن رسمی و فنی است. همیشه نکات امنیتی، ورودی/خروجی، نام\u200Cگذاری متدها و مسیرهای endpoint، و نمونه\u200Cهای JSON پاسخ/درخواست را اضافه کن. خروجی باید به فارسی، ساختاریافته و قابل\u200Cاستفادهٔ مستقیم برای توسعه\u200Cدهنده باشد.\n" +
                "\n" +
                "User:\n" +
                "این متن را بررسی کن، سپس یک نسخهٔ کامل\u200Cشده و اصلاح\u200Cشده ارائه بده که شامل موارد زیر باشد:\n" +
                "2. نسخهٔ بازنویسی\u200Cشدهٔ متن (پارگراف کامل و رسمی).\n" +
//                "3. پیشنهاد دقیق برای:\n" +
//                "   - مسیر (endpoint) (مثال: `GET /api/loans/{customerId}/{loanId}`)\n" +
//                "   - نام متد کنترلر و سرویس (مثال: `getLoanInfo`, `loanService.getLoanByCustomerAndLoanId`)\n" +
//                "   - پارامترهای ورودی و نوعشان (path, query, body)\n" +
//                "   - ساختار JSON ورودی/خروجی با مثال (نمونهٔ درخواست و پاسخ)\n" +
//                "   - مکانیزم امنیتی پیشنهادی (مثال: اعتبارسنجی JWT + ROLE_CHECK)\n" +
//                "   - رفتار در حالات خطا (مثلاً اگر شناسه نامعتبر بود چه کدی و چه پیام)\n" +
                "   - نکات پیاده\u200Cسازی برای فراخوانی stored procedure و اکسترنال سرویس (timeout، retry، بررسی صحت پاسخ)\n" +
//                "4. تغییرات انجام\u200Cشده را به\u200Cصورت خلاصه (چه چیزهایی اضافه/تغییر/حذف شد) بیاور.\n" +
//                "5. اگر نیاز به پارامتر یا اطلاعات بیشتری هست، دقیقا بپرس (فقط در صورت ضروری).\n" +
                "\n" +
                "متن (ورودی):\n" +
                "\n" + userNote + "\n" +
                "\n" +
                "راهنمای خروجی:\n" +
                "- لحن رسمی و فنی باشد.\n" +
                "- حداکثر مفصل: تا زمانی که همهٔ موارد فوق پوشش داده شود.\n" ;
//                "- اگر پیشنهادی مفروضاتی دارد (مثلاً فرمت تاریخ یا کدهای خطا)، آن\u200Cها را واضح لیست کن.\n";

//        String instruction = """
//                لطفاً متن زیر را فقط بازنویسی و تکمیل کن. هدف: یک پاراگراف فارسیِ شفاف که همان درخواست کاربر را بیان کند و در صورت موجود بودن، جزئیات تکمیلی را در دل همان پاراگراف ادغام کند. از آوردن هرگونه قالب Markdown، JSON، لیست، تیتر، یا توضیحات اضافی خودداری کن. فقط یک پاراگراف نهایی بده.
//
//                متن کاربر:
//                ---
//                %s
//                ---
//
//                جزئیات تکمیلی (اگر مقداری تهی بود نادیده بگیر و ذکرش نکن):
//                - نیاز به سرویس خارجی: %s
//                - نام/عنوان سرویس/اندپوینت: %s
//                - آدرس پایه سرویس خارجی: %s
//                - الگوی مسیر: %s
//                - متد HTTP: %s
//                - احراز هویت: %s
//                - timeout(ms): %s
//                - retry: maxAttempts=%s, backoffMs=%s
//                - استفاده از Stored Procedure در دیتابیس: %s (ورودی‌ها: customerId و loanId)
//
//                خروجی: فقط یک پاراگراف فارسی تمیز (بدون هیچ قالب‌بندی یا توضیح اضافه).
//                """.formatted(
//                userNote.isBlank() ? "—" : userNote,
//                needsExternal ? "بله" : "خیر",
//                (req.endpointName() != null && !req.endpointName().isBlank()) ? req.endpointName().trim() : "",
//                extBase,
//                extPath,
//                extMethod,
//                (extAuth != null && !extAuth.isBlank()) ? extAuth : "",
//                (timeoutMs != null ? timeoutMs : ""),
//                (retryMax != null ? retryMax : ""),
//                (retryBackoffMs != null ? retryBackoffMs : ""),
//                needsDb ? "بله" : "خیر"
//        );

        try {
            String out = aiFacade.generate(req.provider(), req.model(), instruction);
            if (out != null) {
                out = out.trim()
                        .replaceAll("^```[a-zA-Z]*\\s*", "")
                        .replaceAll("\\s*```\\s*$", "")
                        .trim();
            }
            if (out != null && !out.isBlank()) {
                return out;
            }
        } catch (Exception ignore) {
        }

        // Fallback خیلی ساده: متن کاربر + الصاق جزئیات غیرخالی
        StringBuilder fb = new StringBuilder();
        fb.append(userNote.isBlank() ? "" : userNote);

        if (needsDb) {
            fb.append(fb.length() > 0 ? " " : "")
                    .append("دریافت داده از Stored Procedure با ورودی‌های customerId و loanId انجام شود.");
        }
        if (needsExternal) {
            fb.append(fb.length() > 0 ? " " : "")
                    .append("همچنین قبل از بازگشت پاسخ، صحت شناسه تسهیلات از سرویس خارجی");
            if (extBase != null && !extBase.isBlank()) fb.append(" به آدرس ").append(extBase);
            if (extMethod != null && !extMethod.isBlank()) fb.append(" با متد ").append(extMethod);
            if (extPath != null && !extPath.isBlank()) fb.append(" و مسیر ").append(extPath);
            if (extAuth != null && !extAuth.isBlank()) fb.append(" (احراز هویت: ").append(extAuth).append(")");
            if (timeoutMs != null) fb.append("، timeout=").append(timeoutMs).append("ms");
            if (retryMax != null || retryBackoffMs != null) {
                fb.append("، retry(")
                        .append("maxAttempts=").append(retryMax != null ? retryMax : "—")
                        .append(", backoffMs=").append(retryBackoffMs != null ? retryBackoffMs : "—")
                        .append(")");
            }
            fb.append(" انجام شود.");
        }

        return fb.toString().trim();
    }

}
