package com.company.appmaker.service;

import com.company.appmaker.model.TemplateSnippet;
import com.company.appmaker.repo.TemplateSnippetRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TemplateService {

    private final TemplateSnippetRepository repo;
    private final ResourceLoader resourceLoader;



    public TemplateService(TemplateSnippetRepository repo, ResourceLoader resourceLoader) {
        this.repo = repo;
        this.resourceLoader = resourceLoader;
    }

    @Cacheable(
            value = "templates",
            key = "#section + '|' + #key + '|' + (#javaVersion==null?'any':#javaVersion) + '|' + (#language==null?'':#language)"
    )
    public String getSnippet(String section, String key, String javaVersion, String language) {
        String jv   = (javaVersion == null || javaVersion.isBlank()) ? "any" : javaVersion.trim();
        String lang = (language == null || language.isBlank()) ? null : language.trim();

        // 1) exact match: (jv, lang)
        String c = fetchDbContentNonBlank(section, key, jv, lang);
        if (c != null) return c;

        // 2) exact match: (jv, language == null)
        c = fetchDbContentNonBlank(section, key, jv, null);
        if (c != null) return c;

        // 3) 'any' + lang
        c = fetchDbContentNonBlank(section, key, "any", lang);
        if (c != null) return c;

        // 4) 'any' + null
        c = fetchDbContentNonBlank(section, key, "any", null);
        if (c != null) return c;

        // 5) fallback to classpath
        String fromClasspath = loadFromClasspath(section, key, jv, lang);
        return (fromClasspath == null) ? "" : fromClasspath;
    }

    /** فقط اگر content غیرخالی باشد برمی‌گرداند؛ وگرنه null */
    private String fetchDbContentNonBlank(String section, String key, String javaVersion, String language) {
        Optional<TemplateSnippet> t;
        if (language != null) {
            t = repo.findFirstBySectionAndKeyNameAndJavaVersionAndLanguage(section, key, javaVersion, language);
        } else {
            t = repo.findFirstBySectionAndKeyNameAndJavaVersionAndLanguageIsNull(section, key, javaVersion);
        }
        if (t.isEmpty()) return null;
        String content = t.get().getContent();
        return (content != null && !content.isBlank()) ? content : null;
    }



    private String loadFromClasspath(String section, String key, String javaVersion, String language) {
        String base = "classpath:scaffold-templates/" + section + "/";
        String[] candidates;
        if (language != null && !language.isBlank()) {
            candidates = new String[]{
                    base + key + "." + javaVersion + "." + language + ".tpl",
                    base + key + "." + javaVersion + ".tpl",
                    base + key + ".any." + language + ".tpl",
                    base + key + ".any.tpl"
            };
        } else {
            candidates = new String[]{
                    base + key + "." + javaVersion + ".tpl",
                    base + key + ".any.tpl"
            };
        }
        try {
            for (String c : candidates) {
                Resource r = resourceLoader.getResource(c);
                if (r.exists()) {
                    return new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception ex) {
            // ignore and return null
        }
        return null;
    }

    public List<TemplateSnippet> listSection(String section) {
        return repo.findBySection(section);
    }

    public Optional<TemplateSnippet> get(String id){
        return repo.findById(id);
    }

    public TemplateSnippet save(TemplateSnippet t) {
        TemplateSnippet saved = repo.save(t);
        // پاکسازی کشِ مرتبط برای refresh
        evictCacheFor(saved.getSection(), saved.getKeyName(), saved.getJavaVersion(), saved.getLanguage());
        return saved;
    }

    public void delete(String id) {
        repo.findById(id).ifPresent(t -> {
            repo.deleteById(id);
            evictCacheFor(t.getSection(), t.getKeyName(), t.getJavaVersion(), t.getLanguage());
        });
    }


    public void writeFromTpl(Path out,
                             String section,
                             String key,
                             String javaVersion,
                             String language,
                             Object project,                 // برای سازگاری با امضای قبلی؛ استفاده نمی‌شود
                             Map<String,String> vars) throws IOException {

        // 1) دریافت متن تمپلیت (از DB یا classpath/scaffold-templates)
        String tpl = getSnippet(section, key, javaVersion, language);
        if (tpl == null || tpl.isBlank()) {
            throw new IOException("Template not found: section=" + section + ", key=" + key
                    + ", javaVersion=" + javaVersion + ", language=" + language);
        }

        // 2) جایگزینی ${...}
        String rendered = render(tpl, (vars == null ? Map.of() : vars));

        // 3) ایجاد دایرکتوری و نوشتن فایل
        Files.createDirectories(out.getParent());
        Files.writeString(out, rendered, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * جایگزینی سادهٔ placeholderها به فرم ${name}.
     * اگر کلیدی در map نباشد، با رشتهٔ خالی جایگزین می‌شود.
     */
    public String render(String template, Map<String,String> vars) {
        String out = template;
        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                String ph = "${" + e.getKey() + "}";
                out = out.replace(ph, e.getValue() == null ? "" : e.getValue());
            }
        }
        return out;
    }


    @CacheEvict(value = "templates", key = "#section + '|' + #key + '|' + (#javaVersion==null?'any':#javaVersion) + '|' + (#language==null?'':#language)")
    public void evictCacheFor(String section, String key, String javaVersion, String language) {
        // annotated method triggers cache eviction
    }
}
