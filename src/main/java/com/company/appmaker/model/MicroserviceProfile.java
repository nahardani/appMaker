package com.company.appmaker.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("microservice_profiles")
public class MicroserviceProfile {
    @Id
    private String id;

    private String name;          // عنوان نمایشی پروفایل (مثلا "Web + Mongo + OpenAPI (J17)")
    private String serviceName;   // مثل orders
    private String basePackage;   // مثل com.company.orders
    private String basePath;      // مثل /api/orders
    private String apiVersion;    // مثل v1
    private String javaVersion;   // "8" | "11" | "17" | "21"

    // گزینه‌ها
    private boolean useMongo = true;
    private boolean useUlidIds;
    private boolean enableActuator;
    private boolean enableOpenApi;
    private boolean enableValidation;
    private boolean enableMetrics;
    private boolean enableSecurityBasic;
    private boolean addDockerfile;
    private boolean addCompose;
    private boolean enableTestcontainers;

    /** تبدیل به ساختار پروژه برای کپی هنگام ساخت */
    public MicroserviceSettings toMs() {
        var ms = new MicroserviceSettings();
        ms.setServiceName(serviceName);
        ms.setBasePackage(basePackage);
        ms.setBasePath(basePath);
        ms.setApiVersion(apiVersion);
        ms.setJavaVersion(javaVersion);
        ms.setUseMongo(useMongo);
        ms.setUseUlidIds(useUlidIds);
        ms.setEnableActuator(enableActuator);
        ms.setEnableOpenApi(enableOpenApi);
        ms.setEnableValidation(enableValidation);
        ms.setEnableMetrics(enableMetrics);
        ms.setEnableSecurityBasic(enableSecurityBasic);
        ms.setAddDockerfile(addDockerfile);
        ms.setAddCompose(addCompose);
        ms.setEnableTestcontainers(enableTestcontainers);
        return ms;
    }
}
