package com.company.appmaker.controller;

import com.company.appmaker.model.Project;
import com.company.appmaker.repo.ProjectRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;

@Controller
@Validated
public class ProjectController {
    private final ProjectRepository repo;

    public ProjectController(ProjectRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/projects")
    public String projects(Model model) {
        var list = repo.findAll();
        list.sort(Comparator.comparing(Project::getCreatedAt).reversed());
        model.addAttribute("projects", list);

        // فرم ایجاد پروژه (بدون تغییر)
        model.addAttribute("form", new NewProjectForm("", ""));

        // مقادیر کمبو نسخهٔ جاوا
        model.addAttribute("javaVersions", java.util.List.of("8", "11", "17", "21"));

        return "projects";
    }

    @PostMapping("/projects")
    public String createProject(@ModelAttribute("form") @Validated NewProjectForm form,
                                @RequestParam(value = "javaVersion", required = false) String javaVersion) {
        var p = new Project(form.projectName(), form.companyName());
        p.setJavaVersion((javaVersion == null || javaVersion.isBlank()) ? "17" : javaVersion.trim());
        p = repo.save(p);

        // دیگر به گام جاوا نمی‌رویم؛ مستقیم به Packages
        return "redirect:/wizard/" + p.getId() + "/packages";
    }


    public record NewProjectForm(@NotBlank String projectName, @NotBlank String companyName) {
    }

    @PostMapping("/projects/{id}/delete")
    public String deleteProject(@PathVariable String id) {
        repo.deleteById(id);
        return "redirect:/projects";
    }

}