package com.company.appmaker.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("template_snippets")
public class TemplateSnippet {
    @Id
    private String id;

    // برای انتخاب/مصرف توسط TemplateService
    private String section;       // مثل: "pom", "config", "logging", "security", ...
    private String keyName;       // مثل: "pom.xml", "logback.xml", "application.yml.base"
    private String javaVersion;   // "any", "8", "11", "17", "21"
    private String language;      // مثلا "fa" یا null

    // برای UI مدیریت
    private String title;         // نمایش در لیست
    private String description;   // توضیح کوتاه
    private String content;       // متن قالب

    private List<String> tags;    // اختیاری
    private Instant updatedAt;    // آخرین ویرایش

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
