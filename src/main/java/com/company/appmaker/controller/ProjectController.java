// src/main/java/com/company/appmaker/controller/ProjectController.java
package com.company.appmaker.controller;

import com.company.appmaker.model.MicroserviceProfile;
import com.company.appmaker.model.MicroserviceSettings;
import com.company.appmaker.model.Project;
import com.company.appmaker.repo.ProjectRepository;
import com.company.appmaker.repo.MicroserviceProfileRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;

@Controller
@Validated
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRepository repo;
    private final MicroserviceProfileRepository profiles;

    @GetMapping("/")
    public String root() { return "redirect:/home"; }

    @GetMapping("/projects")
    public String projects(Model model) {
        var list = repo.findAll();
        list.sort(Comparator.comparing(Project::getCreatedAt).reversed());
        model.addAttribute("projects", list);

        // فرم ایجاد
        model.addAttribute("form", new NewProjectForm("", "", "17", null));

        // کمبو پروفایل‌ها
        model.addAttribute("microProfiles", profiles.findAll());

        // کمبو جاوا برای خود پروژه (فیلد ریشه‌ای)
        model.addAttribute("javaVersions", java.util.List.of("8", "11", "17", "21"));

        return "projects";
    }

    @PostMapping("/projects")
    public String createProject(@ModelAttribute("form") @Validated NewProjectForm form) {
        var p = new Project(form.projectName(), form.companyName());
        p.setJavaVersion((form.javaVersion()==null || form.javaVersion().isBlank()) ? "17" : form.javaVersion().trim());

        if (form.profileId()!=null && !form.profileId().isBlank()) {
            var prof = profiles.findById(form.profileId()).orElse(null);
            if (prof != null) {
                p.setProfileId(prof.getId());
                p.setMs(mergeProfileIntoProject(p, prof));
                // جاوا پروژه اگر لازم است همگام شود (فقط اگر پروفایل override کرده بود):
                if (!blank(prof.getJavaVersion())) {
                    p.setJavaVersion(prof.getJavaVersion());
                }
            }
        }

        p = repo.save(p);
        return "redirect:/wizard/" + p.getId() + "/packages";
    }

    public record NewProjectForm(@NotBlank String projectName,
                                 @NotBlank String companyName,
                                 String javaVersion,
                                 String profileId) {}

    @PostMapping("/projects/{id}/delete")
    public String deleteProject(@PathVariable String id) {
        repo.deleteById(id);
        return "redirect:/projects";
    }


    private MicroserviceSettings mergeProfileIntoProject(Project p, MicroserviceProfile prof) {
        var ms = new MicroserviceSettings();

        // 1) نام سرویس
        String service = (blank(prof.getServiceName()) ? safeServiceName(p.getProjectName()) : prof.getServiceName());
        ms.setServiceName(service);

        // 2) جاوا: اگر پروفایل مشخص نکرده، از پروژه
        ms.setJavaVersion(blank(prof.getJavaVersion()) ? p.getJavaVersion() : prof.getJavaVersion());

        // 3) basePackage: اگر خالی بود از پروژه بساز
        String pkg = blank(prof.getBasePackage())
                ? (sanitizeGroup(p.getCompanyName()) + "." + service)
                : prof.getBasePackage();
        ms.setBasePackage(pkg);

        // 4) basePath: اگر خالی بود از پروژه
        String path = blank(prof.getBasePath()) ? ("/api/" + service) : normalizePath(prof.getBasePath());
        ms.setBasePath(path);

        // 5) apiVersion (اختیاری)
        ms.setApiVersion(blank(prof.getApiVersion()) ? "v1" : prof.getApiVersion());

        // 6) گزینه‌ها
        ms.setUseMongo(prof.isUseMongo());
        ms.setUseUlidIds(prof.isUseUlidIds());
        ms.setEnableActuator(prof.isEnableActuator());
        ms.setEnableOpenApi(prof.isEnableOpenApi());
        ms.setEnableValidation(prof.isEnableValidation());
        ms.setEnableMetrics(prof.isEnableMetrics());
        ms.setEnableSecurityBasic(prof.isEnableSecurityBasic());
        ms.setAddDockerfile(prof.isAddDockerfile());
        ms.setAddCompose(prof.isAddCompose());
        ms.setEnableTestcontainers(prof.isEnableTestcontainers());

        return ms;
    }

    private static boolean blank(String s){ return s==null || s.isBlank(); }

    private static String sanitizeGroup(String company){
        if (company==null) return "com.example";
        String g = company.trim().toLowerCase().replaceAll("[^a-z0-9.]+","-").replaceAll("^-+|-+$","");
        // اگر نقطه ندارد، یک «com.» جلوش بگذاریم تا group معتبر شود
        return g.contains(".") ? g : ("com." + g);
    }

    private static String safeServiceName(String projectName){
        if (projectName==null || projectName.isBlank()) return "service";
        return projectName.trim().toLowerCase().replaceAll("[^a-z0-9]+","-").replaceAll("^-+|-+$","");
    }

    private static String normalizePath(String p){
        String s = p==null? "/api" : p.trim();
        if (!s.startsWith("/")) s = "/" + s;
        return s.replaceAll("/{2,}","/");
    }

}
