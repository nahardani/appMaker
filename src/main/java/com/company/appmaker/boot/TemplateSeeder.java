package com.company.appmaker.boot;

import com.company.appmaker.model.TemplateSnippet;
import com.company.appmaker.repo.TemplateSnippetRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

@Component
public class TemplateSeeder {

    private final ResourceLoader loader;
    private final TemplateSnippetRepository repo;

    public TemplateSeeder(ResourceLoader loader, TemplateSnippetRepository repo){
        this.loader = loader; this.repo = repo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        // فقط اگر خالی است
        if (repo.count() > 0) return;

        // لیست مسیرها (پوشه‌ها)
        String[] sections = {"pom","profiles","config","logging","exception","i18n","db","controller"};
        for (String sec : sections) {
            try (var files = list("classpath:scaffold-templates/" + sec + "/")) {
                files.forEach(path -> {
                    try {
                        Resource r = loader.getResource(path);
                        if (!r.exists()) return;
                        String name = Objects.requireNonNull(r.getFilename());
                        if (!name.endsWith(".tpl")) return;

                        // name pattern: <key>.<javaVersion>.tpl   OR   <key>.<any>.tpl
                        // مثال: pom.17.tpl  /  application-dev.any.tpl
                        String base = name.substring(0, name.length() - 4);
                        String key, javaVersion = "any", language = null;

                        // اجازهٔ زبان با پسوند می‌دهیم: key.<ver>.<lang>.tpl
                        String[] parts = base.split("\\.");
                        if (parts.length == 2) {
                            key = parts[0];
                            javaVersion = parts[1];
                        } else if (parts.length == 3) {
                            key = parts[0];
                            javaVersion = parts[1];
                            language = parts[2];
                        } else {
                            key = base; // fallback
                        }

                        String content = new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                        // اگر وجود ندارد، درج کن
                        Optional<TemplateSnippet> ex = repo.findFirstBySectionAndKeyNameAndJavaVersionAndLanguage(sec, key, javaVersion, language);
                        if (ex.isEmpty()) {
                            TemplateSnippet t = new TemplateSnippet();
                            t.setSection(sec);
                            t.setKeyName(key);
                            t.setJavaVersion(javaVersion);
                            t.setLanguage(language);
                            t.setTitle(sec + "/" + name);
                            t.setDescription("Seeded from " + path);
                            t.setContent(content);
                            repo.save(t);
                        }
                    } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
        }
    }

    private Stream<String> list(String folderPattern) {
        // چون لیست مستقیم classpath ساد‌ه نیست، اینجا اسم‌ها را ثابت می‌گذاری یا با ClassPathScanningCandidateComponentProvider اسکن کن.
        // برای سادگی (و چون فایل‌ها ثابت هستند)، می‌توانی دستی لیست را نگه داری یا از FileSystemResource برای dev profile استفاده کنی.
        // این متد را در پروژه‌ی خودت با یک راه‌حل اسکن مناسب تکمیل کن.
        return Stream.of(
                // pom
                "classpath:scaffold-templates/pom/pom.8.tpl",
                "classpath:scaffold-templates/pom/pom.11.tpl",
                "classpath:scaffold-templates/pom/pom.17.tpl",
                "classpath:scaffold-templates/pom/pom.21.tpl",

                // profiles
                "classpath:scaffold-templates/profiles/application.base.any.tpl",
                "classpath:scaffold-templates/profiles/application-dev.any.tpl",
                "classpath:scaffold-templates/profiles/application-test.any.tpl",
                "classpath:scaffold-templates/profiles/application-prod.any.tpl",

                // config
                "classpath:scaffold-templates/config/OpenApiConfig.any.tpl",
                "classpath:scaffold-templates/config/WebConfig.8.tpl",
                "classpath:scaffold-templates/config/WebConfig.17.tpl",
                "classpath:scaffold-templates/config/WebConfig.21.tpl",
                "classpath:scaffold-templates/config/GlobalConstants.any.tpl",

                // logging
                "classpath:scaffold-templates/logging/logback-spring.any.tpl",

                // exception
                "classpath:scaffold-templates/exception/GlobalExceptionHandler.any.tpl",

                // i18n
                "classpath:scaffold-templates/i18n/messages.any.tpl",
                "classpath:scaffold-templates/i18n/messages_fa.any.tpl",
                "classpath:scaffold-templates/i18n/messages_en.any.tpl",

                // db
                "classpath:scaffold-templates/db/application-db2-jdbc.any.tpl",
                "classpath:scaffold-templates/db/application-db2-jndi.any.tpl",

                // controller
                "classpath:scaffold-templates/controller/Controller.8.tpl",
                "classpath:scaffold-templates/controller/Controller.17.tpl",
                "classpath:scaffold-templates/controller/Controller.21.tpl"
        ).filter(p -> p.startsWith(folderPattern.replace("classpath:", "")) || p.startsWith(folderPattern));
    }
}
