package com.company.appmaker.controller.forms;

import com.company.appmaker.model.SwaggerSettings;

public class SwaggerForm {
    private Boolean enabled;
    private String title;
    private String version;
    private String uiPath;
    private String defaultGroup;

    private String description;
    private String contactName;
    private String contactEmail;
    private String licenseName;
    private String licenseUrl;

    // getters/setters...

    public static SwaggerForm from(SwaggerSettings s) {
        SwaggerForm f = new SwaggerForm();
        if (s == null) {
            f.enabled = true; f.title = "Project API"; f.version = "v1"; f.uiPath = "/swagger-ui";
            return f;
        }
        f.enabled = s.isEnabled();
        f.title = s.getTitle();
        f.version = s.getVersion();
        f.uiPath = s.getUiPath();
        f.defaultGroup = s.getDefaultGroup();
        f.description = s.getDescription();
        f.contactName = s.getContactName();
        f.contactEmail = s.getContactEmail();
        f.licenseName = s.getLicenseName();
        f.licenseUrl = s.getLicenseUrl();
        return f;
    }

    public void applyTo(SwaggerSettings s) {
        s.setEnabled(Boolean.TRUE.equals(this.enabled));
        s.setTitle(n(title,"Project API"));
        s.setVersion(n(version,"v1"));
        s.setUiPath(n(uiPath,"/swagger-ui"));
        s.setDefaultGroup(z(defaultGroup));
        s.setDescription(z(description));
        s.setContactName(z(contactName));
        s.setContactEmail(z(contactEmail));
        s.setLicenseName(z(licenseName));
        s.setLicenseUrl(z(licenseUrl));
    }

    private static String n(String v, String def){ return (v==null || v.isBlank()) ? def : v.trim(); }
    private static String z(String v){ return (v==null || v.isBlank()) ? null : v.trim(); }


    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUiPath() {
        return uiPath;
    }

    public void setUiPath(String uiPath) {
        this.uiPath = uiPath;
    }

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }
}
