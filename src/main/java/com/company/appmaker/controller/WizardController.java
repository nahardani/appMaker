package com.company.appmaker.controller;

import com.company.appmaker.model.*;
import com.company.appmaker.repo.ProjectRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import com.company.appmaker.config.ProjectScaffolder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*; // HttpHeaders, MediaType, ContentDisposition, ResponseEntity
import java.nio.file.Path;



@Controller
@Validated
@RequestMapping("/wizard/{id}") // 👈 مثل نسخهٔ پایدار شما
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

    /* ===== گام کنترلرها (Postman-like + popup) ===== */
    @GetMapping("/controllers")
    public String controllers(@PathVariable String id,
                              @RequestParam(value="ctrl", required=false) String ctrlName,
                              @RequestParam(value="ep",   required=false) Integer epIndex,
                              Model model){
        var p = load(id).orElse(null); if(p==null) return "redirect:/projects";

        ControllerForm form = new ControllerForm();
        form.setType("REST");
        form.setHttpMethod("GET");
        form.setUseEndpointPath(false);

        if (ctrlName != null && !ctrlName.isBlank()) {
            var ctrl = p.getControllers().stream()
                    .filter(c -> c.getName().equals(ctrlName))
                    .findFirst().orElse(null);
            if (ctrl != null) {
                // پرکردن کنترلر
                form.setEditing(true);
                form.setOriginalControllerName(ctrl.getName());
                form.setName(ctrl.getName());
                form.setBasePath(ctrl.getBasePath());
                form.setType(ctrl.getType());

                // اگر ep مشخص شده، اندپوینت را هم پر کن
                if (epIndex != null && ctrl.getEndpoints()!=null && epIndex >= 0 && epIndex < ctrl.getEndpoints().size()){
                    var ep = ctrl.getEndpoints().get(epIndex);
                    form.setEndpointIndex(epIndex);
                    form.setHttpMethod(ep.getHttpMethod());
                    form.setUseEndpointPath(ep.getPath()!=null && !ep.getPath().isBlank());
                    form.setEndpointPath(ep.getPath());
                    form.setEndpointName(ep.getName());

                    // پارامترها
                    var slots = new java.util.ArrayList<ParamSlot>();
                    for (var pd : ep.getParams()){
                        slots.add(new ParamSlot(pd.getName(), pd.getIn(), pd.getJavaType(), pd.isRequired()));
                    }
                    form.setParams(slots);

                    // بدنه
                    var reqSlots = new java.util.ArrayList<FieldSlot>();
                    for (var f : ep.getRequestFields()){
                        reqSlots.add(new FieldSlot(f.getName(), f.getJavaType(), f.isRequired()));
                    }
                    form.setRequestFields(reqSlots);

                    // پاسخ جدید (responseParts)
                    var partSlots = new java.util.ArrayList<ResponsePartSlot>();
                    for (var rp : ep.getResponseParts()){
                        var rps = new ResponsePartSlot();
                        rps.setName(rp.getName());
                        rps.setContainer(rp.getContainer());
                        rps.setKind(rp.getKind());
                        rps.setScalarType(rp.getScalarType());
                        rps.setObjectName(rp.getObjectName());
                        var fs = new java.util.ArrayList<FieldSlot>();
                        for (var ff : rp.getFields()){
                            fs.add(new FieldSlot(ff.getName(), ff.getJavaType(), ff.isRequired()));
                        }
                        rps.setFields(fs);
                        partSlots.add(rps);
                    }
                    form.setResponseParts(partSlots);

                    // سازگاری قدیمی (اگر پروژه‌های قبلی دارند)
                    if ((ep.getResponseParts()==null || ep.getResponseParts().isEmpty())
                            && (ep.getResponseFields()!=null && !ep.getResponseFields().isEmpty())){
                        var single = new ResponsePartSlot();
                        single.setName("result");
                        single.setContainer(ep.isResponseList() ? "LIST" : "SINGLE");
                        if (ep.getResponseFields().isEmpty()){
                            single.setKind("SCALAR");
                            single.setScalarType(ep.getResponseType()==null?"String":ep.getResponseType());
                        } else {
                            single.setKind("OBJECT");
                            single.setObjectName(ep.getResponseModelName());
                            var fs = new java.util.ArrayList<FieldSlot>();
                            for (var ff : ep.getResponseFields()){
                                fs.add(new FieldSlot(ff.getName(), ff.getJavaType(), ff.isRequired()));
                            }
                            single.setFields(fs);
                        }
                        form.getResponseParts().add(single);
                    }
                }
            }
        } else {
            // حالت ساخت کنترلر/اندپوینت جدید
            form.setName("OrderController");
            form.setBasePath("/api/orders");
        }

        model.addAttribute("project", p);
        model.addAttribute("form", form);
        model.addAttribute("types", java.util.List.of("REST"));
        model.addAttribute("httpMethods", java.util.List.of("GET","POST","PUT","PATCH","DELETE"));
        model.addAttribute("paramLocations", java.util.List.of("PATH","QUERY","HEADER"));
        model.addAttribute("javaTypes", java.util.List.of("String","Long","Integer","Double","Boolean","UUID","LocalDate","LocalDateTime"));
        return "wizard-controllers";
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



}

