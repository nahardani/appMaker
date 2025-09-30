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
    // com.company.appmaker.model.Project
    private ProfileSettings profiles;

    public ProfileSettings getProfiles() { return profiles; }
    public void setProfiles(ProfileSettings profiles) { this.profiles = profiles; }

    // com.company.appmaker.model.Project
    private I18nSettings i18n;

    public I18nSettings getI18n() { return i18n; }
    public void setI18n(I18nSettings i18n) { this.i18n = i18n; }

    // com.company.appmaker.model.Project
    private ExternalApisSettings externalApis;
    public ExternalApisSettings getExternalApis(){ return externalApis; }
    public void setExternalApis(ExternalApisSettings s){ this.externalApis = s; }


    // ...
    private LoggingSettings logging;
    public LoggingSettings getLogging(){ return logging; }
    public void setLogging(LoggingSettings logging){ this.logging = logging; }



    public Project() {
    }


    public Project(String projectName, String companyName) {
        this.projectName = projectName;
        this.companyName = companyName;
        this.createdAt = Instant.now();
    }

    public SwaggerSettings getSwagger() {
        return swagger;
    }

    public void setSwagger(SwaggerSettings swagger) {
        this.swagger = swagger;
    }

    public SecuritySettings getSecurity(){ return security; }
    public void setSecurity(SecuritySettings security){ this.security = security; }
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

    public java.util.List<String> getPackages() {
        return packages;
    }

    public void setPackages(java.util.List<String> packages) {
        this.packages = packages;
    }

    public java.util.List<ControllerDef> getControllers() {
        return controllers;
    }

    public void setControllers(java.util.List<ControllerDef> controllers) {
        this.controllers = controllers;
    }
}