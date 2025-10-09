package com.company.appmaker.controller;

import com.company.appmaker.model.TemplateSnippet;
import com.company.appmaker.service.TemplateSnippetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Controller
@RequestMapping("/templates")
public class TemplatesController {

    private final TemplateSnippetService service;

    public TemplatesController(TemplateSnippetService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("templates", service.listAll());
        // فرم خالی برای ایجاد
        model.addAttribute("form", new TemplateSnippet());
        return "templates-list";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable String id, Model model) {
        var form = service.get(id).orElseGet(TemplateSnippet::new);
        model.addAttribute("templates", service.listAll());
        model.addAttribute("form", form);
        return "templates-list";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute TemplateSnippet form) {
        // اگر title خالی بود، یک مقدار حداقلی از section/keyName بساز
        if (form.getTitle() == null || form.getTitle().isBlank()) {
            String t = (form.getSection() == null ? "" : form.getSection()) +
                    " / " +
                    (form.getKeyName() == null ? "" : form.getKeyName());
            form.setTitle(t.trim());
        }
        if (form.getJavaVersion() == null || form.getJavaVersion().isBlank()) {
            form.setJavaVersion("any");
        }
        form.setUpdatedAt(Instant.now());
        service.save(form);
        return "redirect:/templates";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id) {
        service.delete(id);
        return "redirect:/templates";
    }
}
