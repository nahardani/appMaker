package com.company.appmaker.ai;

import com.company.appmaker.ai.draft.AiDraftStore;
import com.company.appmaker.ai.dto.*;
import com.company.appmaker.ai.util.CodeRepairPipeline;
import com.company.appmaker.service.DomainPropertyService;
import com.company.appmaker.service.PromptService;
import com.company.appmaker.service.ValueObjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.company.appmaker.enums.PromptTarget;

import static com.company.appmaker.ai.util.Utils.stripControllerSuffix;


@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiFacade aiFacade;
    private final PlanParser planParser;
    private final CodeWriterService codeWriterService;
    private final AiDraftStore aiDraftStore;
    private final PromptService promptService;
    private final DomainPropertyService domainPropertyService;
    private final ValueObjectService valueObjectService;
    private final com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();


    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody GenerateRequest req) {
        try {
            // 0) Gate: قبل از تولید باید controllerName و endpointName داشته باشیم
            if (req.controllerName() == null || req.controllerName().isBlank()
                    || req.endpointName() == null || req.endpointName().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "controllerName/endpointName is required before AI generation"
                ));
            }

            // 1) جمع‌آوری متغیرها برای رندر پرامپت‌ها
            Map<String, Object> vars = new HashMap<>();
            if (req.basePackage() != null) vars.put("basePackage", req.basePackage());
            if (req.basePath()    != null) vars.put("basePath",    req.basePath());
            if (req.projectId()   != null) vars.put("projectId",   req.projectId());
            vars.put("javaVersion",    req.javaVersionSafe());
            vars.put("controllerName", req.controllerName().trim());
            vars.put("endpointName",   req.endpointName().trim());

            // اگر نیاز داری واژگان دامنه را تزریق کنی اینجا اضافه کن:
             vars.put("propsJson", toJson(domainPropertyService.listActiveForPrompt()));
             vars.put("voJson",    toJson(valueObjectService.listActiveForPrompt()));

            var feature = stripControllerSuffix(req.controllerName().trim());
            vars.put("feature", feature);

            // 2) پرامپت پایه: تلاش برای زنجیرهٔ سطح-متد (CONTROLLER_METHOD, SERVICE_IMPL)
            String javaVerStr = String.valueOf(req.javaVersionSafe());
            String basePrompt = promptService.composeForTargets(
                    req.projectId(),
                    javaVerStr,
                    vars,
                    List.of(PromptTarget.CONTROLLER_METHOD, PromptTarget.SERVICE_IMPL),
                    "ai-method-only"
            );

            // اگر سازمان PromptTarget هنوز «سطح-متد» را ندارد، fallback به اسافولد استاندارد
            if (basePrompt == null || basePrompt.isBlank()) {
                basePrompt = promptService.composeSpringScaffold(req.projectId(), javaVerStr, vars, null);
            }

            // 3) اگر کاربر "پرامپت ذخیره‌شده" انتخاب کرده بود → append (اختیاری)
            if (req.promptId() != null && !req.promptId().isBlank()) {
                String savedRendered = promptService.renderBody(req.promptId(), vars);
                if (savedRendered != null && !savedRendered.isBlank()) {
                    basePrompt += "\n\n### EXTRA (Saved Prompt)\n" + savedRendered.trim() + "\n";
                }
            }

            // 4) اگر کاربر متن دستی وارد کرده بود → append (اختیاری)
            if (req.prompt() != null && !req.prompt().isBlank()) {
                basePrompt += "\n\n### USER NOTE\n" + req.prompt().trim() + "\n";
            }

            // 5) فراخوانی مدل
            String raw = aiFacade.generate(req.provider(), req.model(), basePrompt);
            raw = PlanParser.cleanMarkdown(raw);

            var parsed = planParser.parse(raw);

            // 6) نرمال‌سازی خروجی (همان پایپ‌لاین قبلیِ پروژهٔ تو)
            var deterministic = CodeRepairPipeline.run(
                    parsed.files(),
                    raw,
                    (req.basePackage() != null ? req.basePackage() : "com.example.app"),
                    // اینجا controllerName را می‌دهیم تا feature/service هم درست نام‌گذاری شوند
                    (req.controllerName() != null && !req.controllerName().isBlank())
                            ? req.controllerName()
                            : "GeneratedController",
                    req.basePath(),
                    req.javaVersionSafe()
            );

            // 7) متادیتا
            var old = (parsed != null ? parsed.meta() : null);
            int jv = req.javaVersionSafe();
            var meta = (old != null)
                    ? new CodeMeta(
                    "java",
                    old.framework() != null ? old.framework() : "spring-boot",
                    old.javaVersion() > 0 ? old.javaVersion() : jv,
                    old.module()
            )
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

            // 8) پیش‌نویس پروژه (stash)
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





    /**
     * مرحله ۲ (اختیاری Legacy): نوشتن مستقیم فایل‌ها روی دیسک از طرف کلاینت
     */
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


    private static String compactJson(Object o) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String toJson(Object o) {
        try {
            return om.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }


}
