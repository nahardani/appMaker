package com.company.appmaker.model.swagger;

public class SwaggerSettings {
    private boolean enabled = true;
    private String title = "Project API";
    private String version = "v1";
    private String uiPath = "/swagger-ui";
    private String defaultGroup;

    // اختیاری‌های مفید
    private String description;
    private String contactName;
    private String contactEmail;
    private String licenseName;
    private String licenseUrl;

    // getters/setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getUiPath() { return uiPath; }
    public void setUiPath(String uiPath) { this.uiPath = uiPath; }

    public String getDefaultGroup() { return defaultGroup; }
    public void setDefaultGroup(String defaultGroup) { this.defaultGroup = defaultGroup; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getLicenseName() { return licenseName; }
    public void setLicenseName(String licenseName) { this.licenseName = licenseName; }

    public String getLicenseUrl() { return licenseUrl; }
    public void setLicenseUrl(String licenseUrl) { this.licenseUrl = licenseUrl; }
}
