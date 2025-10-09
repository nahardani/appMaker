package com.company.appmaker.ai.dto;

public record GenerateRequest(
        String projectId,        // ← اضافه شد
        String provider,
        String model,
        String prompt,
        String basePackage,
        String feature,
        String basePath,
        int javaVersion
) {}





