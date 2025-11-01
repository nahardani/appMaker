package com.company.appmaker.service;


import com.company.appmaker.model.ValueObjectField;
import com.company.appmaker.model.ValueObjectTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ValueObjectRenderer {

    public String renderJava(ValueObjectTemplate t) {
        String pkg = t.getPackageName() == null ? "com.company.common.vo" : t.getPackageName().trim();
        boolean isRecord = "record".equalsIgnoreCase(t.getJavaType());
        List<ValueObjectField> fields = t.getFields() == null ? List.of() : t.getFields();

        String className = t.getId(); // فرض: id همان نام کلاس است
        StringBuilder sb = new StringBuilder();

        // package & imports
        sb.append("package ").append(pkg).append(";\n\n");
        boolean needsJsonValue = Boolean.TRUE.equals(t.getJacksonAsPrimitive()) && fields.size()==1;
        if (needsJsonValue) sb.append("import com.fasterxml.jackson.annotation.JsonValue;\n");
        if (fields.stream().anyMatch(f -> "java.math.BigDecimal".equals(f.getType()))) {
            sb.append("import java.math.BigDecimal;\n");
        }
        if (fields.stream().anyMatch(f -> f.getType().startsWith("java.time."))) {
            sb.append("import java.time.*;\n");
        }
        if (sb.charAt(sb.length()-1) != '\n') sb.append("\n");

        // javadoc
        if (t.getDescription()!=null && !t.getDescription().isBlank()){
            sb.append("/** ").append(t.getDescription()).append(" */\n");
        }

        // header
        if (isRecord) {
            sb.append("public record ").append(className).append("(")
                    .append(fields.stream()
                            .map(f -> f.getType() + " " + f.getName())
                            .collect(Collectors.joining(", ")))
                    .append(") {\n");

            // compact constructor (invariants)
            var invariants = t.getInvariants();
            if (invariants!=null && !invariants.isEmpty()){
                sb.append("  public ").append(className).append(" {\n");
                // نمونهٔ ساده از قیود
                for (ValueObjectField f : fields) {
                    Map<String,Object> c = f.getConstraints();
                    if (c==null) continue;
                    if (Boolean.TRUE.equals(c.get("notNull"))) {
                        sb.append("    if (").append(f.getName()).append(" == null) throw new IllegalArgumentException(\"")
                                .append(f.getName()).append(" is required\");\n");
                    }
                    if (c.get("regex") instanceof String rx) {
                        sb.append("    if (").append(f.getName()).append(" != null && !")
                                .append(f.getName()).append(".toString().matches(\"").append(rx).append("\")) ")
                                .append("throw new IllegalArgumentException(\"invalid ").append(f.getName()).append("\");\n");
                    }
                    if (Boolean.TRUE.equals(c.get("uppercase"))) {
                        sb.append("    if (").append(f.getName()).append(" != null) ")
                                .append(f.getName()).append(" = ").append(f.getName()).append(".toString().toUpperCase();\n");
                    }
                    if (Boolean.TRUE.equals(c.get("lowercase"))) {
                        sb.append("    if (").append(f.getName()).append(" != null) ")
                                .append(f.getName()).append(" = ").append(f.getName()).append(".toString().toLowerCase();\n");
                    }
                }
                // invariants سفارشی
                for (String inv : invariants) {
                    sb.append("    // invariant: ").append(inv).append("\n");
                }
                sb.append("  }\n");
            }

            // @JsonValue برای تک‌فیلدی
            if (needsJsonValue) {
                ValueObjectField f = fields.get(0);
                sb.append("  @JsonValue public ").append(f.getType()).append(" value(){ return ").append(f.getName()).append("; }\n");
            }

            sb.append("}\n");

        } else {
            // class
            sb.append("public final class ").append(className).append(" {\n");

            // fields
            for (ValueObjectField f : fields) {
                sb.append("  private ").append(f.getType()).append(" ").append(f.getName()).append(";\n");
            }
            sb.append("\n  public ").append(className).append("(")
                    .append(fields.stream().map(f -> f.getType()+" "+f.getName()).collect(Collectors.joining(", ")))
                    .append("){\n");
            for (ValueObjectField f : fields) {
                sb.append("    this.").append(f.getName()).append(" = ").append(f.getName()).append(";\n");
            }
            sb.append("  }\n");
            // getters
            for (ValueObjectField f : fields) {
                sb.append("  public ").append(f.getType()).append(" ").append(f.getName()).append("(){ return ").append(f.getName()).append("; }\n");
            }
            sb.append("}\n");
        }

        return sb.toString();
    }
}
