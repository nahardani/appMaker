package com.company.appmaker.ai.dto;

import java.util.List;

public record GenerateResult(
        CodeMeta meta,
        List<CodeFile> files,
        String raw      // متن خام برای نمایش در UI
) {
}
