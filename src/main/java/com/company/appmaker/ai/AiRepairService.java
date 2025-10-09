package com.company.appmaker.ai;

import com.company.appmaker.ai.dto.GenerateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiRepairService {
    private final AiFacade ai;
    private final PlanParser parser;

    public GenerateResult repairToJavaFiles(String raw, String basePackage, String feature, String basePath, int javaVersion) {
        String repairPrompt = """
                Convert the following content into Java %d Spring Boot 3 code files ONLY, following the exact schema:
                - Keep only Java code.
                - Separate files with <FILE path="...">...</FILE> blocks.
                - BasePackage: %s ; Feature: %s ; REST base path: %s
                CONTENT START
                %s
                CONTENT END
                """.formatted(javaVersion, basePackage, feature, basePath, raw);

        String fixedRaw = ai.generate("openai", "", repairPrompt); // یا provider فعلی
        return parser.parse(fixedRaw);
    }
}

