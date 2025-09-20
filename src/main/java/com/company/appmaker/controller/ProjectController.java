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
        return "redirect:/projects";
    }

    @GetMapping("/projects")
    public String projects(Model model) {
        var list = repo.findAll();
        list.sort(Comparator.comparing(Project::getCreatedAt).reversed());
        model.addAttribute("projects", list);
        model.addAttribute("form", new NewProjectForm("", ""));
        return "projects";
    }

    @PostMapping("/projects")
    public String createProject(@ModelAttribute("form") @Validated NewProjectForm form) {
        var p = new Project(form.projectName(), form.companyName());
        p = repo.save(p);
        return "redirect:/wizard/" + p.getId() + "/java";
    }

    public record NewProjectForm(@NotBlank String projectName, @NotBlank String companyName) {
    }

    @PostMapping("/projects/{id}/delete")
    public String deleteProject(@PathVariable String id) {
        repo.deleteById(id);
        return "redirect:/projects";
    }

}