package com.company.appmaker.service;

import com.company.appmaker.model.FieldDef;
import com.company.appmaker.model.Project;
import com.company.appmaker.model.ResponsePartDef;
import com.company.appmaker.model.coctroller.ControllerDef;
import com.company.appmaker.model.coctroller.EndpointDef;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.company.appmaker.ai.util.ControllerSkeletonFactory.normalizeControllerName;
import static com.company.appmaker.ai.util.Utils.*;

/**
 * رندرکننده‌ی پرامپت با دو API:
 * 1) render(String template, Map vars)  ← نسخه‌ی قدیمی (برای PromptController و PromptService)
 * 2) render(Project p, ControllerDef c, EndpointDef e, PromptTemplate t)  ← نسخه‌ی جدید برای AI
 */
@Component
public class PromptRenderer {

    // {{var}} یا {{var|something}}
    private static final Pattern VAR = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_\\.\\|]+)\\s*}}");

    /* =========================
       1) API قدیمی – دست نزن
       ========================= */
    public String render(String template, Map<String, ?> vars) {
        if (template == null) return "";
        Map<String, ?> safe = (vars == null) ? Map.of() : vars;

        Matcher m = VAR.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String val = resolveKeyFromMap(key, safe);
            m.appendReplacement(sb, Matcher.quoteReplacement(val));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String resolveKeyFromMap(String key, Map<String, ?> vars) {
        Object v = vars.get(key);
        return (v == null) ? "" : String.valueOf(v);
    }

    /* =========================
       2) API جدید – با کانتکست پروژه
       ========================= */
    public String render(Project p,
                         ControllerDef ctrl,
                         EndpointDef ep,
                         PromptTemplate tpl) {
        if (tpl == null) return "";
        Map<String, Object> ctx = buildCtx(p, ctrl, ep);
        return render(tpl.getBody(), ctx);  // ← از همون متد قدیمی استفاده می‌کنیم
    }

    /* =========================
       ساخت کانتکست
       ========================= */
    private Map<String, Object> buildCtx(Project p,
                                         ControllerDef ctrl,
                                         EndpointDef ep) {
        Map<String, Object> ctx = new LinkedHashMap<>();

        // --- سطح پروژه ---
        String javaVersion = (p != null && p.getJavaVersion() != null && !p.getJavaVersion().isBlank())
                ? p.getJavaVersion().trim()
                : "21";
        String basePackage = (p != null && p.getMs().getBasePackage() != null && !p.getMs().getBasePackage().isBlank())
                ? p.getMs().getBasePackage().trim()
                : "com.example.app";
        ctx.put("javaVersion", javaVersion);
        ctx.put("basePackage", basePackage);
        ctx.put("basePackage|dotToSlash", basePackage.replace('.', '/'));

        // --- سطح کنترلر ---
        String rawCtrlName = (ctrl != null && ctrl.getName() != null) ? ctrl.getName().trim() : "GeneratedController";
        String controllerName = normalizeControllerName(rawCtrlName);   // مثلا AccountController
        String feature = stripControllerSuffix(controllerName);         // مثلا Account
        String basePath = (ctrl != null && ctrl.getBasePath() != null && !ctrl.getBasePath().isBlank())
                ? ctrl.getBasePath().trim()
                : "/api";

        ctx.put("controllerName", controllerName);
        ctx.put("feature", feature);
        ctx.put("feature|UpperCamel", camel(feature));
        ctx.put("basePath", basePath);

        // --- سطح اندپوینت ---
        String endpointName = (ep != null && ep.getName() != null && !ep.getName().isBlank())
                ? ep.getName().trim()
                : "operation";
        ctx.put("endpointName", endpointName);
        ctx.put("endpointName|camelCase", camel(endpointName));

        // اینا برای پرامپت‌های repo/entity/dto بود
        ctx.put("needsExternal",true);
        ctx.put("needsDb", false);
        ctx.put("dbType", "jpa");

        // --- JSON ویژگی‌ها/VO ها از خود EndpointDef بسازیم ---
        ctx.put("propsJson", ep != null ? toPropsJson(ep) : "[]");
        ctx.put("voJson", ep != null ? toVoJson(ep) : "[]");

        return ctx;
    }

    /* =========================
       ساخت propsJson از روی EndpointDef
       ========================= */
    private String toPropsJson(EndpointDef ep) {
        // از 3 جا داده جمع می‌کنیم:
        // 1) requestFields
        // 2) responseFields
        // 3) responseParts (و فیلدهای داخلش)
        List<Map<String, Object>> items = new ArrayList<>();

        if (ep.getRequestFields() != null) {
            for (FieldDef f : ep.getRequestFields()) {
                if (f == null) continue;
                items.add(fieldToJsonLike("request." + f.getName(), f));
            }
        }

        if (ep.getResponseFields() != null) {
            for (FieldDef f : ep.getResponseFields()) {
                if (f == null) continue;
                items.add(fieldToJsonLike("response." + f.getName(), f));
            }
        }

        if (ep.getResponseParts() != null) {
            for (ResponsePartDef part : ep.getResponseParts()) {
                if (part == null) continue;
                if (part.getFields() != null) {
                    for (FieldDef f : part.getFields()) {
                        if (f == null) continue;
                        // مثلا response.items.amount
                        items.add(fieldToJsonLike("response." + part.getName() + "." + f.getName(), f));
                    }
                }
            }
        }

        // اگر چیزی نبود، یه آرایه‌ی خالی بده
        if (items.isEmpty()) return "[]";

        // دستی به JSON تبدیل می‌کنیم تا وابسته به Jackson نباشیم
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Map<String, Object> it : items) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{");
            sb.append("\"id\":\"").append(escape(String.valueOf(it.get("id")))).append("\",");
            sb.append("\"dataType\":\"").append(escape(String.valueOf(it.get("dataType")))).append("\"");
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private Map<String, Object> fieldToJsonLike(String id, FieldDef f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("dataType", f.getJavaType() == null ? "String" : f.getJavaType());
        return m;
    }

    /* =========================
       ساخت voJson
       الان چیزی تو EndpointDef نداریم،
       پس یه آرایه‌ی خالی می‌دیم که پرامپت نخوابه
       ========================= */
    private String toVoJson(EndpointDef ep) {
        return "[]";
    }

}
