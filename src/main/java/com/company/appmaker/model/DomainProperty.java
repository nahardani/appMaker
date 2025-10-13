package com.company.appmaker.model;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Document("domain_properties")
public class DomainProperty {
    @Id
    private String id;          // کلید یکتا: مثل "customer.nationalId" یا "account.iban"
    private String group;       // دسته‌ی منطقی: "customer","account","card","loan","payment","cheque","transaction",...
    private String displayName; // نام نمایشی (اختیاری)
    private String dataType;    // "String","Long","BigDecimal","LocalDate","Enum:CardBrand", ...
    private String description; // توضیح کوتاه دامین

    private List<String> synonyms;       // معادل‌ها/کلیدواژه‌ها
    private Map<String, Object> rules;   // مثل: {regex:"...", maxLen:..., min:..., max:...}
    private List<String> enumValues;     // برای DataTypeهای Enum
    private String example;              // یک نمونه‌ی معنادار
    private String status;               // "ACTIVE" | "ARCHIVED"

    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

}
