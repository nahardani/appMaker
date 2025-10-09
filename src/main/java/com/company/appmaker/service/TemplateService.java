package com.company.appmaker.service;

import com.company.appmaker.model.TemplateSnippet;
import com.company.appmaker.repo.TemplateSnippetRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
public class TemplateService {

    private final TemplateSnippetRepository repo;
    private final ResourceLoader resourceLoader;



    public TemplateService(TemplateSnippetRepository repo, ResourceLoader resourceLoader) {
        this.repo = repo;
        this.resourceLoader = resourceLoader;
    }

    @Cacheable(value = "templates", key = "#section + '|' + #key + '|' + (#javaVersion==null?'any':#javaVersion) + '|' + (#language==null?'':#language)")
    public String getSnippet(String section, String key, String javaVersion, String language) {
        String jv = (javaVersion == null || javaVersion.isBlank()) ? "any" : javaVersion.trim();
        String lang = (language == null || language.isBlank()) ? null : language.trim();

        // 1) exact match
        Optional<TemplateSnippet> t = repo.findFirstBySectionAndKeyNameAndJavaVersionAndLanguage(section, key, jv, lang);
        if (t.isPresent()) return t.get().getContent();

        // 2) try javaVersion match with null language
        t = repo.findFirstBySectionAndKeyNameAndJavaVersionAndLanguage(section, key, jv, null);
        if (t.isPresent()) return t.get().getContent();

        // 3) try 'any' variants
        t = repo.findFirstBySectionAndKeyNameAndJavaVersionAndLanguage(section, key, "any", lang);
        if (t.isPresent()) return t.get().getContent();
        t = repo.findFirstBySectionAndKeyNameAndJavaVersionAndLanguage(section, key, "any", null);
        if (t.isPresent()) return t.get().getContent();

        // 4) fallback to classpath templates
        String fromClasspath = loadFromClasspath(section, key, jv, lang);
        return (fromClasspath == null) ? "" : fromClasspath;
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


    @CacheEvict(value = "templates", key = "#section + '|' + #key + '|' + (#javaVersion==null?'any':#javaVersion) + '|' + (#language==null?'':#language)")
    public void evictCacheFor(String section, String key, String javaVersion, String language) {
        // annotated method triggers cache eviction
    }
}
