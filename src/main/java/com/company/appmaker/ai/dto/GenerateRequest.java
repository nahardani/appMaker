package com.company.appmaker.ai.dto;

import java.util.Objects;

public record GenerateRequest(
        String provider,
        String model,
        String promptId,
        String prompt,

        String projectId,
        String basePackage,
        String basePath,
        int javaVersion,

        // جدید:
        String controllerName,
        String endpointName,

        boolean needsExternal,
        String externalBaseUrl,
        String externalAuthType, // NONE|BASIC|BEARER|API_KEY
        int    externalTimeout,  // ms

        boolean needsDb,
        String dbType // mongo | jpa
) {
    public int javaVersionSafe(){ return javaVersion > 0 ? javaVersion : 17; }
    public String externalAuthTypeSafe(){ return (externalAuthType==null || externalAuthType.isBlank()) ? "NONE" : externalAuthType; }
    public int externalTimeoutSafe(){ return externalTimeout > 0 ? externalTimeout : 5000; }
    public String dbTypeSafe(){ return (dbType==null || dbType.isBlank()) ? "mongo" : dbType; }
}





