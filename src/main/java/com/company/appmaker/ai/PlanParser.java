package com.company.appmaker.ai;

import com.company.appmaker.ai.dto.CodeFile;
import com.company.appmaker.ai.dto.CodeMeta;
import com.company.appmaker.ai.dto.GenerateResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PlanParser {

    private static final Pattern META =
            Pattern.compile("<META>\\s*(\\{[\\s\\S]*?})\\s*</META>", Pattern.CASE_INSENSITIVE);

    private static final Pattern FILE_XML =
            Pattern.compile("<FILE\\s+path=\"([^\"]+)\"\\s+lang=\"([^\"]+)\"\\s*>\\s*([\\s\\S]*?)\\s*</FILE>",
                    Pattern.CASE_INSENSITIVE);

    // ```java filename=...``` یا ```java path=...```
    private static final Pattern MD_FENCE_WITH_NAME =
            Pattern.compile("```\\s*java\\s+(?:filename|file|path)\\s*=\\s*([^\\s\\n]+)\\s*\\n([\\s\\S]*?)```",
                    Pattern.CASE_INSENSITIVE);

    // فقط ```java ... ```
    private static final Pattern MD_FENCE_JAVA =
            Pattern.compile("```\\s*java\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    // داخل کد: // FILE: some/path.java
    private static final Pattern INLINE_FILE_DIRECTIVE =
            Pattern.compile("(?m)^\\s*//\\s*FILE:\\s*(\\S+)\\s*$");

    // برای استنتاج مسیر از package و اسم کلاس
    private static final Pattern PKG =
            Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;\\s*$");
    private static final Pattern TYPE_NAME =
            Pattern.compile("(?m)^\\s*public\\s+(?:class|interface|enum|record)\\s+([A-Za-z0-9_]+)\\b");

    private final ObjectMapper om = new ObjectMapper();

    public GenerateResult parse(String raw) {
        // 0) حالت JSON با اسکیمای files/meta → مستقیم نگاشت کن
        if (looksLikeJson(raw)) {
            GenerateResult fromJson = tryParseJsonPlan(raw);
            if (fromJson != null && fromJson.files() != null && !fromJson.files().isEmpty()) {
                return fromJson;
            }
            // اگر JSON بود ولی یا parse نشد یا files خالی بود، می‌ریم سراغ منطق‌های بعدی
        }

        // 1) متا از تگ META (اگر نبود defaults)
        CodeMeta meta = parseMeta(raw);
        List<CodeFile> files = new ArrayList<>();

        // 2) XML-like <FILE ...> blocks
        files.addAll(parseXmlFiles(raw));

        // 3) Markdown ```java filename=...```
        files.addAll(parseMarkdownNamed(raw));

        // 4) Markdown ```java``` بدون نام فایل → استنتاج مسیر
        files.addAll(parseMarkdownUnnamed(raw));

        // 5) INLINE: // FILE: path
        if (files.isEmpty()) {
            files.addAll(parseInlineFileDirective(raw));
        }

        // 6) Fallback: اگر هنوز هیچ فایلی نداریم، کل raw را به یک md ذخیره کنیم
        if (files.isEmpty()) {
            String ts = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
            files.add(new CodeFile(
                    "docs/ai/GENERATED_" + ts + ".md",
                    "md",
                    raw == null ? "" : raw
            ));
        }

        return new GenerateResult(meta, files, raw);
    }

    // ---------------- JSON plan support ----------------

    private boolean looksLikeJson(String raw) {
        if (raw == null) return false;
        String t = raw.trim();
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"));
    }

    private GenerateResult tryParseJsonPlan(String raw) {
        try {
            JsonPlan plan = om.readValue(raw, JsonPlan.class);

            // files
            List<CodeFile> files = new ArrayList<>();
            if (plan.files != null) {
                for (JsonFile jf : plan.files) {
                    if (jf == null || jf.path == null || jf.path.isBlank()) continue;
                    String lang = (jf.language == null || jf.language.isBlank()) ? "java" : jf.language;
                    files.add(new CodeFile(jf.path, lang, jf.content == null ? "" : jf.content));
                }
            }

            // meta
            String language = "java";
            String framework = "spring-boot";
            int javaVersion = 17;
            String module = "unknown";
            if (plan.meta != null) {
                if (plan.meta.language != null && !plan.meta.language.isBlank()) {
                    language = plan.meta.language;
                }
                // می‌توانی در صورت حضور فیلدهای دیگر آن‌ها را هم بخوانی
            }

            // اگر فایل‌ها خالی است، اجازه بده caller به مسیرهای بعدی برود
            if (files.isEmpty()) return null;

            return new GenerateResult(
                    new CodeMeta(language, framework, javaVersion, module),
                    files,
                    raw
            );
        } catch (Exception e) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class JsonPlan {
        public List<JsonFile> files;
        public JsonMeta meta;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class JsonFile {
        public String path;
        public String language;
        public String content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class JsonMeta {
        public String language;
    }

    // ---------------- existing parsers ----------------

    private CodeMeta parseMeta(String raw) {
        if (raw == null) return new CodeMeta("java", "spring-boot", 17, "unknown");
        Matcher m = META.matcher(raw);
        if (!m.find()) {
            return new CodeMeta("java", "spring-boot", 17, "unknown");
        }
        try {
            var node = om.readTree(m.group(1));
            String language = node.has("language") ? node.get("language").asText("java") : "java";
            String framework = node.has("framework") ? node.get("framework").asText("spring-boot") : "spring-boot";
            int javaVersion = node.has("javaVersion") ? node.get("javaVersion").asInt(17) : 17;
            String module = node.has("module") ? node.get("module").asText("unknown") : "unknown";
            return new CodeMeta(language, framework, javaVersion, module);
        } catch (Exception e) {
            return new CodeMeta("java", "spring-boot", 17, "unknown");
        }
    }

    private List<CodeFile> parseXmlFiles(String raw) {
        List<CodeFile> files = new ArrayList<>();
        if (raw == null) return files;
        Matcher f = FILE_XML.matcher(raw);
        while (f.find()) {
            String path = f.group(1).trim();
            String lang = f.group(2).trim();
            String content = f.group(3);
            files.add(new CodeFile(path, lang, content));
        }
        return files;
    }

    private List<CodeFile> parseMarkdownNamed(String raw) {
        List<CodeFile> files = new ArrayList<>();
        if (raw == null) return files;
        Matcher m = MD_FENCE_WITH_NAME.matcher(raw);
        while (m.find()) {
            String path = m.group(1).trim();
            String content = m.group(2);
            String lang = guessLangFromPath(path);
            files.add(new CodeFile(path, lang, content));
        }
        return files;
    }

    private List<CodeFile> parseMarkdownUnnamed(String raw) {
        List<CodeFile> files = new ArrayList<>();
        if (raw == null) return files;
        Matcher m = MD_FENCE_JAVA.matcher(raw);
        while (m.find()) {
            String content = m.group(1);
            String path = inferJavaPath(content);
            files.add(new CodeFile(path, "java", content));
        }
        return files;
    }

    private List<CodeFile> parseInlineFileDirective(String raw) {
        List<CodeFile> files = new ArrayList<>();
        if (raw == null) return files;
        Matcher d = INLINE_FILE_DIRECTIVE.matcher(raw);
        int lastIdx = 0;
        while (d.find(lastIdx)) {
            String path = d.group(1).trim();
            int start = d.end();
            int next = nextIndexOf(INLINE_FILE_DIRECTIVE, raw, start);
            String content = raw.substring(start, next < 0 ? raw.length() : next);
            files.add(new CodeFile(path, guessLangFromPath(path), content));
            lastIdx = (next < 0 ? raw.length() : next);
        }
        return files;
    }

    private int nextIndexOf(Pattern p, String s, int from) {
        Matcher m = p.matcher(s);
        if (m.find(from)) return m.start();
        return -1;
    }

    private String inferJavaPath(String content) {
        String pkg = null, type = "Generated";
        Matcher pm = PKG.matcher(content == null ? "" : content);
        if (pm.find()) {
            pkg = pm.group(1);
        }
        Matcher tm = TYPE_NAME.matcher(content == null ? "" : content);
        if (tm.find()) {
            type = tm.group(1);
        }
        String pkgPath = (pkg == null ? "" : pkg.replace('.', '/'));
        if (!pkgPath.isEmpty()) {
            return "src/main/java/" + pkgPath + "/" + type + ".java";
        }
        return "src/main/java/com/yourapp/generated/" + type + ".java";
    }

    private String guessLangFromPath(String path) {
        String lower = (path == null ? "" : path.toLowerCase(Locale.ROOT));
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".kt")) return "kotlin";
        if (lower.endsWith(".md")) return "md";
        if (lower.endsWith(".xml")) return "xml";
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) return "yaml";
        return "text";
    }

    public static String cleanMarkdown(String raw) {
        if (raw == null) return "";
        return raw
                .replaceAll("(?s)```java", "")
                .replaceAll("(?s)```", "")
                .trim();
    }
}
