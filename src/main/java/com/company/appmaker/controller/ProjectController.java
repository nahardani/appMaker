package com.company.appmaker.controller;

import com.company.appmaker.model.MicroserviceSettings;
import com.company.appmaker.model.Project;
import com.company.appmaker.repo.ProjectRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@Controller
@Validated
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRepository repo;

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/projects")
    public String projects(Model model) {
        var list = repo.findAll();
        list.sort(Comparator.comparing(Project::getCreatedAt).reversed());
        model.addAttribute("projects", list);

        // آبجکت فرم اصلی (مطابق projects.html → th:object="${project}")
        Project p = new Project();
        if (p.getMs() == null) p.setMs(new MicroserviceSettings());
        if (p.getJavaVersion() == null) p.setJavaVersion("17");
        if (p.getMs().getJavaVersion() == null) p.getMs().setJavaVersion(p.getJavaVersion());
        model.addAttribute("project", p);

        // اگر هنوز جایی از فرم قدیمی استفاده می‌شود، نگه داریم (بی‌ضرر)
        model.addAttribute("form", new NewProjectForm("", ""));

        // مقادیر کمبو نسخهٔ جاوا
        model.addAttribute("javaVersions", List.of("8", "11", "17", "21"));

        return "projects";
    }

    /**
     * ذخیرهٔ پروژه از روی فرم جدید (projects.html با th:object="${project}")
     */
    @PostMapping("/projects")
    public String createOrUpdateProject(@ModelAttribute("project") Project p) {
        // تضمین non-null بودن ms
        if (p.getMs() == null) p.setMs(new MicroserviceSettings());

        // پیش‌فرض‌ها
        if (p.getJavaVersion() == null || p.getJavaVersion().isBlank()) p.setJavaVersion("17");
        if (p.getMs().getJavaVersion() == null || p.getMs().getJavaVersion().isBlank())
            p.getMs().setJavaVersion(p.getJavaVersion());

        // اگر کاربر basePackage نداده، از company+project بساز
        if (p.getMs().getBasePackage() == null || p.getMs().getBasePackage().isBlank()) {
            String group = (p.getCompanyName() == null || p.getCompanyName().isBlank())
                    ? "com.example" : p.getCompanyName().trim();
            String artifact = (p.getProjectName() == null || p.getProjectName().isBlank())
                    ? "app" : p.getProjectName().trim().toLowerCase().replaceAll("[^a-z0-9]+", "");
            p.getMs().setBasePackage(group + "." + artifact);
        }

        // اگر basePath خالی است، از نام سرویس بساز
        if (p.getMs().getBasePath() == null || p.getMs().getBasePath().isBlank()) {
            String svc = (p.getMs().getServiceName() == null || p.getMs().getServiceName().isBlank())
                    ? "service" : p.getMs().getServiceName().trim().toLowerCase().replaceAll("[^a-z0-9]+", "");
            p.getMs().setBasePath("/api/" + svc);
        }

        // ذخیره
        p = repo.save(p);

        // ادامهٔ ویزارد
        return "redirect:/wizard/" + p.getId() + "/packages";
    }

    /**
     * مسیر قدیمی ایجاد (فرم کوتاه قبلی) — اگر هنوز از جایی صدا زده می‌شود.
     * اگر نمی‌خواهی، می‌توانی حذفش کنی.
     */
    @PostMapping("/projects/_legacy")
    public String createProjectLegacy(@ModelAttribute("form") @Validated NewProjectForm form,
                                      @RequestParam(value = "javaVersion", required = false) String javaVersion) {

        var p = new Project(form.projectName(), form.companyName());
        p.setJavaVersion((javaVersion == null || javaVersion.isBlank()) ? "17" : javaVersion.trim());

        // حداقل تنظیمات ms برای سازگاری
        if (p.getMs() == null) p.setMs(new MicroserviceSettings());
        p.getMs().setJavaVersion(p.getJavaVersion());
        p.getMs().setServiceName(form.projectName());
        p.getMs().setBasePath("/api/" + form.projectName().toLowerCase().replaceAll("[^a-z0-9]+", ""));
        if (p.getMs().getBasePackage() == null || p.getMs().getBasePackage().isBlank()) {
            String group = (p.getCompanyName() == null || p.getCompanyName().isBlank())
                    ? "com.example" : p.getCompanyName().trim();
            String artifact = form.projectName().toLowerCase().replaceAll("[^a-z0-9]+", "");
            p.getMs().setBasePackage(group + "." + artifact);
        }

        p = repo.save(p);
        return "redirect:/wizard/" + p.getId() + "/packages";
    }

    public record NewProjectForm(@NotBlank String projectName, @NotBlank String companyName) {}

    @PostMapping("/projects/{id}/delete")
    public String deleteProject(@PathVariable String id) {
        repo.deleteById(id);
        return "redirect:/projects";
    }
}
