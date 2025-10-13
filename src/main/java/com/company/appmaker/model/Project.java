package com.company.appmaker.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.company.appmaker.model.coctroller.ControllerDef;
import com.company.appmaker.model.externalApi.ExternalApisSettings;
import com.company.appmaker.model.l18n.I18nSettings;
import com.company.appmaker.model.logging.LoggingSettings;
import com.company.appmaker.model.profile.ProfileSettings;
import com.company.appmaker.model.security.SecuritySettings;
import com.company.appmaker.model.swagger.SwaggerSettings;
import jakarta.persistence.Column;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "projects")
public class Project {
    @Id
    private String id;
    private String projectName;
    private String companyName;
    private Instant createdAt;
    private String javaVersion;
    private List<String> packages = new ArrayList<>();
    private List<ControllerDef> controllers = new ArrayList<>();
    private SecuritySettings security = new SecuritySettings();
    private SwaggerSettings swagger = new SwaggerSettings();
    private ProfileSettings profiles;
    private MicroserviceSettings ms = new MicroserviceSettings();
    private I18nSettings i18n;
    private ExternalApisSettings externalApis;
    private LoggingSettings logging;
    private ConstantsSettings constants;
    private java.util.List<GeneratedFile> generatedFiles = new java.util.ArrayList<>();
    @Column(name = "root_path", nullable = false)
    private String rootPath;
    public static class GeneratedFile {
        private String path;    // e.g. src/main/java/com/example/controller/FooController.java
        private String content; // file content

        public GeneratedFile() {
        }

        public GeneratedFile(String path, String content) {
            this.path = path;
            this.content = content;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String p) {
            this.path = p;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String c) {
            this.content = c;
        }
    }
    public Project() {}


    public Project(String projectName, String companyName) {
        this.projectName = projectName;
        this.companyName = companyName;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public List<String> getPackages() {
        return packages;
    }

    public void setPackages(List<String> packages) {
        this.packages = packages;
    }

    public List<ControllerDef> getControllers() {
        return controllers;
    }

    public void setControllers(List<ControllerDef> controllers) {
        this.controllers = controllers;
    }

    public SecuritySettings getSecurity() {
        return security;
    }

    public void setSecurity(SecuritySettings security) {
        this.security = security;
    }

    public SwaggerSettings getSwagger() {
        return swagger;
    }

    public void setSwagger(SwaggerSettings swagger) {
        this.swagger = swagger;
    }

    public ProfileSettings getProfiles() {
        return profiles;
    }

    public void setProfiles(ProfileSettings profiles) {
        this.profiles = profiles;
    }

    public MicroserviceSettings getMs() {
        return ms;
    }

    public void setMs(MicroserviceSettings ms) {
        this.ms = ms;
    }

    public I18nSettings getI18n() {
        return i18n;
    }

    public void setI18n(I18nSettings i18n) {
        this.i18n = i18n;
    }

    public ExternalApisSettings getExternalApis() {
        return externalApis;
    }

    public void setExternalApis(ExternalApisSettings externalApis) {
        this.externalApis = externalApis;
    }

    public LoggingSettings getLogging() {
        return logging;
    }

    public void setLogging(LoggingSettings logging) {
        this.logging = logging;
    }

    public ConstantsSettings getConstants() {
        return constants;
    }

    public void setConstants(ConstantsSettings constants) {
        this.constants = constants;
    }

    public List<GeneratedFile> getGeneratedFiles() {
        return generatedFiles;
    }

    public void setGeneratedFiles(List<GeneratedFile> generatedFiles) {
        this.generatedFiles = generatedFiles;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }
}