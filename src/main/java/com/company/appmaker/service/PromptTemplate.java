package com.company.appmaker.service;

import com.company.appmaker.enums.PromptScope;
import com.company.appmaker.enums.PromptStatus;
import com.company.appmaker.enums.PromptTarget;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "prompt_templates")
public class PromptTemplate {
    @Id
    private String id;

    @Indexed
    private String category = "1";       // فعلاً همه = "1"
    private PromptTarget target;         // CONTROLLER/SERVICE/REPOSITORY/DTO/ENTITY/TEST/ANY
    private PromptScope scope = PromptScope.GLOBAL; // GLOBAL/PROJECT
    private String projectId;            // وقتی scope=PROJECT

    private String name;
    private String javaMin;              // "8","11","17","21"
    private String javaMax;
    private String tags;
    private String body;                 // متن پرامپت
    private PromptStatus status = PromptStatus.ACTIVE;
    private Long version = 1L;

    private java.time.Instant createdAt = java.time.Instant.now();
    private java.time.Instant updatedAt = java.time.Instant.now();


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public PromptTarget getTarget() {
        return target;
    }

    public void setTarget(PromptTarget target) {
        this.target = target;
    }

    public PromptScope getScope() {
        return scope;
    }

    public void setScope(PromptScope scope) {
        this.scope = scope;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJavaMin() {
        return javaMin;
    }

    public void setJavaMin(String javaMin) {
        this.javaMin = javaMin;
    }

    public String getJavaMax() {
        return javaMax;
    }

    public void setJavaMax(String javaMax) {
        this.javaMax = javaMax;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public PromptStatus getStatus() {
        return status;
    }

    public void setStatus(PromptStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}





