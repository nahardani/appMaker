package com.company.appmaker.service;

import com.company.appmaker.enums.PromptScope;
import com.company.appmaker.enums.PromptStatus;
import com.company.appmaker.enums.PromptTarget;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
@Data
@Document(collection = "prompt_templates")
public class PromptTemplate {
    @Id
    private String id;

    @Indexed
    private String category = "1";       // فعلاً همه = "1"
    private PromptTarget target;         // CONTROLLER/SERVICE/REPOSITORY/DTO/ENTITY/TEST/ANY
    private PromptScope scope = PromptScope.GLOBAL; // GLOBAL/PROJECT
    private String projectId;            // وقتی scope=PROJECT
    private String name;
    private String javaVersion;              // "8","11","17","21"
    private String tags;
    private String body;                 // متن پرامپت
    private PromptStatus status = PromptStatus.ACTIVE;
    private Long version = 1L;
    private java.time.Instant createdAt = java.time.Instant.now();
    private java.time.Instant updatedAt = java.time.Instant.now();


}





