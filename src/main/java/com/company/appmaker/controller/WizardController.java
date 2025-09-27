package com.company.appmaker.controller;

import com.company.appmaker.controller.forms.I18nForm;
import com.company.appmaker.controller.forms.ProfilesForm;
import com.company.appmaker.controller.forms.SecurityForm;
import com.company.appmaker.controller.forms.SwaggerForm;
import com.company.appmaker.model.*;
import com.company.appmaker.repo.ProjectRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import com.company.appmaker.config.ProjectScaffolder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*; // HttpHeaders, MediaType, ContentDisposition, ResponseEntity
import org.springframework.web.util.UriUtils;

import java.nio.file.Path;



@Controller
@Validated
@RequestMapping("/wizard/{id}")
public class WizardController {

    private final ProjectRepository repo;
    private final ProjectScaffolder scaffolder;

    public WizardController(ProjectRepository repo, ProjectScaffolder scaffolder){
        this.repo = repo;
        this.scaffolder = scaffolder;
    }
    private Optional<Project> load(String id) {
        return repo.findById(id);
    }

    /* ===== گام جاوا ===== */
    @GetMapping("/java")
    public String javaVersion(@PathVariable String id, Model model) {
        var p = load(id).orElse(null);
        if (p == null) return "redirect:/projects";
        model.addAttribute("project", p);
        model.addAttribute("options", List.of("8", "11", "17", "21"));
        return "wizard-java";
    }

    @PostMapping("/java")
    public String javaVersionSave(@PathVariable String id, @RequestParam("javaVersion") @NotBlank String v) {
        var p = load(id).orElse(null);
        if (p == null) return "redirect:/projects";
        p.setJavaVersion(v);
        repo.save(p);
        return "redirect:/wizard/" + id + "/packages";
    }

    /* ===== گام پکیج‌ها ===== */
    @GetMapping("/packages")
    public String packages(@PathVariable String id, Model model){
        var p = load(id).orElse(null); if(p==null) return "redirect:/projects";

        List<String> standard = List.of("service","dto","model","security","controller","repository","config");
        List<String> userPkgs = p.getPackages()==null ? java.util.List.of() : p.getPackages();

        // آن‌هایی که جزو استاندارد هستند
        java.util.Set<String> selectedStandard = new java.util.HashSet<>();
        for (String s : userPkgs) if (standard.contains(s)) selectedStandard.add(s);

        // بقیه را به‌عنوان extras CSV نمایش بده
        java.util.List<String> extras = new java.util.ArrayList<>();
        for (String s : userPkgs) if (!standard.contains(s)) extras.add(s);
        String extrasCsv = String.join(", ", extras);

        model.addAttribute("project", p);
        model.addAttribute("standard", standard);
        model.addAttribute("selectedStandard", selectedStandard);
        model.addAttribute("extrasCsv", extrasCsv);

        return "wizard-packages";
    }

    @PostMapping("/packages")
    public String packagesSave(@PathVariable String id,
                               @RequestParam(value = "pkg", required = false) List<String> selected,
                               @RequestParam(value = "extraPackages", required = false) String extra) {
        var p = load(id).orElse(null);
        if (p == null) return "redirect:/projects";
        List<String> pkgs = new ArrayList<>();
        if (selected != null) pkgs.addAll(selected);
        if (extra != null && !extra.isBlank()) {
            for (String s : extra.split(",")) {
                var t = s.trim();
                if (!t.isEmpty()) pkgs.add(t);
            }
        }
        pkgs = pkgs.stream().map(s -> s.trim().toLowerCase()).filter(s -> !s.isEmpty()).distinct().collect(Collectors.toList());
        p.setPackages(pkgs);
        repo.save(p);
        return "redirect:/wizard/" + id + "/controllers";
    }

    @GetMapping("/controllers")
    public String controllers(@PathVariable String id,
                              @RequestParam(value = "ctrl", required = false) String ctrlName,
                              @RequestParam(value = "ep",   required = false) Integer epIndex,
                              Model model) {
        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";

        model.addAttribute("project", p);

        // داخل متد GET /wizard/{id}/controllers ، بعد از model.addAttribute("project", p) و ...
        var security = p.getSecurity(); // از Project
        SecurityForm sform = new SecurityForm();
        if (security != null) {
            sform.setAuthType(security.getAuthType() == null ? "NONE" : security.getAuthType().name());
            sform.setBasicUsername(security.getBasicUsername());
            sform.setBasicPassword(security.getBasicPassword());
            sform.setBearerToken(security.getBearerToken());
            sform.setJwtSecret(security.getJwtSecret());
            sform.setJwtIssuer(security.getJwtIssuer());
            sform.setJwtAudience(security.getJwtAudience());
            sform.setJwtExpirationSeconds(security.getJwtExpirationSeconds());
            sform.setOauth2ClientId(security.getOauth2ClientId());
            sform.setOauth2ClientSecret(security.getOauth2ClientSecret());
            sform.setOauth2Issuer(security.getOauth2Issuer());
            sform.setOauth2Scopes(security.getOauth2Scopes());
            if (security.getOauth2Scopes() != null)
                sform.setOauth2Scopes(new java.util.ArrayList<>(security.getOauth2Scopes()));
            if (security.getRoles() != null)
                security.getRoles().forEach(r -> {
                    var rs = new SecurityForm.RoleSlot();
                    rs.setName(r.getName());
                    rs.setDesc(r.getDesc());
                    sform.getRoles().add(rs);
                });
            if (security.getRules() != null)
                security.getRules().forEach(r -> {
                    var rl = new SecurityForm.RuleSlot();
                    rl.setPathPattern(r.getPathPattern());
                    rl.setHttpMethod(r.getHttpMethod());
                    rl.setRequirement(r.getRequirement());
                    sform.getRules().add(rl);
                });
        } else {
            sform.setAuthType("NONE");
        }
        model.addAttribute("securityForm", sform);


        var swaggerForm = SwaggerForm.from(p.getSwagger());
        model.addAttribute("swaggerForm", swaggerForm);

        var profilesForm = ProfilesForm.from(p.getProfiles());
        model.addAttribute("profilesForm", profilesForm);

        var i18nForm = I18nForm.from(p.getI18n());
        model.addAttribute("i18nForm", i18nForm);



        model.addAttribute("httpMethodsAll", java.util.List.of("GET","POST","PUT","PATCH","DELETE","ANY"));


        // داده‌های کمکی UI (همیشه بده)
        model.addAttribute("types", List.of("REST"));
        model.addAttribute("httpMethods", List.of("GET","POST","PUT","PATCH","DELETE"));
        model.addAttribute("paramLocations", List.of("PATH","QUERY","HEADER"));
        model.addAttribute("javaTypes", List.of("String","Long","Integer","Double","Boolean","UUID","LocalDate","LocalDateTime"));

        ControllerForm form = new ControllerForm();

        // حالت کنترلر جدید
        if (ctrlName == null || ctrlName.isBlank()) {
            form.setEditing(false);
            form.setType("REST");
            form.setHttpMethod("GET");
            form.setParams(new ArrayList<>());
            form.setRequestFields(new ArrayList<>());
            form.setResponseParts(new ArrayList<>());
            model.addAttribute("form", form);
            return "wizard/wizard-controllers";
        }

        // یافتن کنترلر
        var ctrl = p.getControllers().stream()
                .filter(c -> c != null && ctrlName.equals(c.getName()))
                .findFirst().orElse(null);

        if (ctrl == null) {
            form.setEditing(false);
            form.setType("REST");
            form.setHttpMethod("GET");
            form.setParams(new ArrayList<>());
            form.setRequestFields(new ArrayList<>());
            form.setResponseParts(new ArrayList<>());
            model.addAttribute("form", form);
            return "wizard/wizard-controllers";
        }

        // پرکردن متادیتا
        form.setEditing(true);
        form.setOriginalControllerName(ctrl.getName());
        form.setName(ctrl.getName());
        form.setBasePath(ctrl.getBasePath());
        form.setType(ctrl.getType());

        var eps = ctrl.getEndpoints();
        // ✅ انتخاب اندپوینت ایمن: اگر ep نیامده ولی اندپوینت داریم → idx=0
        Integer idx = null;
        if (eps != null && !eps.isEmpty()) {
            if (epIndex != null && epIndex >= 0 && epIndex < eps.size()) {
                idx = epIndex;
            } else {
                idx = 0;
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
            // هیچ اندپوینتی منتخب نیست
            form.setHttpMethod("GET"); // یا اگر defaultHttpMethod داری، اینجا ست کن
            form.setParams(new ArrayList<>());
            form.setRequestFields(new ArrayList<>());
            form.setResponseParts(new ArrayList<>());
        }

        // تضمین مقدار
        if (form.getHttpMethod() == null) form.setHttpMethod("GET");

        model.addAttribute("form", form);
        // پس از load پروژه p و قبل از return:

        if (sform.getRoles() == null) sform.setRoles(new java.util.ArrayList<>());
        if (sform.getRules() == null) sform.setRules(new java.util.ArrayList<>());
        if (sform.getOauth2Scopes() == null) sform.setOauth2Scopes(new java.util.ArrayList<>());
        if (sform.getAuthType() == null) sform.setAuthType("NONE");

        model.addAttribute("securityForm", sform);

        return "wizard/wizard-controllers";
    }





    @PostMapping("/controllers")
    public String controllersAdd(@PathVariable String id,
                                 @RequestParam(value="action", required=false, defaultValue="save-endpoint") String action,
                                 @ModelAttribute("form") @Validated ControllerForm form){
        var p = load(id).orElse(null); if(p==null) return "redirect:/projects";

        // پیدا/ایجاد کنترلر مقصد
        ControllerDef ctrl = null;
        if (Boolean.TRUE.equals(form.getEditing()) && form.getOriginalControllerName()!=null){
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
                && ctrl.getEndpoints()!=null && idx >=0 && idx < ctrl.getEndpoints().size()){
            ep = ctrl.getEndpoints().get(idx); // ویرایش
        } else {
            ep = new EndpointDef();
            ctrl.getEndpoints().add(ep);      // ایجاد
        }

        ep.setName( (form.getEndpointName()==null || form.getEndpointName().isBlank())
                ? suggestMethodName(form.getHttpMethod(), form.getEndpointPath())
                : form.getEndpointName().trim() );

        ep.setHttpMethod(form.getHttpMethod());
        ep.setPath( (Boolean.TRUE.equals(form.getUseEndpointPath()) && form.getEndpointPath()!=null)
                ? form.getEndpointPath().trim() : "" );

        // پارامترها
        ep.getParams().clear();
        if (form.getParams()!=null) {
            for (ParamSlot s : form.getParams()) {
                if (s == null || s.getName()==null || s.getName().isBlank()) continue;
                ep.getParams().add(new ParamDef(
                        s.getName().trim(),
                        (s.getIn()==null ? "QUERY" : s.getIn()),
                        (s.getJavaType()==null || s.getJavaType().isBlank() ? "String" : s.getJavaType()),
                        Boolean.TRUE.equals(s.getRequired())
                ));
            }
        }

        // بدنه (فقط POST/PUT/PATCH)
        String http = (form.getHttpMethod()==null ? "GET" : form.getHttpMethod().toUpperCase(java.util.Locale.ROOT));
        boolean hasBody = http.equals("POST") || http.equals("PUT") || http.equals("PATCH");
        ep.getRequestFields().clear();
        ep.setRequestBodyType(null);
        if (hasBody && form.getRequestFields()!=null) {
            for (FieldSlot f : form.getRequestFields()){
                if (f==null || f.getName()==null || f.getName().isBlank()) continue;
                ep.getRequestFields().add(new FieldDef(
                        f.getName().trim(),
                        (f.getJavaType()==null || f.getJavaType().isBlank()? "String" : f.getJavaType()),
                        Boolean.TRUE.equals(f.getRequired())
                ));
            }
        }

        // پاسخ چندبخشی
        ep.getResponseParts().clear();
        if (form.getResponseParts()!=null){
            for (ResponsePartSlot slot : form.getResponseParts()){
                if (slot==null || slot.getName()==null || slot.getName().isBlank()) continue;
                var part = new ResponsePartDef();
                part.setName(slot.getName().trim());
                part.setContainer(slot.getContainer()==null ? "SINGLE" : slot.getContainer().toUpperCase());
                part.setKind(slot.getKind()==null ? "SCALAR" : slot.getKind().toUpperCase());
                if ("SCALAR".equalsIgnoreCase(part.getKind())) {
                    part.setScalarType((slot.getScalarType()==null || slot.getScalarType().isBlank()) ? "String" : slot.getScalarType().trim());
                } else {
                    part.setObjectName((slot.getObjectName()==null || slot.getObjectName().isBlank()) ? null : slot.getObjectName().trim());
                    for (FieldSlot f : slot.getFields()){
                        if (f==null || f.getName()==null || f.getName().isBlank()) continue;
                        part.getFields().add(new FieldDef(
                                f.getName().trim(),
                                (f.getJavaType()==null || f.getJavaType().isBlank()? "String" : f.getJavaType()),
                                Boolean.TRUE.equals(f.getRequired())
                        ));
                    }
                }
                ep.getResponseParts().add(part);
            }
        }
        // پاک کردن فیلدهای قدیمی
        ep.setResponseType(null); ep.setResponseList(false); ep.setResponseModelName(null); ep.getResponseFields().clear();

        repo.save(p);
        // بعد از ذخیره، برگردیم به حالت ویرایش همین کنترلر (برای ادامه کار)
        return "redirect:/wizard/" + id + "/controllers?ctrl=" + ctrl.getName();
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

    /* ===== فرم‌ها ===== */
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
        private Boolean editing = false;            // آیا در مود ویرایش هستیم؟
        private String  originalControllerName;     // نام کنترلر قبل از ویرایش (کلید یافتن)
        private Integer endpointIndex;              // ایندکس اندپوینت برای ویرایش (اگر null → افزودن)

        private List<ParamSlot> params = new ArrayList<>();
        private List<FieldSlot> requestFields = new ArrayList<>();  // فقط وقتی POST


        // داخل ControllerForm
        private Boolean useEndpointPath; // اختیاری بودن مسیر

        public Boolean getUseEndpointPath(){ return useEndpointPath; }
        public void setUseEndpointPath(Boolean useEndpointPath){ this.useEndpointPath = useEndpointPath; }

        // کانفیگ پاسخ (جایگزین UI قدیمی responseList/responseType به‌صورت کاربرپسند)
        private String responseContainer;   // "SINGLE" | "LIST"
        private String responseModelKind;   // "SCALAR" | "OBJECT"

        // اگر SCALAR:
        private String responseScalarType;  // String, Long, ...

        // اگر OBJECT:
        private String responseObjectName;  // نام دلخواه DTO خروجی
        private java.util.List<FieldSlot> responseFields = new java.util.ArrayList<>();

        // getters/setters:
        public String getResponseContainer(){return responseContainer;}
        public void setResponseContainer(String v){this.responseContainer=v;}
        public String getResponseModelKind(){return responseModelKind;}
        public void setResponseModelKind(String v){this.responseModelKind=v;}
        public String getResponseScalarType(){return responseScalarType;}
        public void setResponseScalarType(String v){this.responseScalarType=v;}
        public String getResponseObjectName(){return responseObjectName;}
        public void setResponseObjectName(String v){this.responseObjectName=v;}
        public java.util.List<FieldSlot> getResponseFields(){return responseFields;}
        public void setResponseFields(java.util.List<FieldSlot> fs){ this.responseFields = (fs!=null?fs:new java.util.ArrayList<>()); }

        private java.util.List<ResponsePartSlot> responseParts = new java.util.ArrayList<>();
        public java.util.List<ResponsePartSlot> getResponseParts(){return responseParts;}
        public void setResponseParts(java.util.List<ResponsePartSlot> r){ this.responseParts = (r!=null?r:new java.util.ArrayList<>()); }

        private String description;

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }



        // getters/setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }

        public String getEndpointPath() {
            return endpointPath;
        }

        public void setEndpointPath(String endpointPath) {
            this.endpointPath = endpointPath;
        }

        public String getEndpointName() {
            return endpointName;
        }

        public void setEndpointName(String endpointName) {
            this.endpointName = endpointName;
        }

        public String getResponseType() {
            return responseType;
        }

        public void setResponseType(String responseType) {
            this.responseType = responseType;
        }

        public Boolean getResponseList() {
            return responseList;
        }

        public void setResponseList(Boolean responseList) {
            this.responseList = responseList;
        }

        public List<ParamSlot> getParams() {
            return params;
        }

        public void setParams(List<ParamSlot> params) {
            this.params = (params != null ? params : new ArrayList<>());
        }

        public List<FieldSlot> getRequestFields() {
            return requestFields;
        }

        public void setRequestFields(List<FieldSlot> requestFields) {
            this.requestFields = (requestFields != null ? requestFields : new ArrayList<>());
        }

        public Boolean getEditing(){ return editing; }
        public void setEditing(Boolean editing){ this.editing = editing; }
        public String getOriginalControllerName(){ return originalControllerName; }
        public void setOriginalControllerName(String s){ this.originalControllerName = s; }
        public Integer getEndpointIndex(){ return endpointIndex; }
        public void setEndpointIndex(Integer i){ this.endpointIndex = i; }

    }

    /* لازم برای auto-grow بایندینگ */
    public static class ParamSlot {
        private String name;
        private String in;
        private String javaType;
        private Boolean required;

        public ParamSlot() {
        }

        public ParamSlot(String name, String in, String javaType, Boolean required) {
            this.name = name;
            this.in = in;
            this.javaType = javaType;
            this.required = required;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIn() {
            return in;
        }

        public void setIn(String in) {
            this.in = in;
        }

        public String getJavaType() {
            return javaType;
        }

        public void setJavaType(String javaType) {
            this.javaType = javaType;
        }

        public Boolean getRequired() {
            return required;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }
    }

    public static class FieldSlot {
        private String name;
        private String javaType;
        private Boolean required;

        public FieldSlot() {
        }

        public FieldSlot(String name, String javaType, Boolean required) {
            this.name = name;
            this.javaType = javaType;
            this.required = required;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getJavaType() {
            return javaType;
        }

        public void setJavaType(String javaType) {
            this.javaType = javaType;
        }

        public Boolean getRequired() {
            return required;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }
    }

    @GetMapping("/final")
    public String finalStep(@PathVariable String id,
                            @RequestParam(value="msg",required=false) String msg,
                            @RequestParam(value="err",required=false) String err,
                            Model model){
        var p = load(id).orElse(null); if(p==null) return "redirect:/projects";
        model.addAttribute("project", p);
        if (msg != null) model.addAttribute("msg", msg);
        if (err != null) model.addAttribute("err", err);
        return "wizard-final";
    }

    // دانلود ZIP
    @PostMapping("/final/zip")
    public ResponseEntity<ByteArrayResource> downloadZip(@PathVariable String id) {
        var p = load(id).orElse(null);
        if (p == null) return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/projects").build();
        try {
            byte[] bytes = scaffolder.scaffoldZip(p);
            String filename = safeFileName(p.getProjectName()) + ".zip";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/zip"));
            headers.setContentDisposition(
                    ContentDisposition.attachment().filename(filename).build()
            );
            return new ResponseEntity<>(new ByteArrayResource(bytes), headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, "/wizard/" + id + "/final?err=" + url(e.getMessage()))
                    .build();
        }
    }

    // ذخیره پروژه روی دیسک (مسیر محلی سرور)
    @PostMapping("/final/save")
    public String saveToDisk(@PathVariable String id,
                             @RequestParam("targetPath") String targetPath){
        var p = load(id).orElse(null); if(p==null) return "redirect:/projects";
        try {
            var out = Path.of(targetPath).resolve(safeFileName(p.getProjectName()));
            scaffolder.scaffoldToDirectory(p, out);
            return "redirect:/wizard/" + id + "/final?msg=" + url("پروژه در مسیر ذخیره شد: " + out);
        } catch (Exception e) {
            return "redirect:/wizard/" + id + "/final?err=" + url(e.getMessage());
        }
    }

    /* ===== ابزارک‌های کوچک ===== */
    private static String safeFileName(String s){
        if (s == null || s.isBlank()) return "project";
        return s.trim().toLowerCase().replaceAll("[^a-z0-9-_]+","-").replaceAll("^-+|-+$","");
    }
    private static String url(String s){
        try { return java.net.URLEncoder.encode(s==null?"":s, java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception ex){ return ""; }
    }

    public static class ResponsePartSlot {
        private String name;          // نام فیلد خروجی
        private String container;     // SINGLE | LIST
        private String kind;          // SCALAR | OBJECT
        private String scalarType;    // اگر SCALAR
        private String objectName;    // اگر OBJECT
        private java.util.List<FieldSlot> fields = new java.util.ArrayList<>();

        public String getName(){return name;} public void setName(String name){this.name=name;}
        public String getContainer(){return container;} public void setContainer(String container){this.container=container;}
        public String getKind(){return kind;} public void setKind(String kind){this.kind=kind;}
        public String getScalarType(){return scalarType;} public void setScalarType(String scalarType){this.scalarType=scalarType;}
        public String getObjectName(){return objectName;} public void setObjectName(String objectName){this.objectName=objectName;}
        public java.util.List<FieldSlot> getFields(){return fields;} public void setFields(java.util.List<FieldSlot> f){this.fields=(f!=null?f:new java.util.ArrayList<>());}
    }

    @PostMapping("/controllers/{ctrlName}/endpoints/{index}/delete")
    public String deleteEndpoint(@PathVariable String id,
                                 @PathVariable String ctrlName,
                                 @PathVariable int index){
        var p = load(id).orElse(null); if(p==null) return "redirect:/projects";
        var ctrl = p.getControllers().stream().filter(c->c.getName().equals(ctrlName)).findFirst().orElse(null);
        if (ctrl!=null && ctrl.getEndpoints()!=null && index>=0 && index<ctrl.getEndpoints().size()){
            ctrl.getEndpoints().remove(index);
            repo.save(p);
        }
        return "redirect:/wizard/" + id + "/controllers?ctrl=" + ctrlName;
    }

    @PostMapping("/controllers/meta")
    public String saveControllerMeta(@PathVariable String id,
                                     @ModelAttribute("form") ControllerForm form) {
        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";

        // پیدا/ایجاد کنترلر
        String name = form.getName();
        ControllerDef ctrl = p.getControllers().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    ControllerDef c = new ControllerDef();
                    c.setName(name);
                    p.getControllers().add(c);
                    return c;
                });

        ctrl.setBasePath(form.getBasePath());
        ctrl.setType(form.getType());

        // ✅ مقدار انتخاب‌شده در کمبوی HTTP Method را نگه می‌داریم
        String hm = (form.getHttpMethod() == null || form.getHttpMethod().isBlank())
                ? "GET"
                : form.getHttpMethod().toUpperCase();
        ctrl.setDefaultHttpMethod(hm);

        repo.save(p);

        // بازگشت به همان صفحه در حالت ویرایش این کنترلر
        return "redirect:/wizard/" + id + "/controllers?ctrl=" + UriUtils.encode(name, StandardCharsets.UTF_8);
    }


    @PostMapping("/controllers/endpoint")
    public String saveEndpoint(@PathVariable String id,
                               @ModelAttribute("form") ControllerForm form) {
        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";

        // کنترلر
        String ctrlName = (form.getName() != null) ? form.getName().trim() : "";
        var ctrl = p.getControllers().stream()
                .filter(c -> c != null && ctrlName.equals(c.getName()))
                .findFirst().orElse(null);
        if (ctrl == null) return "redirect:/wizard/" + id + "/controllers";

        // اندپوینت
        Integer idx = form.getEndpointIndex();
        EndpointDef ep;
        if (idx != null && idx >= 0 && idx < ctrl.getEndpoints().size()) {
            ep = ctrl.getEndpoints().get(idx);
        } else {
            ep = new EndpointDef();
            ctrl.getEndpoints().add(ep);
            idx = ctrl.getEndpoints().size() - 1;
        }

        // HTTP method از hidden که از باکس اول sync شده
        String http = (form.getHttpMethod() == null) ? "GET" : form.getHttpMethod().toUpperCase();
        ep.setHttpMethod(http);

        // path اختیاری
        String path = (Boolean.TRUE.equals(form.getUseEndpointPath()) && form.getEndpointPath()!=null)
                ? form.getEndpointPath().trim() : "";
        ep.setPath(path);

        // نام تابع جاوا
        String methodName = (form.getEndpointName()!=null && !form.getEndpointName().isBlank())
                ? form.getEndpointName().trim()
                : suggestMethodName(http, path);
        ep.setName(methodName);

        repo.save(p);

        // ریدایرکت امن به همان کنترلر و اندپوینت
        int size = ctrl.getEndpoints().size();
        String redirect = "redirect:/wizard/" + id + "/controllers?ctrl=" + UriUtils.encode(ctrlName, StandardCharsets.UTF_8);
        if (idx != null && idx >= 0 && idx < size) redirect += "&ep=" + idx;
        return redirect;
    }



    @PostMapping("/controllers/body")
    public String saveBody(@PathVariable String id,
                           @ModelAttribute("form") ControllerForm form) {
        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";
        ControllerDef ctrl = p.getControllers().stream()
                .filter(c -> c.getName().equals(form.getName()))
                .findFirst().orElse(null);
        if (ctrl == null) return "redirect:/wizard/" + id + "/controllers";

        Integer idx = form.getEndpointIndex();
        if (idx == null || idx < 0 || idx >= ctrl.getEndpoints().size())
            return "redirect:/wizard/" + id + "/controllers?ctrl=" + ctrl.getName();

        EndpointDef ep = ctrl.getEndpoints().get(idx);
        ep.getRequestFields().clear();
        if ("POST".equalsIgnoreCase(ep.getHttpMethod()) && form.getRequestFields()!=null) {
            for (FieldSlot f : form.getRequestFields()) {
                if (f == null || f.getName()==null || f.getName().isBlank()) continue;
                ep.getRequestFields().add(new FieldDef(f.getName().trim(),
                        f.getJavaType()==null ? "String" : f.getJavaType(),
                        Boolean.TRUE.equals(f.getRequired())));
            }
        }
        repo.save(p);
        return "redirect:/wizard/" + id + "/controllers?ctrl=" + ctrl.getName() + "&ep=" + idx;
    }


    @PostMapping("/controllers/response")
    public String saveResponse(@PathVariable String id,
                               @ModelAttribute("form") ControllerForm form) {
        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";
        ControllerDef ctrl = p.getControllers().stream()
                .filter(c -> c.getName().equals(form.getName()))
                .findFirst().orElse(null);
        if (ctrl == null) return "redirect:/wizard/" + id + "/controllers";

        Integer idx = form.getEndpointIndex();
        if (idx == null || idx < 0 || idx >= ctrl.getEndpoints().size())
            return "redirect:/wizard/" + id + "/controllers?ctrl=" + ctrl.getName();

        EndpointDef ep = ctrl.getEndpoints().get(idx);
        ep.getResponseParts().clear();
        if (form.getResponseParts()!=null) {
            for (ResponsePartSlot rp : form.getResponseParts()) {
                if (rp == null || rp.getName()==null || rp.getName().isBlank()) continue;
                ResponsePartDef rpd = new ResponsePartDef();
                rpd.setName(rp.getName().trim());
                rpd.setContainer(rp.getContainer()==null ? "SINGLE" : rp.getContainer());
                rpd.setKind(rp.getKind()==null ? "SCALAR" : rp.getKind());
                if ("SCALAR".equalsIgnoreCase(rpd.getKind())) {
                    rpd.setScalarType(rp.getScalarType()==null ? "String" : rp.getScalarType());
                } else {
                    rpd.setObjectName(rp.getObjectName());
                    if (rp.getFields()!=null) {
                        for (FieldSlot f : rp.getFields()) {
                            if (f == null || f.getName()==null || f.getName().isBlank()) continue;
                            rpd.getFields().add(new FieldDef(f.getName().trim(),
                                    f.getJavaType()==null ? "String" : f.getJavaType(),
                                    Boolean.TRUE.equals(f.getRequired())));
                        }
                    }
                }
                ep.getResponseParts().add(rpd);
            }
        }
        repo.save(p);
        return "redirect:/wizard/" + id + "/controllers?ctrl=" + ctrl.getName() + "&ep=" + idx;
    }


    @PostMapping("/controllers/{ctrlName}/delete")
    public String deleteController(@PathVariable String id,
                                   @PathVariable("ctrlName") String ctrlName) {
        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";

        // حذف کنترلر با نام داده‌شده
        p.getControllers().removeIf(c -> c != null && ctrlName.equals(c.getName()));
        repo.save(p);

        // برگرد به صفحه‌ی کنترلرها بدون انتخاب کنترلر
        return "redirect:/wizard/" + id + "/controllers";
    }

    @PostMapping("/controllers/params")
    public String saveParams(@PathVariable String id,
                             @ModelAttribute("form") ControllerForm form) {
        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";

        var ctrl = p.getControllers().stream()
                .filter(c -> c != null && form.getName() != null && form.getName().equals(c.getName()))
                .findFirst().orElse(null);
        if (ctrl == null) {
            return "redirect:/wizard/" + id + "/controllers";
        }

        Integer idx = form.getEndpointIndex();
        if (idx == null || idx < 0 || idx >= ctrl.getEndpoints().size()) {
            // اگر اندکس معتبر نیست، چیزی برای نمایش نداریم
            return "redirect:/wizard/" + id + "/controllers?ctrl=" + UriUtils.encode(ctrl.getName(), StandardCharsets.UTF_8);
        }

        var ep = ctrl.getEndpoints().get(idx);
        ep.getParams().clear();
        if (form.getParams() != null) {
            for (var ps : form.getParams()) {
                if (ps == null || ps.getName() == null || ps.getName().isBlank()) continue;
                ep.getParams().add(new ParamDef(
                        ps.getName().trim(),
                        ps.getIn() == null ? "QUERY" : ps.getIn(),
                        ps.getJavaType() == null ? "String" : ps.getJavaType(),
                        Boolean.TRUE.equals(ps.getRequired())
                ));
            }
        }

        repo.save(p);

        // ✅ با ep برگرد تا جدول با داده‌های ذخیره‌شده پر شود
        return "redirect:/wizard/" + id + "/controllers?ctrl=" +
                UriUtils.encode(ctrl.getName(), StandardCharsets.UTF_8) + "&ep=" + idx;
    }


    @PostMapping("/settings/security")
    public String saveSecurity(@PathVariable String id,
                               @ModelAttribute("securityForm") SecurityForm form) {
        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";

        if (p.getSecurity() == null) p.setSecurity(new SecuritySettings());
        var s = p.getSecurity();

        // اگر اسکوپ‌ها را به صورت CSV از UI می‌فرستی:
        try {
            var req = ((org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest();
            String csv = req.getParameter("oauth2ScopesCsv");
            if (csv != null) {
                var list = java.util.Arrays.stream(csv.split(","))
                        .map(String::trim).filter(v -> !v.isEmpty()).toList();
                form.setOauth2Scopes(new java.util.ArrayList<>(list));
            }
        } catch (Exception ignored) {}

        // map به مدل دیتابیس
        SecuritySettings.AuthType at;
        try { at = SecuritySettings.AuthType.valueOf(
                (form.getAuthType()==null?"NONE":form.getAuthType()).toUpperCase()); }
        catch (Exception e){ at = SecuritySettings.AuthType.NONE; }
        s.setAuthType(at);

        s.setBasicUsername(form.getBasicUsername());
        s.setBasicPassword(form.getBasicPassword());
        s.setBearerToken(form.getBearerToken());

        s.setJwtSecret(form.getJwtSecret());
        s.setJwtIssuer(form.getJwtIssuer());
        s.setJwtAudience(form.getJwtAudience());
        s.setJwtExpirationSeconds(form.getJwtExpirationSeconds());

        s.setOauth2ClientId(form.getOauth2ClientId());
        s.setOauth2ClientSecret(form.getOauth2ClientSecret());
        s.setOauth2Issuer(form.getOauth2Issuer());
        s.setOauth2Scopes(form.getOauth2Scopes()==null ? new java.util.ArrayList<>() : form.getOauth2Scopes());

        var roles = new java.util.ArrayList<SecuritySettings.Role>();
        if (form.getRoles()!=null) {
            for (var rs : form.getRoles()) {
                if (rs==null || rs.getName()==null || rs.getName().isBlank()) continue;
                roles.add(new SecuritySettings.Role(rs.getName().trim(), rs.getDesc()));
            }
        }
        s.setRoles(roles);

        var rules = new java.util.ArrayList<SecuritySettings.Rule>();
        if (form.getRules()!=null) {
            for (var rl : form.getRules()) {
                if (rl==null || rl.getPathPattern()==null || rl.getPathPattern().isBlank()) continue;
                var method = (rl.getHttpMethod()==null || rl.getHttpMethod().isBlank()) ? "ANY" : rl.getHttpMethod().trim().toUpperCase();
                var req = rl.getRequirement()==null ? "" : rl.getRequirement().trim();
                rules.add(new SecuritySettings.Rule(rl.getPathPattern().trim(), method, req));
            }
        }
        s.setRules(rules);

        repo.save(p);
        return "redirect:/wizard/" + id + "/controllers";
    }


    @PostMapping("/settings/swagger")
    public String saveSwagger(@PathVariable String id,
                              @ModelAttribute("swaggerForm") SwaggerForm form,
                              @RequestParam(value="ctrl", required=false) String ctrlName,
                              @RequestParam(value="ep",   required=false) Integer epIndex) {

        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";

        if (p.getSwagger() == null) p.setSwagger(new SwaggerSettings());
        form.applyTo(p.getSwagger());

        repo.save(p);

        // برگرد به همان کنترلر/اندپوینت انتخاب‌شده
        String redirect = "redirect:/wizard/" + id + "/controllers";
        if (ctrlName != null && !ctrlName.isBlank()) {
            redirect += "?ctrl=" + ctrlName;
            if (epIndex != null) redirect += "&ep=" + epIndex;
        }
        return redirect;
    }


    @PostMapping("/settings/profiles")
    public String saveProfiles(@PathVariable String id,
                               @ModelAttribute("profilesForm") com.company.appmaker.controller.forms.ProfilesForm form,
                               @RequestParam(value="ctrl", required=false) String ctrlName,
                               @RequestParam(value="ep",   required=false) Integer epIndex) {

        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";

        if (p.getProfiles() == null) p.setProfiles(new com.company.appmaker.model.ProfileSettings());
        form.applyTo(p.getProfiles());
        repo.save(p);

        String redirect = "redirect:/wizard/" + id + "/controllers";
        if (ctrlName != null && !ctrlName.isBlank()) {
            redirect += "?ctrl=" + ctrlName;
            if (epIndex != null) redirect += "&ep=" + epIndex;
        }
        return redirect;
    }


    @PostMapping("/settings/i18n")
    public String saveI18n(@PathVariable String id,
                           @ModelAttribute("i18nForm") com.company.appmaker.controller.forms.I18nForm form,
                           @RequestParam(value="ctrl", required=false) String ctrlName,
                           @RequestParam(value="ep",   required=false) Integer epIndex) {
        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";

        if (p.getI18n()==null) p.setI18n(new com.company.appmaker.model.I18nSettings());
        form.applyTo(p.getI18n());
        repo.save(p);

        String redirect = "redirect:/wizard/" + id + "/controllers";
        if (ctrlName != null && !ctrlName.isBlank()) {
            redirect += "?ctrl=" + ctrlName;
            if (epIndex != null) redirect += "&ep=" + epIndex;
        }
        return redirect;
    }




}

