package com.company.appmaker.ai.dto;


import java.util.List;
import java.util.Map;

public record PropForPrompt(
        String id,               // همون _id مثل "tx.status"
        String group,            // مثلا "transaction"
        String dataType,         // مثل "Enum:TxStatus"
        String description,
        List<String> synonyms,
        String example,
        Map<String, Object> rules // هرچه هست برای ولیدیشن/محدودیت‌ها
) {}