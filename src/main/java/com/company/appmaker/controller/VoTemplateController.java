package com.company.appmaker.controller;

import com.company.appmaker.model.ValueObjectField;
import com.company.appmaker.model.ValueObjectTemplate;
import com.company.appmaker.repo.ValueObjectTemplateRepo;
import com.company.appmaker.service.ValueObjectRenderer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/vo-templates")
public class VoTemplateController {

    private final ValueObjectTemplateRepo repo;
    private final ValueObjectRenderer renderer;
    private final ObjectMapper objectMapper; // ← مطمئن شو Bean موجوده (Spring Boot خودش داره)

    @GetMapping
    public String list(@RequestParam(required=false) String category,
                       @RequestParam(required=false) String status,
                       Model model){
        var data = (category!=null || status!=null)
                ? repo.findActiveByCategory(category==null? "banking":category)
                : repo.findAll();
        model.addAttribute("templates", data);
        model.addAttribute("category", category);
        model.addAttribute("status", status);
        return "vo/list";
    }

    @GetMapping("/new")
    public String createForm(Model model){
        var t = new ValueObjectTemplate();
        t.setId("EmailAddress");
        t.setCategory("banking");
        t.setPackageName("com.company.common.vo");
        t.setJavaType("record");
        t.setStatus("ACTIVE");

        // JSONهای پیش‌فرض برای پرکردن textarea
        model.addAttribute("t", t);
        model.addAttribute("fieldsJson", "[]");
        model.addAttribute("invariantsJson", "[]");
        return "vo/form";
    }

    @PostMapping
    public String create(@ModelAttribute VoTemplateForm form){
        var now = Instant.now();
        var t = new ValueObjectTemplate();
        t.setId(form.getId());
        t.setName(form.getName());
        t.setCategory(form.getCategory());
        t.setPackageName(form.getPackageName());
        t.setJavaType(form.getJavaType());
        t.setDescription(form.getDescription());
        t.setJacksonAsPrimitive(Boolean.TRUE.equals(form.getJacksonAsPrimitive()));
        t.setStatus(form.getStatus()==null? "ACTIVE" : form.getStatus());
        t.setVersion(1L);
        t.setCreatedAt(now);
        t.setUpdatedAt(now);

        // Parse JSON → List
        t.setFields(parseFields(form.getFields()));
        t.setInvariants(parseInvariants(form.getInvariants()));

        repo.save(t);
        return "redirect:/vo-templates";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable String id, Model model){
        var t = repo.findById(id).orElseThrow();
        model.addAttribute("t", t);
        // تبدیل Object → JSON برای نمایش در textarea
        try {
            model.addAttribute("fieldsJson", objectMapper.writeValueAsString(t.getFields()==null? List.of() : t.getFields()));
            model.addAttribute("invariantsJson", objectMapper.writeValueAsString(t.getInvariants()==null? List.of() : t.getInvariants()));
        } catch (Exception e) {
            model.addAttribute("fieldsJson", "[]");
            model.addAttribute("invariantsJson", "[]");
        }
        return "vo/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable String id, @ModelAttribute VoTemplateForm form){
        var t = repo.findById(id).orElseThrow();
        t.setName(form.getName());
        t.setCategory(form.getCategory());
        t.setPackageName(form.getPackageName());
        t.setJavaType(form.getJavaType());
        t.setDescription(form.getDescription());
        t.setJacksonAsPrimitive(Boolean.TRUE.equals(form.getJacksonAsPrimitive()));
        t.setStatus(form.getStatus()==null? "ACTIVE" : form.getStatus());
        t.setVersion(t.getVersion()==null?1L:t.getVersion()+1);
        t.setUpdatedAt(Instant.now());

        // Parse JSON → List
        t.setFields(parseFields(form.getFields()));
        t.setInvariants(parseInvariants(form.getInvariants()));

        repo.save(t);
        return "redirect:/vo-templates";
    }

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable String id){
        var t = repo.findById(id).orElseThrow();
        t.setStatus("ARCHIVED");
        t.setUpdatedAt(Instant.now());
        repo.save(t);
        return "redirect:/vo-templates";
    }

    @PostMapping("/render")
    @ResponseBody
    public String render(@RequestBody ValueObjectTemplate t){
        return renderer.renderJava(t);
    }

    // ---------- helpers ----------
    private List<ValueObjectField> parseFields(String json){
        try {
            if (json==null || json.isBlank()) return List.of();
            return objectMapper.readValue(json, new TypeReference<List<ValueObjectField>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("fields JSON نامعتبر است", e);
        }
    }
    private List<String> parseInvariants(String json){
        try {
            if (json==null || json.isBlank()) return List.of();
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("invariants JSON نامعتبر است", e);
        }
    }

    @Data
    public static class VoTemplateForm {
        private String id;
        private String name;
        private String category;
        private String packageName;
        private String javaType;
        private String description;
        private String fields;       // textarea JSON
        private String invariants;   // textarea JSON
        private Boolean jacksonAsPrimitive;
        private String status;
    }
}
