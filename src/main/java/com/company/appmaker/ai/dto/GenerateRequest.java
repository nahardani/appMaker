package com.company.appmaker.ai.dto;

public record GenerateRequest(
        String projectId,
        String provider,
        String model,
        String prompt,     // اختیاری (اگر promptId نباشد)
        String promptId,     // ← جدید: شناسه پرامپت ذخیره‌شده
        String basePackage,
        String feature,
        String basePath,
        int javaVersion
) {}





