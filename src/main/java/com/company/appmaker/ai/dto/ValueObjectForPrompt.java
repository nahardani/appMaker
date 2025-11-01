package com.company.appmaker.ai.dto;

import java.util.List;


public record ValueObjectForPrompt(
        String name,
        List<VOFieldForPrompt> fields
) {}