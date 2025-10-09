package com.company.appmaker.ai.dto;

public record CodeMeta(
        String language,
        String framework,
        int javaVersion,
        String module
) {}