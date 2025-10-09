package com.company.appmaker.controller;

import com.company.appmaker.repo.ProjectRepository;
import com.company.appmaker.service.DefaultProjectInitializer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@Validated
@RequestMapping("/wizard/{id}")
public class PackageController {

    private final ProjectRepository repo;
    private final DefaultProjectInitializer initializer;


    public PackageController(ProjectRepository repo, DefaultProjectInitializer initializer) {
        this.repo = repo;
        this.initializer = initializer;
    }

    private static final java.util.Set<String> DEFAULT_LOCKED =
            java.util.Set.of(
                    "config","controller","service","repository","domain","dto",
                    "mapper","exception","configprops","common",
                    "swagger","i18n","client","security"
            );


    public static final class PackageOpt {
        private String key;
        private String label;
        private String desc;
        private boolean core;     // هسته‌ای؟
        private boolean checked;  // برای UI

        public PackageOpt(String key, String label, String desc, boolean core) {
            this.key = key; this.label = label; this.desc = desc; this.core = core;
        }
        // getters/setters
        public String getKey(){return key;} public String getLabel(){return label;}
        public String getDesc(){return desc;} public boolean isCore(){return core;}
        public boolean isChecked(){return checked;} public void setChecked(boolean c){this.checked=c;}
    }

    private List<PackageOpt> corePackageOptions() {
        return List.of(
                new PackageOpt("config",      "config","پیکربندی‌های Spring (Web, Jackson, CORS, OpenAPI…)",true),
                new PackageOpt("controller",  "controller","API های REST (@RestController)",  true),
                new PackageOpt("service",     "service","منطق کسب‌وکار و تراکنش‌ها (@Transactional)",          true),
                new PackageOpt("repository",  "repository","دسترسی داده (JPA/Jdbc/Reactive)", true),
                new PackageOpt("domain",      "domain","موجودیت‌ها/Value Object ها",true),
                new PackageOpt("dto",         "dto",         "مدل‌های ورودی/خروجی API",true),
                new PackageOpt("mapper",      "mapper",      "MapStruct/مپرهای دستی بین domain و dto",true),
                new PackageOpt("exception",   "exception",   "استثناها + GlobalExceptionHandler",true),
                new PackageOpt("configprops", "configprops", "کلاس‌های @ConfigurationProperties برای تنظیمات",true),
                new PackageOpt("common",      "common","ابزارها و اشتراکات (util/common/constants)",          true),
                new PackageOpt("swagger",     "swagger","پیکربندی springdoc-openapi",true),
                new PackageOpt("i18n",        "i18n","چندزبانه و MessageSource",true),
                new PackageOpt("client",      "client","کلاینت‌های HTTP (RestClient/WebClient)",  true),
                new PackageOpt("security",    "security","Spring Security، JWT/Basic/OAuth2",true)
        );
    }

    private List<PackageOpt> optionalPackageOptions() {
        return List.of(
                new PackageOpt("validation",  "validation",  "Annotation/Validator سفارشی",false),
                new PackageOpt("aop",         "aop",         "جنبه‌ها: لاگ/ممیزی/پرِف/مالتی‌تنَنت",false),
                new PackageOpt("event",       "event",       "Domain/Application Events",false),
                new PackageOpt("integration", "integration", "Kafka/Rabbit/Stream/Cloud Stream",        false),
                new PackageOpt("scheduling",  "scheduling",  "وظایف زمان‌بندی‌شده (@Scheduled)",        false),
                new PackageOpt("cache",       "cache",       "کش (Caffeine/Redis) و کلیدها",            false),
                new PackageOpt("storage",     "storage",     "مدیریت فایل/S3/MinIO",false),
                new PackageOpt("monitoring",  "monitoring",  "Actuator/Micrometer/Health",              false),
                new PackageOpt("batch",       "batch",       "Spring Batch/Jobها",  false)
        );
    }

    // کمک‌متد نرمال‌سازی کلید
    private static String norm(String s){
        return (s==null) ? "" : s.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @GetMapping("/packages")
    public String packagesPage(@PathVariable String id, Model model){
        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";
        model.addAttribute("project", p);

        // انتخاب‌های فعلی پروژه + قفل‌ها
        var selected = new java.util.LinkedHashSet<String>();
        if (p.getPackages()!=null) {
            for (var s : p.getPackages()) if (s!=null) selected.add(norm(s));
        }
        selected.addAll(DEFAULT_LOCKED); // تضمین حضور قفل‌ها

        // گردآوری تمام گزینه‌ها (core + optional) و یکتا کردن بر مبنای key نرمال
        var all = new java.util.LinkedHashMap<String,PackageOpt>(); // key=norm(key)
        for (var o : corePackageOptions())     all.putIfAbsent(norm(o.getKey()), o);
        for (var o : optionalPackageOptions()) all.putIfAbsent(norm(o.getKey()), o);

        // اگر برخی قفل‌ها در لیست نبودند، برای نمایش اضافه‌شان کن
        for (var k : DEFAULT_LOCKED) {
            if (!all.containsKey(k)) {
                all.put(k, new PackageOpt(k, k, "ویژگی سیستمی", true));
            }
        }

        // تقسیم به locked / selectable
        var lockedView = new java.util.ArrayList<PackageOpt>();
        var selectableView = new java.util.ArrayList<PackageOpt>();
        for (var entry : all.entrySet()){
            var key = entry.getKey();            // already normalized
            var opt = entry.getValue();
            // یک کپی سبک برای ست‌کردن checked (نمایش)
            var copy = new PackageOpt(opt.getKey(), opt.getLabel(), opt.getDesc(), opt.isCore());
            copy.setChecked(selected.contains(key));
            if (DEFAULT_LOCKED.contains(key)) lockedView.add(copy);
            else selectableView.add(copy);
        }

        model.addAttribute("lockedPkgs", lockedView);
        model.addAttribute("optPkgs", selectableView);
        model.addAttribute("selected", new java.util.ArrayList<>(selected));
        return "wizard-packages";
    }

    @PostMapping("/packages")
    public String savePackages(@PathVariable String id,
                               @RequestParam(value="packages", required=false) List<String> pkgs) {
        var p = repo.findById(id).orElse(null);
        if (p == null) return "redirect:/projects";

        var clean = new java.util.LinkedHashSet<String>();
        if (pkgs != null) for (var s : pkgs) if (s!=null) clean.add(norm(s));
        clean.addAll(DEFAULT_LOCKED);

        p.setPackages(new java.util.ArrayList<>(clean));

        initializer.applyDefaults(p);

        repo.save(p);

        // ✅ مستقیم برو به گام کنترلرها
        return "redirect:/wizard/" + id + "/controllers";
    }

}
