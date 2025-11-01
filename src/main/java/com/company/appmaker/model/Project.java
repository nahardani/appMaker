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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
    private List<GeneratedFile> generatedFiles = new ArrayList<>();
    @Column(name = "root_path", nullable = false)
    private String rootPath;
    private String profileId;



    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GeneratedFile {
        private String path;    // e.g. src/main/java/com/example/controller/FooController.java
        private String content; // file content
    }

    public Project(String projectName, String companyName) {
        this.projectName = projectName;
        this.companyName = companyName;
        this.createdAt = Instant.now();
    }


}