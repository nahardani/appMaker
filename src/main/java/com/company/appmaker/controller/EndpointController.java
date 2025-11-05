package com.company.appmaker.controller;


import com.company.appmaker.ai.draft.AiDraftStore;
import com.company.appmaker.ai.dto.AiArtifact;
import com.company.appmaker.ai.dto.CodeFile;
import com.company.appmaker.model.Project;
import com.company.appmaker.model.coctroller.ControllerDef;
import com.company.appmaker.model.coctroller.EndpointDef;
import com.company.appmaker.repo.ProjectRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.company.appmaker.ai.util.Utils;

import static com.company.appmaker.ai.util.Utils.*;

@RestController
@RequestMapping(path = "/wizard/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class EndpointController {

    private final ProjectRepository repo;
    private final AiDraftStore aiDraftStore;

    // ---------- 1) List endpoints (JSON) ----------
    @GetMapping("/controllers/{ctrlName}/endpoints/json")
    public List<EndpointDef> listEndpoints(@PathVariable String id,
                                           @PathVariable String ctrlName) {
        var p = getProjectOr404(id);
        var ctrl = findController(p, ctrlName);
        if (ctrl == null || ctrl.getEndpoints() == null) return List.of();
        return ctrl.getEndpoints();
    }

    // (اختیاری) فقط نام‌ها — اگر جایی لازم داشته باشی
    @GetMapping("/controllers/{ctrlName}/endpoints/list")
    public List<String> listEndpointNames(@PathVariable String id,
                                          @PathVariable String ctrlName) {
        return listEndpoints(id, ctrlName).stream()
                .filter(Objects::nonNull)
                .map(EndpointDef::getName)
                .collect(Collectors.toList());
    }

    // ---------- 2) Add / Edit endpoint ----------
    @PostMapping("/controllers/{ctrlName}/endpoints")
    public Map<String, Object> upsertEndpoint(@PathVariable String id,
                                              @PathVariable String ctrlName,
                                              @RequestBody EpUpsert req) {
        var p = getProjectOr404(id);
        var ctrl = ensureController(p, ctrlName);

        // validation
        if (!StringUtils.hasText(req.name) ||
                !StringUtils.hasText(req.httpMethod) ||
                !StringUtils.hasText(req.path)) {
            return Map.of("ok", false, "message", "نام/متد/مسیر الزامی است.");
        }

        if (ctrl.getEndpoints() == null) ctrl.setEndpoints(new ArrayList<>());

        var ep = new EndpointDef();
        ep.setName(req.name.trim());
        ep.setHttpMethod(req.httpMethod.trim());
        ep.setPath(req.path.trim());

        if (req.index == null) {
            // create
            boolean nameExists = ctrl.getEndpoints().stream()
                    .filter(Objects::nonNull)
                    .anyMatch(e -> req.name.equals(e.getName()));
            if (nameExists) return Map.of("ok", false, "message", "اندپوینتی با این نام وجود دارد.");
            ctrl.getEndpoints().add(ep);
        } else {
            // update by index
            if (req.index < 0 || req.index >= ctrl.getEndpoints().size())
                return Map.of("ok", false, "message", "Index out of range");
            ctrl.getEndpoints().set(req.index, ep);
        }

        repo.save(p);
        return Map.of("ok", true);
    }
    @Data
    public static class EpUpsert {
        public Integer index;     // null => create
        public String  name;
        public String  httpMethod;
        public String  path;
    }

    // ---------- 3) Delete endpoint (by index) ----------
    @DeleteMapping("/controllers/{ctrlName}/endpoints/{index}")
    public Map<String, Object> deleteEndpoint(@PathVariable String id,
                                              @PathVariable String ctrlName,
                                              @PathVariable int index) {
        var p = getProjectOr404(id);
        var ctrl = findController(p, ctrlName);
        if (ctrl == null || ctrl.getEndpoints() == null)
            return Map.of("ok", false, "message", "Controller/endpoint list not found");

        if (index < 0 || index >= ctrl.getEndpoints().size())
            return Map.of("ok", false, "message", "Endpoint not found");

        ctrl.getEndpoints().remove(index);
        repo.save(p);
        return Map.of("ok", true);
    }

    @ResponseBody
    @PostMapping("/controllers/{ctrlName}/endpoints/{epName}/ai/commit")
    public Map<String, Object> commitEpAi(@PathVariable String id,
                                          @PathVariable String ctrlName,
                                          @PathVariable String epName,
                                          @RequestParam(value = "clearStash", defaultValue = "true") boolean clearStash) {

        var p = repo.findById(id).orElse(null);
        if (p == null) return Map.of("ok", false, "message", "Project not found");

        var ctrl = (p.getControllers() == null) ? null :
                p.getControllers().stream()
                        .filter(c -> c != null && ctrlName.equals(c.getName()))
                        .findFirst()
                        .orElse(null);
        if (ctrl == null) return Map.of("ok", false, "message", "Controller not found");

        var ep = (ctrl.getEndpoints() == null) ? null :
                ctrl.getEndpoints().stream()
                        .filter(e -> e != null && epName.equals(e.getName()))
                        .findFirst()
                        .orElse(null);
        if (ep == null) return Map.of("ok", false, "message", "Endpoint not found");

        var draft = aiDraftStore.get(id); // List<CodeFile>
        if (draft == null || draft.isEmpty()) {
            return Map.of("ok", false, "message", "هیچ فایل AI برای commit وجود ندارد.");
        }

        if (ep.getAiArtifacts() == null) {
            ep.setAiArtifacts(new ArrayList<>());
        }

        final String basePackage = resolveBasePackage(p);
        final String pkgPath     = basePackage.replace('.', '/');

        // نام‌ها را استاندارد کنیم
        String normalizedCtrl = Utils.ensureControllerName(ctrl.getName());
        String feature        = Utils.stripControllerSuffix(normalizedCtrl);
        String svcSimple      = Utils.ensureServiceName(feature);
        String implSimple     = Utils.ensureServiceImplName(feature);

        // مسیرهای استاندارد
        final String controllerPath  = "src/main/java/" + pkgPath + "/controller/" + normalizedCtrl + ".java";
        final String servicePath     = "src/main/java/" + pkgPath + "/service/"    + svcSimple  + ".java";
        final String serviceImplPath = "src/main/java/" + pkgPath + "/service/"    + implSimple + ".java";

        var previewFiles   = new ArrayList<Project.GeneratedFile>();
        int addedArtifacts = 0;

        for (var f : draft) {
            if (f == null || f.path() == null || f.content() == null) continue;

            final String path    = f.path();
            final String content = f.content();

            // 0) اگر فایل تست است: به آرتیفکت اضافه نمی‌کنیم؛ فقط برای UI نشان می‌دهیم
            if (Utils.isTestFile(path, content)) {
                previewFiles.add(new Project.GeneratedFile(
                        Utils.normalizeTestPath(path, pkgPath),
                        content
                ));
                continue;
            }

            // 1) حالت artifact قدیمی
            if (path.startsWith("__ARTIFACT__/")) {
                final String rawType = path.substring("__ARTIFACT__/".length());
                final String type    = Utils.normalizeAiArtifactType(rawType);

                String hint = null;
                String previewTargetPath = null;
                String tagged = content;

                switch (type) {
                    case "controller-method" -> {
                        hint = controllerPath;
                        previewTargetPath = controllerPath + "  (method: " + epName + ")";
                        tagged = Utils.wrapControllerTagged(Utils.ensureOnlyMethod(content, epName));
                    }
                    case "service-method" -> {
                        hint = servicePath;
                        previewTargetPath = servicePath + "  (method: " + epName + ")";
                        tagged = Utils.wrapServiceTagged(Utils.ensureOnlyMethod(content, epName));
                    }
                    case "service-impl-method" -> {
                        hint = serviceImplPath;
                        previewTargetPath = serviceImplPath + "  (method: " + epName + ")";
                        tagged = Utils.wrapServiceTagged(Utils.ensureOnlyMethod(content, epName));
                    }
                    default -> {
                        continue;
                    }
                }

                // حذف آرتیفکت هم‌نام/هم‌نوع قبلی (upsert)
                ep.getAiArtifacts().removeIf(a ->
                        a != null
                                && Objects.equals(a.getType(), type)
                                && Objects.equals(a.getName(), epName)
                );

                var art = new AiArtifact();
                art.setType(type);
                art.setName(epName);
                art.setPathHint(hint);
                art.setContent(tagged);
                ep.getAiArtifacts().add(art);
                addedArtifacts++;

                if (previewTargetPath != null) {
                    previewFiles.add(new Project.GeneratedFile(previewTargetPath, tagged));
                }
                continue;
            }

            // 2) فایل‌های کامل کنترلر/سرویس/امپلیم که AI تولید کرده
            boolean controllerLike = Utils.isControllerFile(path, content, normalizedCtrl);
            boolean serviceLike    = Utils.isServiceFile(path, content, svcSimple);
            boolean implLike       = Utils.isServiceImplFile(path, content, implSimple);

            if (controllerLike) {
                // اگر کلاس بدون پسوند Controller باشد، نام کلاس را تصحیح می‌کنیم
                String fixedContent = Utils.ensureControllerClassName(content, normalizedCtrl);
                // سعی می‌کنیم فقط متد مربوط به epName را استخراج کنیم (بر اساس نام یا مپینگ)
                String methodOnly   = Utils.extractControllerMethodByNameOrMapping(fixedContent, epName);
                if (methodOnly == null || methodOnly.isBlank()) {
                    // fallback: اگر پیدا نشد، برای اینکه فرایند نخوابد، کل فایل را تگ می‌کنیم
                    methodOnly = fixedContent;
                }
                String tagged = Utils.wrapControllerTagged(methodOnly);

                // upsert روی آرتیفکت کنترلر
                ep.getAiArtifacts().removeIf(a ->
                        a != null
                                && Objects.equals(a.getType(), "controller-method")
                                && Objects.equals(a.getName(), epName)
                );

                var art = new AiArtifact();
                art.setType("controller-method");
                art.setName(epName);
                art.setPathHint(controllerPath);
                art.setContent(tagged);
                ep.getAiArtifacts().add(art);
                addedArtifacts++;

                // UI: نسخه‌ی کاملِ اصلاح‌شده را هم نمایش بدهیم
                previewFiles.add(new Project.GeneratedFile(
                        controllerPath + "  (full-file from AI, normalized)",
                        fixedContent
                ));
                continue;
            }

            if (serviceLike) {
                String methodOnly = Utils.extractServiceMethodByName(content, epName);
                if (methodOnly == null || methodOnly.isBlank()) {
                    methodOnly = content; // fallback امن
                }
                String tagged = Utils.wrapServiceTagged(methodOnly);

                ep.getAiArtifacts().removeIf(a ->
                        a != null
                                && Objects.equals(a.getType(), "service-method")
                                && Objects.equals(a.getName(), epName)
                );

                var art = new AiArtifact();
                art.setType("service-method");
                art.setName(epName);
                art.setPathHint(servicePath);
                art.setContent(tagged);
                ep.getAiArtifacts().add(art);
                addedArtifacts++;

                previewFiles.add(new Project.GeneratedFile(
                        servicePath + "  (full-file from AI)",
                        content
                ));
                continue;
            }

            if (implLike) {
                String methodOnly = Utils.extractServiceMethodByName(content, epName);
                if (methodOnly == null || methodOnly.isBlank()) {
                    methodOnly = content;
                }
                String tagged = Utils.wrapServiceTagged(methodOnly);

                ep.getAiArtifacts().removeIf(a ->
                        a != null
                                && Objects.equals(a.getType(), "service-impl-method")
                                && Objects.equals(a.getName(), epName)
                );

                var art = new AiArtifact();
                art.setType("service-impl-method");
                art.setName(epName);
                art.setPathHint(serviceImplPath);
                art.setContent(tagged);
                ep.getAiArtifacts().add(art);
                addedArtifacts++;

                previewFiles.add(new Project.GeneratedFile(
                        serviceImplPath + "  (full-file from AI)",
                        content
                ));
                continue;
            }

            // 3) سایر فایل‌های جاوا: فقط برای UI نمایش بده
            if (path.endsWith(".java")) {
                previewFiles.add(new Project.GeneratedFile(path, content));
            }
        }

        ep.setAiFiles(previewFiles);
        repo.save(p);

        if (clearStash) {
            aiDraftStore.clear(id);
        }

        return Map.of(
                "ok", true,
                "count", addedArtifacts,
                "files", previewFiles.size()
        );
    }


/* ============================
   کمک‌متدها: تگ‌گذاری و استخراج
   ============================ */

    private String wrapControllerTagged(String body) {
        // Utils.controllerStart()/controllerEnd() را استفاده کن
        return Utils.controllerStart() + "\n" + safeTrim(body) + "\n" + Utils.controllerEnd();
    }

    private String wrapServiceTagged(String body) {
        return Utils.serviceRegionStart() + "\n" + safeTrim(body) + "\n" + Utils.serviceRegionEnd();
    }

    private String safeTrim(String s) {
        return (s == null) ? "" : s.trim();
    }

    /**
     * تلاش برای استخراج بدنهٔ متد کنترلر بر اساس نام مشتق‌شده از epName
     * یا مسیر مپینگ که epName در آن آمده است (heuristic).
     */
    private String extractControllerMethodByNameOrMapping(String src, String epName) {
//        String guessedName = epName; // همان روالی که برای نام‌گذاری متدها داری
        String m = extractMethodByName(src, epName);
        if (m != null && !m.isBlank()) return m;

        // اگر نام پیدا نشد، با annotation mappingها امتحان کن
        // به دنبال @GetMapping("/...epName...")، @PostMapping، @PutMapping، @DeleteMapping
        String byMapping = extractMethodByMappingContains(src, epName);
        return safeTrim(byMapping);
    }

    private String extractServiceMethodByName(String src, String epName) {
        String guessedName = toUpperCamel(epName);
        return safeTrim(extractMethodByName(src, guessedName));
    }

    /**
     * جست‌وجوی متد با اسم مشخص (public .* guessedName(...)
     * با یک پارسر سادهٔ براکت‌محور.
     */
    private String extractMethodByName(String src, String methodName) {
        if (src == null || methodName == null || methodName.isBlank()) return null;
        String regex = "(public|protected|private)\\s+[\\w\\<\\>\\[\\]]+\\s+" + Pattern.quote(methodName) + "\\s*\\(";
        Matcher m = Pattern.compile(regex).matcher(src);
        if (!m.find()) return null;
        int start = m.start();
        // از اولین '{' بعد از امضا، تا براکت بسته متناظر
        int braceOpen = src.indexOf('{', m.end());
        if (braceOpen < 0) return null;
        int end = findMatchingBrace(src, braceOpen);
        if (end < 0) return null;
        return src.substring(start, end + 1);
    }

    /**
     * جست‌وجوی متدِ دارای انوتیشن مپینگ که مسیرش حاوی epName است.
     */
    private String extractMethodByMappingContains(String src, String epName) {
        if (src == null || epName == null || epName.isBlank()) return null;
        Pattern p = Pattern.compile("@(GetMapping|PostMapping|PutMapping|DeleteMapping)\\s*\\(\\s*\"([^\"]*)\"\\s*\\)");
        Matcher m = p.matcher(src);
        while (m.find()) {
            String path = m.group(2);
            if (path != null && path.toLowerCase().contains(epName.toLowerCase())) {
                // امضای متد بعد از این annotation می‌آید
                int annEnd = m.end();
                // به دنبال اولین '{' بعد از امضا
                int sigStart = src.indexOf("(", annEnd);
                if (sigStart < 0) continue;
                int braceOpen = src.indexOf('{', sigStart);
                if (braceOpen < 0) continue;
                int end = findMatchingBrace(src, braceOpen);
                if (end < 0) continue;

                // برای بازگشت تمیزتر، از آغاز خطِ امضای متد برگردان
                int methodDeclStart = backToLineStart(src, annEnd);
                return src.substring(methodDeclStart, end + 1);
            }
        }
        return null;
    }

    private int backToLineStart(String s, int from) {
        int i = from;
        while (i > 0 && s.charAt(i - 1) != '\n' && s.charAt(i - 1) != '\r') i--;
        return i;
    }

    /** پیدا کردن ‘}’ متناظر با ‘{’ با احتساب تو در تو */
    private int findMatchingBrace(String s, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    /** اگر محتوا فقط یک متد است همان را برگردان؛ اگر کلاس کامل است، سعی کن متد این اندپوینت را بیرون بکشی */
    private String ensureOnlyMethod(String content, String epName) {
        if (content == null) return "";
        // اگر ظاهراً یک متد است (با '{' و بدون class ... { )
        if (!content.contains(" class ") && content.contains("(") && content.contains("{") && content.contains("}")) {
            return content.trim();
        }
        // در غیر این صورت سعی کن متد مناسب را بیرون بکشی
        String m = extractControllerMethodByNameOrMapping(content, epName);
        return (m == null || m.isBlank()) ? content.trim() : m.trim();
    }



    private boolean isControllerFile(String path, String content, String normalizedCtrl) {
        if (path == null) path = "";
        // مسیر درست
        if (path.contains("/controller/")) {
            // اگر هم‌نام کنترلر نرمال‌شده باشد → قطعی
            if (path.endsWith("/" + normalizedCtrl + ".java")) {
                return true;
            }
            // اگر AI مثلاً Customer.java ساخته ولی در controller است
            if (content != null && content.contains("class " + stripControllerSuffix(normalizedCtrl))) {
                return true;
            }
        }
        // محتوایی
        if (content != null) {
            if (content.contains("@RestController") || content.contains("@Controller")) {
                if (content.contains("class " + normalizedCtrl)) {
                    return true;
                }
                // کنترلر بدون پسوند
                if (content.contains("class " + stripControllerSuffix(normalizedCtrl))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isServiceFile(String path, String content, String svcSimple) {
        if (path == null) path = "";
        if (path.contains("/service/") && path.endsWith("/" + svcSimple + ".java")) {
            return true;
        }
        if (content != null && content.contains("interface " + svcSimple)) {
            return true;
        }
        return false;
    }

    private boolean isServiceImplFile(String path, String content, String implSimple) {
        if (path == null) path = "";
        if (path.contains("/service/") && path.endsWith("/" + implSimple + ".java")) {
            return true;
        }
        if (content != null && content.contains("class " + implSimple)) {
            return true;
        }
        return false;
    }

    /**
     * تشخیص فایل تست تا اشتباهی توی controller مرج نشه
     */
    private boolean isTestFile(String path, String content) {
        String p = (path == null) ? "" : path;
        String c = (content == null) ? "" : content;

        // 1) مسیر تست
        if (p.contains("/src/test/java/")) return true;

        // 2) اسم‌های رایج تست
        if (p.endsWith("Test.java") || p.endsWith("Tests.java") || p.endsWith("IT.java")) return true;

        // 3) محتوا
        if (c.contains("@SpringBootTest") || c.contains("org.junit.jupiter.api.Test")) return true;
        if (c.contains("MockMvc") || c.contains("TestRestTemplate")) return true;

        return false;
    }

    /**
     * اگر AI تست را در مسیر main ساخت، ما لااقل راهنمای UI را به test ببریم
     */
    private String normalizeTestPath(String originalPath, String pkgPath) {
        if (originalPath == null || originalPath.isBlank()) {
            return "src/test/java/" + pkgPath + "/SomeGeneratedTest.java";
        }
        // اگر زیر src/main بود، به src/test منتقلش کن
        if (originalPath.startsWith("src/main/java/")) {
            return originalPath.replace("src/main/java/", "src/test/java/");
        }
        // وگرنه همون
        return originalPath;
    }

    /**
     * نرمال‌سازی type آرتیفکت‌های AI
     * ورودی‌های مثل controller_method یا service-interface را به 3 حالت اصلی تبدیل می‌کند
     */
    private String normalizeAiArtifactType(String rawType) {
        if (rawType == null || rawType.isBlank()) return "";
        String t = rawType.trim();
        // یکتا کردن dash / underscore
        t = t.replace('_', '-');

        // کنترلر
        if (t.equalsIgnoreCase("controller-method")
                || t.equalsIgnoreCase("controller-file-method")) {
            return "controller-method";
        }

        // سرویس (interface / signature)
        if (t.equalsIgnoreCase("service-method")
                || t.equalsIgnoreCase("service-interface")
                || t.equalsIgnoreCase("service-signature")) {
            return "service-method";
        }

        // سرویس ایمپل
        if (t.equalsIgnoreCase("service-impl-method")
                || t.equalsIgnoreCase("service-implementation-method")
                || t.equalsIgnoreCase("service-impl")) {
            return "service-impl-method";
        }

        return t; // چیزهای دیگر را برنگردانیم بهتر است بالا فیلترشان کنیم
    }


    private String ensureControllerClassName(String content, String normalizedCtrl) {
        if (content == null || content.isBlank()) return content;
        // اگر همین الان همون اسم رو دارد، دست نزن
        if (content.contains("class " + normalizedCtrl)) {
            return content;
        }
        // اگر کلاس بدون Controller است، جایگزین کن
        String noSuffix = stripControllerSuffix(normalizedCtrl);
        if (content.contains("class " + noSuffix)) {
            return content.replace("class " + noSuffix, "class " + normalizedCtrl);
        }
        // اگر هیچ‌کدام نبود، همون رو برگردون
        return content;
    }



    // ---------- 5) Save edits on endpoint AI files ----------
    @PostMapping("/controllers/{ctrlName}/endpoints/{epName}/ai/save")
    public Map<String, Object> saveAiFilesForEndpoint(@PathVariable String id,
                                                      @PathVariable String ctrlName,
                                                      @PathVariable String epName,
                                                      @RequestBody SaveAiRequest req) {
        var p = getProjectOr404(id);
        var ctrl = findController(p, ctrlName);
        if (ctrl == null) return Map.of("ok", false, "message", "Controller not found");

        var ep = findEndpoint(ctrl, epName);
        if (ep == null) return Map.of("ok", false, "message", "Endpoint not found");

        var list = ep.getAiFiles();
        if (list == null || list.isEmpty())
            return Map.of("ok", false, "message", "No AI files on endpoint.");

        for (var f : req.files) {
            if (f.path != null) {
                list.stream().filter(x -> f.path.equals(x.getPath())).findFirst()
                        .ifPresent(cur -> cur.setContent(f.content != null ? f.content : cur.getContent()));
            } else if (f.index != null && f.index >= 0 && f.index < list.size()) {
                var cur = list.get(f.index);
                cur.setContent(f.content != null ? f.content : cur.getContent());
            }
        }
        repo.save(p);
        return Map.of("ok", true);
    }

    @Data
    public static class SaveAiRequest {
        public List<Item> files;
        @Data
        public static class Item {
            public Integer index;
            public String  path;
            public String  content;
        }
    }

    // ---------- Helpers ----------
    private Project getProjectOr404(String id) {
        return repo.findById(id).orElseThrow(() -> new NoSuchElementException("Project not found"));
    }

    private ControllerDef findController(Project p, String ctrlName) {
        if (p == null || p.getControllers() == null || !StringUtils.hasText(ctrlName)) return null;
        return p.getControllers().stream()
                .filter(Objects::nonNull)
                .filter(c -> ctrlName.equals(c.getName()))
                .findFirst().orElse(null);
    }

    /** ensure: اگر نبود، می‌سازیم و به پروژه اضافه می‌کنیم */
    private ControllerDef ensureController(Project p, String ctrlName) {
        var c = findController(p, ctrlName);
        if (c != null) return c;

        if (p.getControllers() == null) p.setControllers(new ArrayList<>());
        c = new ControllerDef();
        c.setName(ctrlName);
        c.setType("REST");
        var base = (p.getMs() != null && StringUtils.hasText(p.getMs().getBasePath()))
                ? p.getMs().getBasePath().trim() : "/api";
        c.setBasePath(base);
        c.setEndpoints(new ArrayList<>());
        p.getControllers().add(c);
        repo.save(p);
        return c;
    }

    private EndpointDef findEndpoint(ControllerDef ctrl, String epName) {
        if (ctrl == null || ctrl.getEndpoints() == null || !StringUtils.hasText(epName)) return null;
        return ctrl.getEndpoints().stream()
                .filter(Objects::nonNull)
                .filter(e -> epName.equals(e.getName()))
                .findFirst().orElse(null);
    }

    /** نگاشت فایل‌های AI (CodeFile) به Project.GeneratedFile برای ذخیره روی endpoint */
    private List<Project.GeneratedFile> mapToGenerated(List<CodeFile> aiFiles) {
        if (aiFiles == null) return List.of();
        return aiFiles.stream()
                .filter(Objects::nonNull)
                .map(cf -> new Project.GeneratedFile(cf.path(),
                        cf.content() == null ? "" : cf.content()
                ))
                .collect(Collectors.toList());
    }


    // GET: خواندن پرامپت ذخیره‌شده
    @GetMapping("/controllers/{ctrlName}/endpoints/{epName}/prompt")
    @ResponseBody
    public Map<String,Object> getEndpointPrompt(@PathVariable String id,
                                                @PathVariable String ctrlName,
                                                @PathVariable String epName){
        var p = repo.findById(id).orElse(null);
        var ep = findEndpoint(findController(p, ctrlName), epName);
        if (p==null || ep==null) return Map.of("ok", false);
        return Map.of("ok", true,
                "prompt", ep.getFinalPrompt(),
                "updatedAt", ep.getPromptUpdatedAt());
    }

    // POST: ذخیره/آپدیت پرامپت ذخیره‌شده
    @PostMapping("/controllers/{ctrlName}/endpoints/{epName}/prompt")
    @ResponseBody
    public Map<String,Object> saveEndpointPrompt(@PathVariable String id,
                                                 @PathVariable String ctrlName,
                                                 @PathVariable String epName,
                                                 @RequestBody Map<String,String> body){
        var p = repo.findById(id).orElse(null);
        var ep = findEndpoint(findController(p, ctrlName), epName);
        if (p==null || ep==null) return Map.of("ok", false);
        ep.setFinalPrompt(body.getOrDefault("prompt",""));
        ep.setPromptUpdatedAt(java.time.Instant.now());
        repo.save(p);
        return Map.of("ok", true);
    }

    // GET: خواندن خروجی خام AI برای نمایش
    @GetMapping("/controllers/{ctrlName}/endpoints/{epName}/ai/raw")
    @ResponseBody
    public Map<String,Object> getEndpointRaw(@PathVariable String id,
                                             @PathVariable String ctrlName,
                                             @PathVariable String epName){
        var p = repo.findById(id).orElse(null);
        var ep = findEndpoint(findController(p, ctrlName), epName);
        if (p==null || ep==null) return Map.of("ok", false);
        return Map.of("ok", true,
                "raw", ep.getLastAiRaw(),
                "updatedAt", ep.getRawUpdatedAt());
    }

    // POST: ست‌کردن خروجی خام AI بعد از generate
    @PostMapping("/controllers/{ctrlName}/endpoints/{epName}/ai/raw")
    @ResponseBody
    public Map<String,Object> saveEndpointRaw(@PathVariable String id,
                                              @PathVariable String ctrlName,
                                              @PathVariable String epName,
                                              @RequestBody Map<String,String> body){
        var p = repo.findById(id).orElse(null);
        var ep = findEndpoint(findController(p, ctrlName), epName);
        if (p==null || ep==null) return Map.of("ok", false);
        ep.setLastAiRaw(body.getOrDefault("raw",""));
        ep.setRawUpdatedAt(java.time.Instant.now());
        repo.save(p);
        return Map.of("ok", true);
    }




}
