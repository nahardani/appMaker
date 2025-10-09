package com.company.appmaker.controller;

import com.company.appmaker.model.TemplateSnippet;
import com.company.appmaker.service.TemplateService;
import com.company.appmaker.service.TemplateSnippetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/templates")
public class TemplateAdminController {

    private final TemplateService service;
    private final TemplateSnippetService templateSnippetService;

    public TemplateAdminController(TemplateService service, TemplateSnippetService templateSnippetService) {
        this.service = service;
        this.templateSnippetService = templateSnippetService;
    }

    // صفحهٔ مدیریتی (اگر می‌خواهی UI داشته باشی)
    @GetMapping
    public String list(Model model) {
        List<TemplateSnippet> all = service.listSection("pom"); // یا همهٔ sectoinها
        model.addAttribute("snippets", all);
        return "admin/templates/list"; // optional thymeleaf template
    }

    // API: لیست همه سیکشن‌های یک بخش
    @GetMapping("/section/{section}")
    @ResponseBody
    public List<TemplateSnippet> listSection(@PathVariable String section){
        return service.listSection(section);
    }


    @PostMapping
    @ResponseBody
    public TemplateSnippet upsert(@RequestBody TemplateSnippet t){
        return service.save(t);
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<TemplateSnippet> get(@PathVariable String id) {
        try {
            return templateSnippetService.get(id)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception ex) {
            // log.error("Error fetching TemplateSnippet id=" + id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> delete(@PathVariable String id) {
        try {
            boolean existed = templateSnippetService.delete(id); // اگر سرویس boolean برمی‌گردونه
            if (existed)
                return ResponseEntity.noContent().build();
            else
                return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            // log.error("Error deleting TemplateSnippet id=" + id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


}
