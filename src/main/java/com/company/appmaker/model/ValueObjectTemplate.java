package com.company.appmaker.model;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Document("value_object_templates")
public class ValueObjectTemplate {
    @Id
    private String id;               // مثال: "EmailAddress" (کلید یکتا و human-readable)
    private String name;             // عنوان نمایشی (اختیاری؛ می‌تواند == id)
    private String category;         // "banking" | "common" ... (فعلاً "banking")
    private String packageName;      // مثال: "com.company.common.vo"
    private String javaType;         // "record" | "class"
    private String description;

    private List<ValueObjectField> fields;        // تعریف فیلدها
    private List<String> invariants;              // قواعد صحت (java-like assertions)
    private Boolean jacksonAsPrimitive;           // اگر تک‌فیلدی و بخواهیم @JsonValue
    private List<String> mongoIndexOn;            // مسیر ایندکس‌های پیشنهادی
    private List<Map<String, Object>> examples;   // نمونه‌ها (برای UI/Docs)

    private String status;            // "ACTIVE" | "ARCHIVED"
    private Long version;             // 1, 2, ...
    private Instant createdAt;
    private Instant updatedAt;


}
