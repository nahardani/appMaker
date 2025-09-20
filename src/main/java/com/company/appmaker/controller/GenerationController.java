package com.company.appmaker.controller;

import com.company.appmaker.config.ProjectScaffolder;
import com.company.appmaker.model.Project;
import com.company.appmaker.repo.ProjectRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Controller
@RequestMapping
public class GenerationController {
    private final ProjectRepository repo;
    private final ProjectScaffolder scaffolder;

    public GenerationController(ProjectRepository repo, ProjectScaffolder scaffolder) {
        this.repo = repo;
        this.scaffolder = scaffolder;
    }

    @PostMapping("/generate/{id}/zip")
    public ResponseEntity<byte[]> generateZip(@PathVariable String id) throws IOException {
        Project p = repo.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        byte[] bytes = scaffolder.scaffoldZip(p);
        String filename = (p.getProjectName() == null ? "project" : p.getProjectName().replaceAll("[^A-Za-z0-9-]", "-").replaceAll("^-+|-+$", "")) + ".zip";
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM).body(bytes);
    }

    @PostMapping("/generate/{id}/write")
    public String generateWrite(@PathVariable String id, @RequestParam("outputDir") String outputDir, Model model) throws IOException {
        Project p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";
        Path root = Path.of(outputDir).resolve(p.getProjectName() == null ? "project" : p.getProjectName());
        Files.createDirectories(root);
        scaffolder.scaffoldToDirectory(p, root);
        model.addAttribute("path", root.toAbsolutePath().toString());
        model.addAttribute("project", p);
        return "generated";
    }
}