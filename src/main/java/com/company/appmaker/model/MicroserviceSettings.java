package com.company.appmaker.model;

import lombok.Data;

@Data
public class MicroserviceSettings {
    private String serviceName;
    private String basePackage;
    private String basePath;
    private String apiVersion = "v1";

    private String javaVersion = "17";
    private boolean useMongo = true;
    private boolean useUlidIds = true;

    private boolean enableActuator = true;
    private boolean enableOpenApi = true;
    private boolean enableValidation = true;
    private boolean enableMetrics = true;
    private boolean enableSecurityBasic = false;

    private boolean addDockerfile = false;
    private boolean addCompose = false;
    private boolean enableTestcontainers = false;

}
