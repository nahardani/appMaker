package com.company.appmaker.ai;

import com.company.appmaker.ai.draft.AiDraftStore;
import com.company.appmaker.ai.dto.ApplyRequest;
import com.company.appmaker.ai.dto.GenerateRequest;
import com.company.appmaker.ai.dto.GenerateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiFacade aiFacade;
    private final PlanParser planParser;
    private final CodeWriterService codeWriterService;
    private final PromptFactory promptFactory;
    private final AiRepairService aiRepairService;
    private final AiDraftStore aiDraftStore;

    /** مرحله ۱: تولید کد با AI و استش در حافظه */
    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody GenerateRequest req) {
        try {
            // 1) ساخت پرامپت محکم
            String prompt = promptFactory.forSpringScaffold(
                    req.prompt(), req.basePackage(), req.feature(), req.basePath(), req.javaVersion()
            );

            // 2) فراخوانی مدل
            String raw = aiFacade.generate(req.provider(), req.model(), prompt);

            // 3) پارس خروجی
            GenerateResult parsed = planParser.parse(raw);

            // 4) در صورت نیاز Repair
            boolean okLang = parsed.meta() != null &&
                    "java".equalsIgnoreCase(parsed.meta().language());
            if (!okLang || parsed.files() == null || parsed.files().isEmpty()) {
                parsed = aiRepairService.repairToJavaFiles(
                        raw, req.basePackage(), req.feature(), req.basePath(), req.javaVersion()
                );
            }

            // 5) ذخیره در حافظه به ازای projectId (تبدیل Long→String در صورت نیاز)
            if (req.projectId() != null && parsed.files() != null && !parsed.files().isEmpty()) {
                aiDraftStore.put(String.valueOf(req.projectId()), parsed.files());
            }

            // 6) پاسخ UI برای پیش‌نمایش
            return ResponseEntity.ok(parsed);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "AI generate failed", "details", ex.getMessage()));
        }
    }

    /** مرحله ۲ (اختیاری Legacy): نوشتن مستقیم فایل‌ها روی دیسک از طرف کلاینت */
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
}
