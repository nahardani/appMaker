package com.company.appmaker.boot;


import com.company.appmaker.model.DomainProperty;
import com.company.appmaker.model.ValueObjectField;
import com.company.appmaker.model.ValueObjectTemplate;
import com.company.appmaker.repo.DomainPropertyRepo;
import com.company.appmaker.repo.ValueObjectTemplateRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class VoAndPropertySeeder {

    private final ValueObjectTemplateRepo voRepo;
    private final DomainPropertyRepo propRepo;

    @PostConstruct
    void init() {
        if (voRepo.count() == 0) seedVOs();
        if (propRepo.count() == 0) seedDomainProperties();
    }

    private void seedVOs() {
        Instant now = Instant.now();
        String pkg = "com.company.common.vo";

        // EmailAddress
        voRepo.save(new ValueObjectTemplateBuilder()
                .id("EmailAddress").name("Email Address").category("banking")
                .packageName(pkg).javaType("record").description("Normalized lowercase email with basic RFC validation")
                .fields(List.of(
                        field("value", "String", Map.of("regex", "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$","notNull", true), "email string")
                ))
                .invariants(List.of("value != null", "value.matches(regex)"))
                .jacksonAsPrimitive(true)
                .examples(List.of(Map.of("value","user@example.com")))
                .status("ACTIVE").version(1L).createdAt(now).updatedAt(now)
                .build()
        );

        // PhoneNumber (E.164)
        voRepo.save(new ValueObjectTemplateBuilder()
                .id("PhoneNumber").name("Phone Number").category("banking")
                .packageName(pkg).javaType("record").description("E.164 normalized phone number")
                .fields(List.of(
                        field("value","String", Map.of("regex","^\\+?[1-9]\\d{7,14}$","notNull",true), "E.164 format")
                ))
                .jacksonAsPrimitive(true)
                .examples(List.of(Map.of("value","+989121234567")))
                .status("ACTIVE").version(1L).createdAt(now).updatedAt(now)
                .build()
        );

        // Money
        voRepo.save(new ValueObjectTemplateBuilder()
                .id("Money").name("Money").category("banking")
                .packageName(pkg).javaType("record").description("Amount + Currency with scale limit")
                .fields(List.of(
                        field("amount","java.math.BigDecimal", Map.of("min",0,"scaleMax",9,"notNull",true), "monetary amount"),
                        field("currency","java.util.Currency", Map.of("notNull",true), "ISO 4217")
                ))
                .invariants(List.of("amount != null", "currency != null"))
                .examples(List.of(Map.of("amount","12.50","currency","USD")))
                .status("ACTIVE").version(1L).createdAt(now).updatedAt(now)
                .build()
        );

        // IBAN
        voRepo.save(new ValueObjectTemplateBuilder()
                .id("Iban").name("IBAN").category("banking")
                .packageName(pkg).javaType("record").description("International Bank Account Number (normalized)")
                .fields(List.of(
                        field("value","String", Map.of("regex","^[A-Z]{2}\\d{2}[A-Z0-9]{11,30}$","uppercase",true,"notNull",true), "IBAN")
                ))
                .jacksonAsPrimitive(true)
                .examples(List.of(Map.of("value","DE89370400440532013000")))
                .status("ACTIVE").version(1L).createdAt(now).updatedAt(now)
                .build()
        );

        // SWIFT/BIC
        voRepo.save(new ValueObjectTemplateBuilder()
                .id("SwiftBic").name("SWIFT/BIC").category("banking")
                .packageName(pkg).javaType("record").description("Bank SWIFT/BIC code (8 or 11 chars)")
                .fields(List.of(
                        field("value","String", Map.of("regex","^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$","uppercase",true,"notNull",true), "BIC")
                ))
                .jacksonAsPrimitive(true)
                .examples(List.of(Map.of("value","DEUTDEFF")))
                .status("ACTIVE").version(1L).createdAt(now).updatedAt(now)
                .build()
        );

        // CardExpiry (YYYY-MM)
        voRepo.save(new ValueObjectTemplateBuilder()
                .id("CardExpiry").name("Card Expiry").category("banking")
                .packageName(pkg).javaType("record").description("Card expiry year-month (future date)")
                .fields(List.of(
                        field("year","int", Map.of("min",2000,"max",2100,"notNull",true), "year"),
                        field("month","int", Map.of("min",1,"max",12,"notNull",true), "month")
                ))
                .invariants(List.of("isFutureOrCurrent(year,month)"))
                .examples(List.of(Map.of("year",2027,"month",12)))
                .status("ACTIVE").version(1L).createdAt(now).updatedAt(now)
                .build()
        );

        // TokenizedCard
        voRepo.save(new ValueObjectTemplateBuilder()
                .id("TokenizedCard").name("Tokenized Card").category("banking")
                .packageName(pkg).javaType("record").description("Card PAN replaced by token; do not store raw PAN")
                .fields(List.of(
                        field("token","String", Map.of("maxLen",64,"notNull",true), "opaque token")
                ))
                .jacksonAsPrimitive(true)
                .examples(List.of(Map.of("token","tok_4f3a...")))
                .status("ACTIVE").version(1L).createdAt(now).updatedAt(now)
                .build()
        );

        // Address
        voRepo.save(new ValueObjectTemplateBuilder()
                .id("Address").name("Address").category("banking")
                .packageName(pkg).javaType("record").description("Postal address")
                .fields(List.of(
                        field("street","String", Map.of("maxLen",200), null),
                        field("city","String", Map.of("maxLen",100), null),
                        field("postalCode","String", Map.of("maxLen",20), null),
                        field("country","String", Map.of("regex","^[A-Z]{2}$"), "ISO 3166-1 alpha-2")
                ))
                .examples(List.of(Map.of("street","Main 1","city","Tehran","postalCode","12345","country","IR")))
                .status("ACTIVE").version(1L).createdAt(now).updatedAt(now)
                .build()
        );

        // ULID
        voRepo.save(new ValueObjectTemplateBuilder()
                .id("Ulid").name("ULID").category("banking")
                .packageName(pkg).javaType("record").description("Sortable unique id for Mongo")
                .fields(List.of(
                        field("value","String", Map.of("regex","^[0-7][0-9A-HJKMNP-TV-Z]{25}$","notNull",true), "Crockford's Base32")
                ))
                .jacksonAsPrimitive(true)
                .examples(List.of(Map.of("value","01J9H2QW8B2K2K2Y86ZJB6GZ9X")))
                .status("ACTIVE").version(1L).createdAt(now).updatedAt(now)
                .build()
        );

        // Percentage
        voRepo.save(new ValueObjectTemplateBuilder()
                .id("Percentage").name("Percentage").category("banking")
                .packageName(pkg).javaType("record").description("0..100 with scale limit")
                .fields(List.of(
                        field("value","java.math.BigDecimal", Map.of("min",0,"max",100,"scaleMax",4,"notNull",true), "%")
                ))
                .jacksonAsPrimitive(true)
                .examples(List.of(Map.of("value","12.5")))
                .status("ACTIVE").version(1L).createdAt(now).updatedAt(now)
                .build()
        );

        // DateRange
        voRepo.save(new ValueObjectTemplateBuilder()
                .id("DateRange").name("Date Range").category("banking")
                .packageName(pkg).javaType("class").description("start <= end; LocalDate")
                .fields(List.of(
                        field("start","java.time.LocalDate", Map.of("notNull",true), null),
                        field("end","java.time.LocalDate", Map.of("notNull",true), null)
                ))
                .invariants(List.of("!end.isBefore(start)"))
                .examples(List.of(Map.of("start","2025-01-01","end","2025-12-31")))
                .status("ACTIVE").version(1L).createdAt(now).updatedAt(now)
                .build()
        );
    }

    private void seedDomainProperties() {
        Instant now = Instant.now();

        // ===== Customer / Identity
        propRepo.saveAll(List.of(
                prop("customer.firstName","customer","String","نام کوچک مشتری", List.of("fname","givenName"), Map.of("maxLen",80),"Ali","ACTIVE",now),
                prop("customer.lastName","customer","String","نام خانوادگی مشتری", List.of("lname","surname"), Map.of("maxLen",80),"Rahimi","ACTIVE",now),
                prop("customer.fullName","customer","String","نام کامل مشتری", List.of("name"), Map.of("maxLen",160),"Ali Rahimi","ACTIVE",now),
                prop("customer.nationalId","customer","String","شناسه/کد ملی", List.of("nid","nationalCode"), Map.of("regex","^[0-9A-Za-z\\-]{6,20}$"),"0081234567","ACTIVE",now),
                prop("customer.email","customer","EmailAddress","ایمیل", List.of("mail"), Map.of(),"user@example.com","ACTIVE",now),
                prop("customer.phone","customer","PhoneNumber","تلفن همراه", List.of("mobile","msisdn"), Map.of(),"(+98912...)","ACTIVE",now),
                prop("customer.birthDate","customer","LocalDate","تاریخ تولد", List.of(), Map.of(),"1990-01-01","ACTIVE",now),
                prop("customer.address","customer","Address","نشانی پستی", List.of(), Map.of(),"—","ACTIVE",now),
                prop("customer.customerId","customer","Ulid","شناسه مشتری", List.of("cid"), Map.of(),"01J9...","ACTIVE",now)
        ));

        // ===== Account / Banking
        propRepo.saveAll(List.of(
                prop("account.accountId","account","Ulid","شناسه حساب", List.of("aid"), Map.of(),"01J9...","ACTIVE",now),
                prop("account.iban","account","Iban","IBAN حساب", List.of("sheba"), Map.of(),"DE89370400440532013000","ACTIVE",now),
                prop("account.swift","account","SwiftBic","کد SWIFT بانک", List.of("bic"), Map.of(),"DEUTDEFF","ACTIVE",now),
                prop("account.numberMasked","account","String","شماره حساب ماسک‌شده", List.of("acctMasked"), Map.of("mask",true,"maxLen",34),"****1234","ACTIVE",now),
                prop("account.currency","account","String","کد ارز حساب", List.of("ccy"), Map.of("regex","^[A-Z]{3}$"),"IRR","ACTIVE",now),
                prop("account.balance","account","Money","مانده حساب", List.of(), Map.of(),"100.00 IRR","ACTIVE",now)
        ));

        // ===== Card
        propRepo.saveAll(List.of(
                prop("card.token","card","TokenizedCard","توکن کارت", List.of("cardToken"), Map.of(),"tok_xxx","ACTIVE",now),
                prop("card.brand","card","Enum:CardBrand","برند کارت", List.of(), Map.of(), "VISA","ACTIVE",now),
                prop("card.expiry","card","CardExpiry","تاریخ انقضا کارت", List.of(), Map.of(),"2027-12","ACTIVE",now),
                prop("card.holderName","card","String","نام دارنده کارت", List.of(), Map.of("maxLen",60),"ALI RAHIMI","ACTIVE",now),
                prop("card.panMasked","card","String","PAN ماسک‌شده", List.of("maskedPan"), Map.of("mask",true,"maxLen",19),"**** **** **** 1234","ACTIVE",now)
        ));

        // ===== Payment / Transfer
        propRepo.saveAll(List.of(
                prop("payment.amount","payment","Money","مبلغ پرداخت", List.of(), Map.of(),"250000 IRR","ACTIVE",now),
                prop("payment.currency","payment","String","ارز پرداخت", List.of(), Map.of("regex","^[A-Z]{3}$"),"IRR","ACTIVE",now),
                prop("payment.reference","payment","String","شناسه ارجاع پرداخت", List.of("paymentRef"), Map.of("maxLen",64),"PMT-2025-001","ACTIVE",now),
                prop("transfer.srcIban","payment","Iban","IBAN مبدا", List.of("fromIban"), Map.of(),"IR...","ACTIVE",now),
                prop("transfer.dstIban","payment","Iban","IBAN مقصد", List.of("toIban"), Map.of(),"IR...","ACTIVE",now),
                prop("transfer.fee","payment","Money","کارمزد انتقال", List.of(), Map.of(),"5000 IRR","ACTIVE",now),
                prop("fx.rate","payment","java.math.BigDecimal","نرخ تبدیل", List.of("exchangeRate"), Map.of("min",0,"scaleMax",9),"580000.0001","ACTIVE",now)
        ));

        // ===== Loan / Credit
        propRepo.saveAll(List.of(
                prop("loan.loanId","loan","Ulid","شناسه وام", List.of(), Map.of(),"01J9...","ACTIVE",now),
                prop("loan.amount","loan","Money","مبلغ وام", List.of(), Map.of(),"2_000_000_000 IRR","ACTIVE",now),
                prop("loan.termMonths","loan","Integer","مدت وام (ماه)", List.of(), Map.of("min",1,"max",600),"24","ACTIVE",now),
                prop("loan.interestRate","loan","Percentage","نرخ سود", List.of(), Map.of(),"18.0","ACTIVE",now),
                prop("loan.startDate","loan","LocalDate","تاریخ شروع", List.of(), Map.of(),"2025-01-01","ACTIVE",now),
                prop("loan.endDate","loan","LocalDate","تاریخ پایان", List.of(), Map.of(),"2027-01-01","ACTIVE",now)
        ));

        // ===== Cheque
        propRepo.saveAll(List.of(
                prop("cheque.number","cheque","String","شماره چک", List.of(), Map.of("regex","^[0-9A-Za-z\\-]{4,32}$"),"A-123456","ACTIVE",now),
                prop("cheque.series","cheque","String","سری/سریال", List.of(), Map.of("maxLen",16),"S-01","ACTIVE",now),
                prop("cheque.issueDate","cheque","LocalDate","تاریخ صدور", List.of(), Map.of(),"2025-02-01","ACTIVE",now),
                prop("cheque.dueDate","cheque","LocalDate","تاریخ سررسید", List.of(), Map.of(),"2025-03-01","ACTIVE",now),
                prop("cheque.amount","cheque","Money","مبلغ چک", List.of(), Map.of(),"10000000 IRR","ACTIVE",now),
                prop("cheque.ownerName","cheque","String","صاحب چک", List.of(), Map.of("maxLen",80),"Ali Rahimi","ACTIVE",now)
        ));

        // ===== Transaction
        propRepo.saveAll(List.of(
                prop("tx.id","transaction","Ulid","شناسه تراکنش", List.of("transactionId"), Map.of(),"01J9...","ACTIVE",now),
                prop("tx.type","transaction","Enum:TxType","نوع تراکنش", List.of(), Map.of(),"DEBIT","ACTIVE",now),
                prop("tx.amount","transaction","Money","مبلغ تراکنش", List.of(), Map.of(),"100000 IRR","ACTIVE",now),
                prop("tx.timestamp","transaction","java.time.Instant","زمان تراکنش", List.of(), Map.of(),"2025-10-12T09:00:00Z","ACTIVE",now),
                prop("tx.status","transaction","Enum:TxStatus","وضعیت", List.of(), Map.of(),"SUCCESS","ACTIVE",now),
                prop("tx.reference","transaction","String","شناسه ارجاع", List.of("ref"), Map.of("maxLen",64),"REF-001","ACTIVE",now)
        ));
    }

    // ===== helper builders =====
    private static ValueObjectField field(String name, String type, Map<String,Object> c, String desc){
        ValueObjectField f = new ValueObjectField();
        f.setName(name); f.setType(type); f.setConstraints(c); f.setDescription(desc);
        return f;
    }

    private static DomainProperty prop(String id, String group, String dataType, String desc,
                                       List<String> syn, Map<String,Object> rules, String example,
                                       String status, Instant now) {
        DomainProperty p = new DomainProperty();
        p.setId(id);
        p.setGroup(group);
        p.setDisplayName(null);
        p.setDataType(dataType);
        p.setDescription(desc);
        p.setSynonyms(syn);
        p.setRules(rules);
        p.setEnumValues(null);
        p.setExample(example);
        p.setStatus(status);
        p.setVersion(1L);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return p;
    }

    // یک بیلدر خیلی ساده برای پرکردن ValueObjectTemplate
    private static class ValueObjectTemplateBuilder {
        private final ValueObjectTemplate t = new ValueObjectTemplate();
        ValueObjectTemplateBuilder id(String v){ t.setId(v); return this; }
        ValueObjectTemplateBuilder name(String v){ t.setName(v); return this; }
        ValueObjectTemplateBuilder category(String v){ t.setCategory(v); return this; }
        ValueObjectTemplateBuilder packageName(String v){ t.setPackageName(v); return this; }
        ValueObjectTemplateBuilder javaType(String v){ t.setJavaType(v); return this; }
        ValueObjectTemplateBuilder description(String v){ t.setDescription(v); return this; }
        ValueObjectTemplateBuilder fields(List<ValueObjectField> v){ t.setFields(v); return this; }
        ValueObjectTemplateBuilder invariants(List<String> v){ t.setInvariants(v); return this; }
        ValueObjectTemplateBuilder jacksonAsPrimitive(Boolean v){ t.setJacksonAsPrimitive(v); return this; }
        ValueObjectTemplateBuilder mongoIndexOn(List<String> v){ t.setMongoIndexOn(v); return this; }
        ValueObjectTemplateBuilder examples(List<Map<String,Object>> v){ t.setExamples(v); return this; }
        ValueObjectTemplateBuilder status(String v){ t.setStatus(v); return this; }
        ValueObjectTemplateBuilder version(Long v){ t.setVersion(v); return this; }
        ValueObjectTemplateBuilder createdAt(Instant v){ t.setCreatedAt(v); return this; }
        ValueObjectTemplateBuilder updatedAt(Instant v){ t.setUpdatedAt(v); return this; }
        ValueObjectTemplate build(){ return t; }
    }
}

