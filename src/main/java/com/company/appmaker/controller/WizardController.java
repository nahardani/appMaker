package com.company.appmaker.controller;

import com.company.appmaker.ai.draft.AiDraftStore;
import com.company.appmaker.config.ProjectScaffolder;
import com.company.appmaker.controller.forms.*;
import com.company.appmaker.model.*;
import com.company.appmaker.model.coctroller.ControllerDef;
import com.company.appmaker.model.coctroller.EndpointDef;
import com.company.appmaker.repo.ProjectRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.*;
import java.nio.file.Path;


@Controller
@Validated
@RequestMapping("/wizard/{id}")
public class WizardController {

    private final ProjectRepository repo;
    private final ProjectScaffolder scaffolder;
    private final AiDraftStore aiDraftStore;


    public WizardController(ProjectRepository repo, ProjectScaffolder scaffolder, AiDraftStore aiDraftStore) {
        this.repo = repo;
        this.scaffolder = scaffolder;
        this.aiDraftStore = aiDraftStore;
    }

    @GetMapping("/controllers")
    public String controllers(@PathVariable String id,
                              @RequestParam(value = "ctrl", required = false) String ctrlName,
                              @RequestParam(value = "ep", required = false) Integer epIndex,
                              @RequestParam(value = "api", required = false) String apiSelected,
                              @RequestParam(value = "profile", required = false, defaultValue = "dev") String profile,
                              Model model) {

        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";


        // پروژه برای همه فرگمنت‌ها
        model.addAttribute("project", p);

        // ===== داده‌های کمکی UI (ثابت‌ها) =====
        model.addAttribute("types", List.of("REST"));
        model.addAttribute("httpMethods", List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        model.addAttribute("httpMethodsAll", List.of("GET", "POST", "PUT", "PATCH", "DELETE", "ANY"));
        model.addAttribute("paramLocations", List.of("PATH", "QUERY", "HEADER"));
        model.addAttribute("javaTypes", List.of("String", "Long", "Integer", "Double", "Boolean", "UUID", "LocalDate", "LocalDateTime"));

        // External APIs کمکی
        model.addAttribute("apiAuthTypes", List.of("NONE", "BASIC", "BEARER", "API_KEY"));
        model.addAttribute("apiKeyInOptions", List.of("HEADER", "QUERY"));
        model.addAttribute("httpMethodsApi", List.of("GET", "POST"));

        // ===== فرم کنترلر/اندپوینت =====
        ControllerForm form = buildControllerForm(p, ctrlName, epIndex);
        model.addAttribute("form", form);


        // برای DropDown سطح لاگ
        model.addAttribute("logLevels", List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR"));


        // WizardController.java (route: GET /wizard/{id}/settings)
        var constantsForm = ConstantsForm.from(p.getConstants());
        model.addAttribute("constantsForm", constantsForm);


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

        // همیشه متای کنترلر را از فرم به‌روز کن (rename هم پشتیبانی می‌شود)
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
        // بعد از ذخیره، برگردیم به حالت ویرایش همین کنترلر (برای ادامه کار)
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

        // 1) هیدراته کردن پروژه با فایل‌های AI از حافظه
        var aiFiles = aiDraftStore.get(id); // List<CodeFile>
        if (aiFiles != null && !aiFiles.isEmpty()) {
            p.setGeneratedFiles(toGenerated(aiFiles)); // ← تبدیل به GeneratedFile
        }

        // 2) ساخت ZIP
        byte[] zip;
        try {
            zip = scaffolder.scaffoldZip(p); // داخلش scaffoldToDirectory را روی temp انجام می‌دهد
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        String fileName = (p.getProjectName() == null || p.getProjectName().isBlank())
                ? "project.zip"
                : p.getProjectName().trim().replaceAll("[^A-Za-z0-9-]", "-").replaceAll("^-+|-+$", "") + ".zip";

        // (اختیاری) بعد از تولید نهایی می‌تونی stash را پاک کنی
        // aiDraftStore.clear(id);

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
            var aiFiles = aiDraftStore.get(id); // List<CodeFile>
            if (aiFiles != null && !aiFiles.isEmpty()) {
                p.setGeneratedFiles(toGenerated(aiFiles)); // ← تبدیل به GeneratedFile
            }

            // 2) مسیر خروجی
            var safeName = safeFileName(p.getProjectName());
            var out = Path.of(targetPath).resolve(safeName);

            // 3) اسکفولد نهایی (مرج AI درون scaffoldToDirectory انجام می‌شود)
            scaffolder.scaffoldToDirectory(p, out);

            // (اختیاری) پاکسازی stash
            // aiDraftStore.clear(id);

            return "redirect:/wizard/" + id + "/final?msg=" + url("پروژه در مسیر ذخیره شد: " + out);
        } catch (Exception e) {
            return "redirect:/wizard/" + id + "/final?err=" + url(e.getMessage());
        }
    }



    @PostMapping("/controllers/{ctrlName}/endpoints/{index}/delete")
    public String deleteEndpoint(@PathVariable String id,
                                 @PathVariable String ctrlName,
                                 @PathVariable int index) {
        var p = load(id).orElse(null);
        if (p == null) return "redirect:/projects";
        var ctrl = p.getControllers().stream().filter(c -> c.getName().equals(ctrlName)).findFirst().orElse(null);
        if (ctrl != null && ctrl.getEndpoints() != null && index >= 0 && index < ctrl.getEndpoints().size()) {
            ctrl.getEndpoints().remove(index);
            repo.save(p);
        }
        return "redirect:/wizard/" + id + "/controllers?ctrl=" + ctrlName;
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


    private ControllerForm buildControllerForm(Project p, String ctrlName, Integer epIndex) {
        ControllerForm form = new ControllerForm();

        // حالت "کنترلر جدید"
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

        // متادیتای کنترلر
        form.setEditing(true);
        form.setOriginalControllerName(ctrl.getName());
        form.setName(ctrl.getName());
        form.setBasePath(ctrl.getBasePath());
        form.setType(ctrl.getType());

        // انتخاب امن endpoint:
        var eps = ctrl.getEndpoints();
        Integer idx = null;
        if (eps != null && !eps.isEmpty()) {
            if (epIndex != null && epIndex >= 0 && epIndex < eps.size()) {
                idx = epIndex;
            } else {
                idx = 0; // اگر چیزی انتخاب نشده بود، اولین اندپوینت
            }
        }
        form.setEndpointIndex(idx);

        if (idx != null) {
            var e = eps.get(idx);

            // HTTP method
            form.setHttpMethod(e.getHttpMethod() == null ? "GET" : e.getHttpMethod());

            // مسیر نسبی
            String path = e.getPath();
            form.setUseEndpointPath(path != null && !path.isBlank());
            form.setEndpointPath(path);

            // نام متد جاوا
            form.setEndpointName(e.getName());

            // پارامترها
            form.setParams(
                    e.getParams() == null ? new ArrayList<>() :
                            e.getParams().stream()
                                    .map(pd -> new ParamSlot(pd.getName(), pd.getIn(), pd.getJavaType(), pd.isRequired()))
                                    .collect(Collectors.toList())
            );

            // بدنهٔ درخواست
            form.setRequestFields(
                    e.getRequestFields() == null ? new ArrayList<>() :
                            e.getRequestFields().stream()
                                    .map(fd -> new FieldSlot(fd.getName(), fd.getJavaType(), fd.isRequired()))
                                    .collect(Collectors.toList())
            );

            // پاسخ چندبخشی
            form.setResponseParts(
                    e.getResponseParts() == null ? new ArrayList<>() :
                            e.getResponseParts().stream().map(rp -> {
                                var slot = new ResponsePartSlot();
                                slot.setName(rp.getName());
                                slot.setContainer(rp.getContainer());
                                slot.setKind(rp.getKind());
                                slot.setScalarType(rp.getScalarType());
                                slot.setObjectName(rp.getObjectName());
                                slot.setFields(
                                        rp.getFields() == null ? new ArrayList<>() :
                                                rp.getFields().stream()
                                                        .map(ff -> new FieldSlot(ff.getName(), ff.getJavaType(), ff.isRequired()))
                                                        .collect(Collectors.toList())
                                );
                                return slot;
                            }).collect(Collectors.toList())
            );

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

    private static String suggestMethodName(String http, String path) {
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

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class ControllerForm {
        @NotBlank
        private String name;
        @NotBlank
        private String basePath;
        @NotBlank
        private String type;
        @NotBlank
        private String httpMethod; // GET/POST/PUT/PATCH/DELETE
        private String endpointPath;
        private String endpointName;
        private String responseType;
        private Boolean responseList;
        private Boolean editing = false;
        private String originalControllerName;
        private Integer endpointIndex;
        private List<ParamSlot> params = new ArrayList<>();
        private List<FieldSlot> requestFields = new ArrayList<>();  // فقط وقتی POST
        private Boolean useEndpointPath;
        private String responseContainer;
        private String responseModelKind;
        private String responseScalarType;
        private String responseObjectName;
        private List<FieldSlot> responseFields = new java.util.ArrayList<>();
        private List<ResponsePartSlot> responseParts = new java.util.ArrayList<>();
        private String description;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ParamSlot {
        private String name;
        private String in;
        private String javaType;
        private Boolean required;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FieldSlot {
        private String name;
        private String javaType;
        private Boolean required;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResponsePartSlot {
        private String name;          // نام فیلد خروجی
        private String container;     // SINGLE | LIST
        private String kind;          // SCALAR | OBJECT
        private String scalarType;    // اگر SCALAR
        private String objectName;    // اگر OBJECT
        private List<FieldSlot> fields = new ArrayList<>();
    }

    private Optional<Project> load(String id) {
        return repo.findById(id);
    }

    private static String safeFileName(String s) {
        if (s == null || s.isBlank()) return "project";
        return s.trim().toLowerCase().replaceAll("[^a-z0-9-_]+", "-").replaceAll("^-+|-+$", "");
    }

    private static String url(String s) {
        try {
            return java.net.URLEncoder.encode(s == null ? "" : s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "";
        }
    }


    private static java.util.List<Project.GeneratedFile>
    toGenerated(java.util.List<com.company.appmaker.ai.dto.CodeFile> files) {
        if (files == null || files.isEmpty()) return java.util.List.of();
        var out = new java.util.ArrayList<Project.GeneratedFile>(files.size());
        for (var f : files) {
            if (f == null || f.path() == null || f.path().isBlank()) continue;
            out.add(new Project.GeneratedFile(f.path(), f.content()));
        }
        return out;
    }


}

