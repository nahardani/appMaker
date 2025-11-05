package com.company.appmaker.ai.dto;

import java.util.Map;

public record VOFieldForPrompt(
        String name,
        String type,
        boolean required,
        Map<String, Object> constraints
) {}