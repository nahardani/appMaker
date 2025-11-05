package com.company.appmaker.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AiArtifact {
    private String type;        // controller-method | service-method | dto | ...
    private String name;        // getBalance
    private String content;     // متن متد یا کلاس
    private String pathHint;    // برای DTOها مسیر پیشنهادی
}

