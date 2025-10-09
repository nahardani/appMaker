package com.company.appmaker.ai.dto;

import java.util.List;

public record ApplyRequest(
        String projectRoot, // مسیر ریشه پروژه (مثلا absolute)
        List<CodeFile> files
) {
}
