package com.company.appmaker.ai.dto;

import java.util.Objects;

public record GenerateRequest(
        String projectId,
        String provider,
        String model,
        String prompt,
        String promptId,
        String basePackage,
        String basePath,
        Integer javaVersion,
        String controllerName,
        String endpointName,

        // ⬇️ فیلدهای Preflight
        Boolean needsExternal,
        Boolean needsDb,

        String externalBaseUrl,
        String externalPathTemplate,   // e.g. "/facilities/{facilityId}"
        String externalHttpMethod,     // GET|POST|PUT|PATCH|DELETE
        String externalAuthType,       // NONE|BASIC|BEARER|API_KEY

        Integer timeoutMs,
        Integer retryMaxAttempts,
        Integer retryBackoffMs
) {
    public int javaVersionSafe(){ return (javaVersion != null && javaVersion > 0) ? javaVersion : 21; }
}






