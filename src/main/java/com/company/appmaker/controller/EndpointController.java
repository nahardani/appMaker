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

    @PostMapping("/controllers/{ctrlName}/endpoints/{epName}/ai/commit")
    @ResponseBody
    public Map<String, Object> commitEpAi(@PathVariable String id,
                                          @PathVariable String ctrlName,
                                          @PathVariable String epName,
                                          @RequestParam(value = "clearStash", defaultValue = "true") boolean clearStash) {
        var p = repo.findById(id).orElse(null);
        if (p == null) return Map.of("ok", false, "message", "Project not found");

        var ctrl = (p.getControllers() == null) ? null :
                p.getControllers().stream().filter(c -> c != null && ctrlName.equals(c.getName())).findFirst().orElse(null);
        if (ctrl == null) return Map.of("ok", false, "message", "Controller not found");

        var ep = (ctrl.getEndpoints() == null) ? null :
                ctrl.getEndpoints().stream().filter(e -> e != null && epName.equals(e.getName())).findFirst().orElse(null);
        if (ep == null) return Map.of("ok", false, "message", "Endpoint not found");

        var draft = aiDraftStore.get(id); // List<CodeFile>
        if (draft == null || draft.isEmpty())
            return Map.of("ok", false, "message", "هیچ فایل AI برای commit وجود ندارد.");

        // خروجی‌های نمایشی برای UI (لیست فایل‌ها)
        var previewFiles = new ArrayList<Project.GeneratedFile>();
        // آرتیفکت‌ها برای مرج نهایی
        if (ep.getAiArtifacts() == null) ep.setAiArtifacts(new ArrayList<>());

        final String basePackage = resolveBasePackage(p);
        final String pkgPath     = basePackage.replace('.', '/');
        final String feature     = stripControllerSuffix(normalizeControllerName(ctrlName)); // e.g. Accounting
        final String svcSimple   = feature + "Service";
        final String implSimple  = svcSimple + "Impl";

        // مسیرهای استاندارد فایل‌ها برای نمایش
        final String controllerPath   = "src/main/java/" + pkgPath + "/controller/" + normalizeControllerName(ctrlName) + ".java";
        final String servicePath      = "src/main/java/" + pkgPath + "/service/"    + svcSimple  + ".java";
        final String serviceImplPath  = "src/main/java/" + pkgPath + "/service/"    + implSimple + ".java";

        int addedArtifacts = 0;
        for (var f : draft) {
            if (f == null || f.path() == null || f.content() == null) continue;
            final String path    = f.path();
            final String content = f.content();

            // 1) اگر artifact است (فقط متد/ریجن)
            if (path.startsWith("__ARTIFACT__/")) {
                // type خام از روی path
                String rawType = path.substring("__ARTIFACT__/".length()); // controller-method | service-method | service-impl-method | ...
                // نرمال‌سازی نوع
                String type = normalizeAiArtifactType(rawType);

                String hint = null;
                String previewTargetPath = null;

                switch (type) {
                    case "controller-method" -> {
                        hint = controllerPath;
                        previewTargetPath = controllerPath + "  (method: " + epName + ")";
                    }
                    case "service-method" -> {
                        hint = servicePath;
                        previewTargetPath = servicePath + "  (method: " + epName + ")";
                    }
                    case "service-impl-method" -> {
                        hint = serviceImplPath;
                        previewTargetPath = serviceImplPath + "  (method: " + epName + ")";
                    }
                    default -> {
                        // ناشناخته‌ها را ذخیره نکنیم تا بعداً collectCommittedAiFiles قاطی نکند
                        continue;
                    }
                }

                var art = new AiArtifact();
                art.setType(type);
                art.setName(epName);
                art.setPathHint(hint);
                art.setContent(content);
                ep.getAiArtifacts().add(art);
                addedArtifacts++;

                // برای UI
                if (previewTargetPath != null) {
                    previewFiles.add(new Project.GeneratedFile(previewTargetPath, content));
                }
                continue;
            }

            // 2) فایل‌های واقعی (DTO/Repo/…): همان‌طور که هست نمایش بده
            if (path.endsWith(".java")) {
                previewFiles.add(new Project.GeneratedFile(path, content));
            }
        }

        // در UI از ep.aiFiles برای نمایش استفاده می‌کنیم (فایل‌های واقعی + پیش‌نمایش متدها)
        ep.setAiFiles(previewFiles);
        repo.save(p);

        if (clearStash) aiDraftStore.clear(id);
        return Map.of("ok", true, "count", addedArtifacts, "files", previewFiles.size());
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
