package com.company.appmaker.controller;

import com.company.appmaker.ai.draft.AiDraftStore;
import com.company.appmaker.ai.dto.CodeFile;
import com.company.appmaker.ai.dto.SaveAiRequest;
import com.company.appmaker.config.ProjectScaffolder;
import com.company.appmaker.enums.PromptStatus;
import com.company.appmaker.enums.PromptTarget;
import com.company.appmaker.model.*;
import com.company.appmaker.model.coctroller.*;
import com.company.appmaker.repo.ProjectRepository;
import com.company.appmaker.repo.PromptTemplateRepo;
import com.company.appmaker.service.PromptTemplate;
import com.company.appmaker.service.TemplateService;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.company.appmaker.util.Utils.collectCommittedAiFiles;

@Controller
@Validated
@RequestMapping("/wizard/{id}")
public class WizardController {

    private final ProjectRepository repo;
    private final ProjectScaffolder scaffolder;
    private final AiDraftStore aiDraftStore;
    private final PromptTemplateRepo promptRepo;
    private final TemplateService templateService;

    private static final java.util.Set<String> CORE_LOCKED = java.util.Set.of(
            "config","controller","service","repository","domain","dto",
            "mapper","exception","configprops","common",
            "swagger","i18n","client","security"
    );

    public WizardController(ProjectRepository repo,
                            ProjectScaffolder scaffolder,
                            AiDraftStore aiDraftStore,
                            PromptTemplateRepo promptRepo, TemplateService templateService) {
        this.repo = repo;
        this.scaffolder = scaffolder;
        this.aiDraftStore = aiDraftStore;
        this.promptRepo = promptRepo;
        this.templateService = templateService;
    }


    @GetMapping("/controllers")
    public String controllers(@PathVariable String id,
                              @RequestParam(value = "ctrl", required = false) String ctrlName,
                              @RequestParam(value = "ep", required = false) String epName,
                              @RequestParam(value = "api", required = false) String apiSelected,
                              @RequestParam(value = "profile", required = false, defaultValue = "dev") String profile,
                              Model model) {
        var controllerName = ctrlName;
        // --- 1. یافتن پروژه
        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";

        // اطمینان از پکیج‌های حداقلی
        ensureCorePackagesOnProject(p);

        if (p.getControllers() == null)
            p.setControllers(new java.util.ArrayList<>());

        // --- 2. یافتن یا ساخت کنترلر انتخاب‌شده
        ControllerDef selectedCtrl = null;
        if (ctrlName != null && !ctrlName.isBlank()) {
            selectedCtrl = p.getControllers().stream()
                    .filter(c -> c != null && controllerName.equals(c.getName()))
                    .findFirst().orElse(null);

            // اگر وجود ندارد → بساز
            if (selectedCtrl == null) {
                selectedCtrl = new ControllerDef();
                selectedCtrl.setName(ctrlName.trim());
                selectedCtrl.setType("REST");
                selectedCtrl.setEndpoints(new java.util.ArrayList<>());

                String defaultBasePath = (p.getMs() != null && p.getMs().getBasePath() != null && !p.getMs().getBasePath().isBlank())
                        ? p.getMs().getBasePath().trim()
                        : "/api";
                selectedCtrl.setBasePath(defaultBasePath);

                p.getControllers().add(selectedCtrl);
                repo.save(p);
            }
        }

        // اگر هنوز کنترلری انتخاب نشده اما پروژه کنترلر دارد → اولی را بگیر
        if (selectedCtrl == null && !p.getControllers().isEmpty()) {
            selectedCtrl = p.getControllers().get(0);
            ctrlName = selectedCtrl.getName();
        }

        // --- 3. یافتن یا انتخاب اندپوینت
        com.company.appmaker.model.coctroller.EndpointDef selectedEp = null;
        if (selectedCtrl != null && selectedCtrl.getEndpoints() != null && !selectedCtrl.getEndpoints().isEmpty()) {
            if (epName != null && !epName.isBlank()) {
                selectedEp = selectedCtrl.getEndpoints().stream()
                        .filter(e -> e != null && epName.equals(e.getName()))
                        .findFirst().orElse(null);
            }
            // اگر اندپوینت مشخص نشده، اولی را انتخاب کن
            if (selectedEp == null)
                selectedEp = selectedCtrl.getEndpoints().get(0);
        }

        // --- 4. فایل‌های AI مربوط به اندپوینت انتخاب‌شده
        List<Project.GeneratedFile> aiCommitted =
                (selectedEp != null && selectedEp.getAiFiles() != null)
                        ? selectedEp.getAiFiles()
                        : java.util.List.of();

        boolean hasCommitted = !aiCommitted.isEmpty();

        // --- 5. stash فعلی (تعداد فایل‌های تولیدشده و هنوز commit‌نشده)
        var stash = aiDraftStore.get(id);
        int stashCount = (stash == null) ? 0 : stash.size();

        // --- 6. فرم کنترلر/اندپوینت (برای بخش form)
        ControllerForm form = buildControllerForm(p, ctrlName, null);

        // --- 7. پرامپت‌های فعال مرتبط با CONTROLLER
        java.util.List<PromptTemplate> prompts;
        try {
            prompts = promptRepo.findAll().stream()
                    .filter(t -> t != null
                            && t.getStatus() == PromptStatus.ACTIVE
                            && t.getTarget() == PromptTarget.CONTROLLER)
                    .toList();
        } catch (Exception ex) {
            prompts = java.util.List.of();
        }

        // --- 8. تعیین حالت صفحه
        // NEW: هنوز هیچ کنترلری وجود ندارد
        // EMPTY: کنترلر انتخاب‌شده دارد ولی اندپوینت یا فایل AI ندارد
        // COMMITTED: اندپوینت انتخاب‌شده فایل‌های AI دارد
        String mode;
        if (selectedCtrl == null) {
            mode = "NEW";
        } else if (selectedEp == null || !hasCommitted) {
            mode = "EMPTY";
        } else {
            mode = "COMMITTED";
        }

        // --- 9. افزودن داده‌ها به مدل
        model.addAttribute("project", p);
        model.addAttribute("form", form);
        model.addAttribute("prompts", prompts);
        model.addAttribute("selectedCtrl", selectedCtrl);
        model.addAttribute("selectedEp", selectedEp);
        model.addAttribute("aiCommitted", aiCommitted);
        model.addAttribute("hasCommitted", hasCommitted);
        model.addAttribute("stashCount", stashCount);
        model.addAttribute("mode", mode);

        return "wizard-controllers";
    }



    @PostMapping("/controllers")
    public String controllersAdd(@PathVariable String id,
                                 @RequestParam(value = "action", required = false, defaultValue = "save-endpoint") String action,
                                 @ModelAttribute("form") @Validated ControllerForm form) {
        var p = load(id).orElse(null);
        if (p == null) return "redirect:/projects";

        // پیدا/ایجاد کنترلر مقصد
        ControllerDef ctrl = null;
        if (Boolean.TRUE.equals(form.getEditing()) && form.getOriginalControllerName() != null) {
            ctrl = p.getControllers().stream()
                    .filter(c -> c.getName().equals(form.getOriginalControllerName()))
                    .findFirst().orElse(null);
        }
        if (ctrl == null) {
            ctrl = p.getControllers().stream()
                    .filter(c -> c.getName().equals(form.getName()))
                    .findFirst().orElse(null);
        }
        if (ctrl == null) {
            ctrl = new ControllerDef();
            ctrl.setName(form.getName());
            ctrl.setBasePath(form.getBasePath());
            ctrl.setType(form.getType());
            p.getControllers().add(ctrl);
        }

        // همیشه متادیتای کنترلر را از فرم به‌روز کن (rename هم پشتیبانی می‌شود)
        ctrl.setBasePath(form.getBasePath());
        ctrl.setType(form.getType());
        if (!ctrl.getName().equals(form.getName())) ctrl.setName(form.getName());

        if ("save-controller".equalsIgnoreCase(action)) {
            repo.save(p);
            return "redirect:/wizard/" + id + "/controllers?ctrl=" + ctrl.getName();
        }

        // از اینجا به بعد: ذخیرهٔ اندپوینت (ایجاد/ویرایش)
        EndpointDef ep;
        Integer idx = form.getEndpointIndex();
        if (Boolean.TRUE.equals(form.getEditing()) && idx != null
                && ctrl.getEndpoints() != null && idx >= 0 && idx < ctrl.getEndpoints().size()) {
            ep = ctrl.getEndpoints().get(idx); // ویرایش
        } else {
            ep = new EndpointDef();
            ctrl.getEndpoints().add(ep);      // ایجاد
        }

        ep.setName((form.getEndpointName() == null || form.getEndpointName().isBlank())
                ? suggestMethodName(form.getHttpMethod(), form.getEndpointPath())
                : form.getEndpointName().trim());

        ep.setHttpMethod(form.getHttpMethod());
        ep.setPath((Boolean.TRUE.equals(form.getUseEndpointPath()) && form.getEndpointPath() != null)
                ? form.getEndpointPath().trim() : "");

        // پارامترها
        ep.getParams().clear();
        if (form.getParams() != null) {
            for (ParamSlot s : form.getParams()) {
                if (s == null || s.getName() == null || s.getName().isBlank()) continue;
                ep.getParams().add(new ParamDef(
                        s.getName().trim(),
                        (s.getIn() == null ? "QUERY" : s.getIn()),
                        (s.getJavaType() == null || s.getJavaType().isBlank() ? "String" : s.getJavaType()),
                        Boolean.TRUE.equals(s.getRequired())
                ));
            }
        }

        // بدنه (فقط POST/PUT/PATCH)
        String http = (form.getHttpMethod() == null ? "GET" : form.getHttpMethod().toUpperCase(java.util.Locale.ROOT));
        boolean hasBody = http.equals("POST") || http.equals("PUT") || http.equals("PATCH");
        ep.getRequestFields().clear();
        ep.setRequestBodyType(null);
        if (hasBody && form.getRequestFields() != null) {
            for (FieldSlot f : form.getRequestFields()) {
                if (f == null || f.getName() == null || f.getName().isBlank()) continue;
                ep.getRequestFields().add(new FieldDef(
                        f.getName().trim(),
                        (f.getJavaType() == null || f.getJavaType().isBlank() ? "String" : f.getJavaType()),
                        Boolean.TRUE.equals(f.getRequired())
                ));
            }
        }

        // پاسخ چندبخشی
        ep.getResponseParts().clear();
        if (form.getResponseParts() != null) {
            for (ResponsePartSlot slot : form.getResponseParts()) {
                if (slot == null || slot.getName() == null || slot.getName().isBlank()) continue;
                var part = new ResponsePartDef();
                part.setName(slot.getName().trim());
                part.setContainer(slot.getContainer() == null ? "SINGLE" : slot.getContainer().toUpperCase());
                part.setKind(slot.getKind() == null ? "SCALAR" : slot.getKind().toUpperCase());
                if ("SCALAR".equalsIgnoreCase(part.getKind())) {
                    part.setScalarType((slot.getScalarType() == null || slot.getScalarType().isBlank()) ? "String" : slot.getScalarType().trim());
                } else {
                    part.setObjectName((slot.getObjectName() == null || slot.getObjectName().isBlank()) ? null : slot.getObjectName().trim());
                    for (FieldSlot f : slot.getFields()) {
                        if (f == null || f.getName() == null || f.getName().isBlank()) continue;
                        part.getFields().add(new FieldDef(
                                f.getName().trim(),
                                (f.getJavaType() == null || f.getJavaType().isBlank() ? "String" : f.getJavaType()),
                                Boolean.TRUE.equals(f.getRequired())
                        ));
                    }
                }
                ep.getResponseParts().add(part);
            }
        }
        // پاک کردن فیلدهای قدیمی
        ep.setResponseType(null);
        ep.setResponseList(false);
        ep.setResponseModelName(null);
        ep.getResponseFields().clear();

        repo.save(p);
        return "redirect:/wizard/" + id + "/controllers?ctrl=" + ctrl.getName();
    }

    @GetMapping("/final")
    public String finalStep(@PathVariable String id,
                            @RequestParam(value = "msg", required = false) String msg,
                            @RequestParam(value = "err", required = false) String err,
                            Model model) {
        var p = load(id).orElse(null);
        if (p == null) return "redirect:/projects";
        model.addAttribute("project", p);
        if (msg != null) model.addAttribute("msg", msg);
        if (err != null) model.addAttribute("err", err);
        return "wizard-final";
    }

    @PostMapping("/final/zip")
    public ResponseEntity<byte[]> downloadZip(@PathVariable String id) {
        var p = repo.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();


        templateService.getSnippet("controller","Controller",p.getJavaVersion(),"java");
        // 1) فقط فایل‌های AI کامیت‌شده در دیتابیس (از همهٔ کنترلرها)
        var committed = collectCommittedAiFiles(p);
        p.setGeneratedFiles(committed);

        // 2) ساخت ZIP
        final byte[] zip;
        try {
            zip = scaffolder.scaffoldZip(p);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // 3) نام فایل خروجی
        String fileName = (p.getProjectName() == null || p.getProjectName().isBlank())
                ? "project.zip"
                : p.getProjectName().trim()
                .replaceAll("[^A-Za-z0-9-]", "-")
                .replaceAll("^-+|-+$", "") + ".zip";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .body(zip);
    }

    @PostMapping("/final/save")
    public String saveToDisk(@PathVariable String id,
                             @RequestParam("targetPath") String targetPath) {
        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";

        try {
            // 1) فقط فایل‌های AI کامیت‌شده در دیتابیس
            var committed = collectCommittedAiFiles(p);
            p.setGeneratedFiles(committed);

            // 2) مسیر خروجی
            var safeName = safeFileName(p.getProjectName());
            var out = Path.of(targetPath).resolve(safeName);

            // 3) اسکفولد نهایی
            scaffolder.scaffoldToDirectory(p, out);

            return "redirect:/wizard/" + id + "/final?msg=" + url("پروژه در مسیر ذخیره شد: " + out);
        } catch (Exception e) {
            return "redirect:/wizard/" + id + "/final?err=" + url(e.getMessage());
        }
    }


    @PostMapping("/controllers/{ctrlName}/delete")
    public String deleteController(@PathVariable String id,
                                   @PathVariable("ctrlName") String ctrlName) {
        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";
        p.getControllers().removeIf(c -> c != null && ctrlName.equals(c.getName()));
        repo.save(p);
        return "redirect:/wizard/" + id + "/controllers";
    }

    @PostMapping("/controllers/{ctrlName}/ai/commit")
    @ResponseBody
    public Map<String, Object> commitAiFilesToController(@PathVariable String id,
                                                         @PathVariable String ctrlName,
                                                         @RequestParam(name = "clearStash", defaultValue = "false") boolean clearStash) {

        var p = repo.findById(id).orElse(null);
        if (p == null) return Map.of("ok", false, "message", "Project not found");

        var aiFiles = aiDraftStore.get(id); // List<CodeFile>
        if (aiFiles == null || aiFiles.isEmpty()) {
            return Map.of("ok", false, "message", "No AI files in stash. Run generate first.");
        }

        if (p.getControllers() == null) p.setControllers(new ArrayList<>());

        // پیدا یا ساخت کنترلر
        var ctrl = p.getControllers().stream()
                .filter(c -> c != null && ctrlName.equals(c.getName()))
                .findFirst().orElse(null);

        if (ctrl == null) {
            ctrl = new ControllerDef();
            ctrl.setName(ctrlName);
            ctrl.setType("REST");
            String basePath = (p.getMs() != null && p.getMs().getBasePath() != null && !p.getMs().getBasePath().isBlank())
                    ? p.getMs().getBasePath().trim()
                    : "/api/" + safeFileName(p.getProjectName()).replace("-", "");
            ctrl.setBasePath(basePath);
            ctrl.setDefaultHttpMethod("GET");
            p.getControllers().add(ctrl);
        }

        // تبدیل CodeFile → Project.GeneratedFile
        var generated = toGenerated(aiFiles);
        if (generated == null || generated.isEmpty()) {
            return Map.of("ok", false, "message", "AI files are not mappable.");
        }

        //  ذخیره در خود کنترلر
        if (ctrl.getAiFiles() == null) ctrl.setAiFiles(new ArrayList<>());
        ctrl.getAiFiles().clear();
        ctrl.getAiFiles().addAll(generated);

        repo.save(p);
        if (clearStash) aiDraftStore.clear(id);

        return Map.of("ok", true, "message", "AI files committed to controller: " + ctrlName, "files", generated.size());
    }


    @PostMapping("/controllers/{ctrlName}/ai/save")
    @ResponseBody
    public Map<String,Object> saveAiFiles(@PathVariable String id,
                                          @PathVariable String ctrlName,
                                          @RequestBody SaveAiRequest req){
        var p = repo.findById(id).orElse(null);
        if (p == null) return Map.of("ok", false, "message", "Project not found");

        // پیدا/ایجاد کنترلر
        var ctrl = (p.getControllers()==null)? null :
                p.getControllers().stream()
                        .filter(c -> c!=null && ctrlName.equals(c.getName()))
                        .findFirst().orElse(null);
        if (ctrl == null) {
            ctrl = new ControllerDef();
            ctrl.setName(ctrlName);
            ctrl.setBasePath(p.getMs()!=null ? p.getMs().getBasePath() : "/api");
            ctrl.setType("REST");
            if (p.getControllers()==null) p.setControllers(new java.util.ArrayList<>());
            p.getControllers().add(ctrl);
        }

        if (ctrl.getAiFiles()==null || ctrl.getAiFiles().isEmpty())
            return Map.of("ok", false, "message", "هیچ فایل AI برای این کنترلر ثبت نشده است.");

        // اعمال ویرایش‌ها (index یا path)
        var list = ctrl.getAiFiles();
        for (var f : req.files) {
            if (f.index != null && f.index >= 0 && f.index < list.size()) {
                var cur = list.get(f.index);
                cur.setContent(f.content != null ? f.content : cur.getContent());
            } else if (f.path != null) {
                list.stream().filter(x -> f.path.equals(x.getPath())).findFirst()
                        .ifPresent(cur -> cur.setContent(f.content != null ? f.content : cur.getContent()));
            }
        }
        repo.save(p);
        return Map.of("ok", true);
    }


    private ControllerForm buildControllerForm(Project p, String ctrlName, Integer epIndex) {
        ControllerForm form = new ControllerForm();

        if (ctrlName == null || ctrlName.isBlank()) {
            form.setEditing(false);
            form.setType("REST");
            form.setHttpMethod("GET");
            ensureControllerLists(form);
            return form;
        }

        var ctrl = p.getControllers() == null ? null :
                p.getControllers().stream()
                        .filter(c -> c != null && ctrlName.equals(c.getName()))
                        .findFirst().orElse(null);

        if (ctrl == null) {
            form.setEditing(false);
            form.setType("REST");
            form.setHttpMethod("GET");
            ensureControllerLists(form);
            return form;
        }

        form.setEditing(true);
        form.setOriginalControllerName(ctrl.getName());
        form.setName(ctrl.getName());
        form.setBasePath(ctrl.getBasePath());
        form.setType(ctrl.getType());

        var eps = ctrl.getEndpoints();
        Integer idx = null;
        if (eps != null && !eps.isEmpty()) {
            if (epIndex != null && epIndex >= 0 && epIndex < eps.size()) {
                idx = epIndex;
            } else idx = 0;
        }
        form.setEndpointIndex(idx);

        if (idx != null) {
            var e = eps.get(idx);
            form.setHttpMethod(e.getHttpMethod() == null ? "GET" : e.getHttpMethod());
            String path = e.getPath();
            form.setUseEndpointPath(path != null && !path.isBlank());
            form.setEndpointPath(path);
            form.setEndpointName(e.getName());
            form.setParams(e.getParams() == null ? new ArrayList<>() :
                    e.getParams().stream()
                            .map(pd -> new ParamSlot(pd.getName(), pd.getIn(), pd.getJavaType(), pd.isRequired()))
                            .collect(Collectors.toList()));
            form.setRequestFields(e.getRequestFields() == null ? new ArrayList<>() :
                    e.getRequestFields().stream()
                            .map(fd -> new FieldSlot(fd.getName(), fd.getJavaType(), fd.isRequired()))
                            .collect(Collectors.toList()));
            form.setResponseParts(e.getResponseParts() == null ? new ArrayList<>() :
                    e.getResponseParts().stream().map(rp -> {
                        var slot = new ResponsePartSlot();
                        slot.setName(rp.getName());
                        slot.setContainer(rp.getContainer());
                        slot.setKind(rp.getKind());
                        slot.setScalarType(rp.getScalarType());
                        slot.setObjectName(rp.getObjectName());
                        slot.setFields(rp.getFields() == null ? new ArrayList<>() :
                                rp.getFields().stream()
                                        .map(ff -> new FieldSlot(ff.getName(), ff.getJavaType(), ff.isRequired()))
                                        .collect(Collectors.toList()));
                        return slot;
                    }).collect(Collectors.toList()));
        } else {
            form.setHttpMethod("GET");
            ensureControllerLists(form);
        }
        if (form.getHttpMethod() == null) form.setHttpMethod("GET");
        return form;
    }
    private void ensureControllerLists(ControllerForm form) {
        if (form.getParams() == null) form.setParams(new ArrayList<>());
        if (form.getRequestFields() == null) form.setRequestFields(new ArrayList<>());
        if (form.getResponseParts() == null) form.setResponseParts(new ArrayList<>());
    }
    private String suggestMethodName(String http, String path) {
        String base = (http == null ? "get" : http.toLowerCase(Locale.ROOT));
        if (path == null || path.isBlank()) return base;
        String cleaned = path.replaceAll("[{}]", "").replaceAll("[^a-zA-Z0-9]+", "-");
        String[] parts = cleaned.split("-");
        StringBuilder sb = new StringBuilder(base);
        for (String part : parts) {
            if (part.isBlank()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }
    private void ensureCorePackagesOnProject(Project p) {
        var set = new java.util.LinkedHashSet<String>();
        if (p.getPackages() != null) {
            for (var s : p.getPackages()) if (s != null)
                set.add(s.trim().toLowerCase(Locale.ROOT));
        }
        if (!set.containsAll(CORE_LOCKED)) {
            set.addAll(CORE_LOCKED);
            p.setPackages(new ArrayList<>(set));
            repo.save(p);
        }
    }
    private Optional<Project> load(String id) {
        return repo.findById(id);
    }
    private String safeFileName(String s) {
        if (s == null || s.isBlank()) return "project";
        return s.trim().toLowerCase().replaceAll("[^a-z0-9-_]+", "-").replaceAll("^-+|-+$", "");
    }
    private String url(String s) {
        try {
            return java.net.URLEncoder.encode(s == null ? "" : s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "";
        }
    }
    private List<Project.GeneratedFile> toGenerated(List<CodeFile> files) {
        if (files == null || files.isEmpty()) return java.util.List.of();
        var out = new ArrayList<Project.GeneratedFile>(files.size());
        for (var f : files) {
            if (f == null || f.path() == null || f.path().isBlank()) continue;
            if (!f.path().endsWith(".java")) continue; // فقط جاوا
            out.add(new Project.GeneratedFile(f.path(), f.content()));
        }
        return out;
    }


}
