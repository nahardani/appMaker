package com.company.appmaker.ai;

import com.company.appmaker.ai.draft.AiDraftStore;
import com.company.appmaker.ai.dto.ApplyRequest;
import com.company.appmaker.ai.dto.GenerateRequest;
import com.company.appmaker.ai.dto.GenerateResult;
import com.company.appmaker.service.PromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    private final PromptService promptService;

    /** مرحله ۱: تولید کد با AI و استش در حافظه */
    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody GenerateRequest req) {
        try {
            String prompt;
            if (req.promptId() != null) {
                // 1) پرامپت ذخیره‌شده را بخوان و رندر کن
                Map<String,Object> vars = Map.of(
                        "basePackage", req.basePackage(),
                        "feature",     req.feature(),
                        "basePath",    req.basePath(),
                        "javaVersion", req.javaVersion(),
                        "projectId",   req.projectId()
                );
                var rendered = promptService.renderBody(req.promptId(), vars);
                prompt = rendered;
            } else {
                // 2) برگشت به پرامپت کارخانه‌ای فعلی
                prompt = promptFactory.forSpringScaffold(
                        req.prompt(), req.basePackage(), req.feature(), req.basePath(), req.javaVersion()
                );
            }

            String raw   = aiFacade.generate(req.provider(), req.model(), prompt);
            var parsed   = planParser.parse(raw);
            boolean ok   = parsed.meta()!=null && "java".equalsIgnoreCase(parsed.meta().language());
            if (!ok || parsed.files()==null || parsed.files().isEmpty()) {
                parsed = aiRepairService.repairToJavaFiles(raw, req.basePackage(), req.feature(), req.basePath(), req.javaVersion());
            }

            if (req.projectId()!=null && parsed.files()!=null && !parsed.files().isEmpty()) {
                aiDraftStore.put(req.projectId(), parsed.files()); // در حافظه ذخیره کن
            }

            return ResponseEntity.ok(parsed);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error","AI generate failed","details",ex.getMessage()));
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
