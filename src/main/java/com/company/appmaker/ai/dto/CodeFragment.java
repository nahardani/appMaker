package com.company.appmaker.ai.dto;

public record CodeFragment(
        String path,       // مسیر فایل
        String markerType, // AI-ENDPOINT یا AI-SERVICE یا AI-SERVICE-IMPL
        String name,       // endpointName
        String content     // کل بلوک
) {}

