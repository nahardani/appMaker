package com.company.appmaker.controller;


import com.company.appmaker.enums.*;
import com.company.appmaker.repo.PromptTemplateRepo;
import com.company.appmaker.service.PromptRenderer;
import com.company.appmaker.service.PromptTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/prompts")
public class PromptController {

    private final PromptTemplateRepo repo;
    private final PromptRenderer renderer;

    // لیست + فیلتر
    @GetMapping
    public String list(@RequestParam(required = false) String category,
                       @RequestParam(required = false) PromptTarget target,
                       @RequestParam(required = false, name = "java") String javaVersion,
                       @RequestParam(required = false) String projectId,
                       Model model) {

        List<PromptTemplate> prompts = (projectId != null || category != null || target != null)
                ? repo.searchActive(projectId, category, target)
                : repo.findAll();

        if (javaVersion != null && !javaVersion.isBlank()) {
            prompts = prompts.stream()
                    .filter(p ->  p.getJavaVersion().equals(javaVersion))
                    .toList();
        }

        model.addAttribute("prompts", prompts);
        model.addAttribute("category", category);
        model.addAttribute("target", target);
        model.addAttribute("java", javaVersion);
        model.addAttribute("projectId", projectId);
        addCombos(model);
        return "prompts/list";
    }

    // فرم ایجاد
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("prompt", new PromptTemplate());
        addCombos(model);
        return "prompts/form";
    }

    // ذخیره (ایجاد)
    @PostMapping
    public String create(@ModelAttribute PromptTemplate prompt) {
        if (prompt.getStatus() == null) prompt.setStatus(PromptStatus.ACTIVE);
        if (prompt.getScope() == null)  prompt.setScope(PromptScope.GLOBAL);
        prompt.setVersion(1L);
        prompt.setCreatedAt(Instant.now());
        prompt.setUpdatedAt(Instant.now());
        repo.save(prompt);
        return "redirect:/prompts";
    }

    // فرم ویرایش
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model) {
        PromptTemplate p = repo.findById(id).orElseThrow();
        model.addAttribute("prompt", p);
        addCombos(model);
        return "prompts/form";
    }

    // ذخیره (ویرایش)
    @PostMapping("/{id}")
    public String update(@PathVariable String id, @ModelAttribute PromptTemplate patch) {
        PromptTemplate p = repo.findById(id).orElseThrow();
        p.setName(patch.getName());
        p.setCategory(patch.getCategory());
        p.setTarget(patch.getTarget());
        p.setScope(patch.getScope());
        p.setProjectId(patch.getProjectId());
        p.setJavaVersion(patch.getJavaVersion());
        p.setTags(patch.getTags());
        p.setBody(patch.getBody());
        p.setStatus(patch.getStatus());
        p.setVersion(p.getVersion() == null ? 1L : p.getVersion() + 1);
        p.setUpdatedAt(Instant.now());
        repo.save(p);
        return "redirect:/prompts";
    }

    // آرشیو
    @PostMapping("/{id}/archive")
    public String archive(@PathVariable String id) {
        PromptTemplate p = repo.findById(id).orElseThrow();
        p.setStatus(PromptStatus.ARCHIVED);
        p.setUpdatedAt(Instant.now());
        repo.save(p);
        return "redirect:/prompts";
    }

    // پیش‌نمایش رندر (AJAX)
    @PostMapping("/render")
    @ResponseBody
    public String render(@RequestBody RenderRequest req) {
        return renderer.render(req.body(), req.vars());
    }

    private void addCombos(Model model){
        model.addAttribute("javaVersions", JavaVersion.values());
        model.addAttribute("categories", Category.values());
        model.addAttribute("targets",     PromptTarget.values());
        model.addAttribute("scopes",      PromptScope.values());
        model.addAttribute("statuses",    PromptStatus.values());
    }

    public record RenderRequest(String body, Map<String, Object> vars) {}
}
