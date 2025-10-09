package com.company.appmaker.ai;


import com.company.appmaker.ai.dto.CodeFile;
import com.company.appmaker.ai.dto.CodeMeta;
import com.company.appmaker.ai.dto.GenerateResult;
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
            Pattern.compile("(?m)^\\s*public\\s+(?:class|interface|enum)\\s+([A-Za-z0-9_]+)\\b");

    private final ObjectMapper om = new ObjectMapper();

    public GenerateResult parse(String raw) {
        CodeMeta meta = parseMeta(raw);
        List<CodeFile> files = new ArrayList<>();

        // 1) XML-like <FILE ...> blocks
        files.addAll(parseXmlFiles(raw));

        // 2) Markdown ```java filename=...```
        files.addAll(parseMarkdownNamed(raw));

        // 3) Markdown ```java``` بدون نام فایل → استنتاج مسیر
        files.addAll(parseMarkdownUnnamed(raw));

        // 4) INLINE: // FILE: path
        // (اگر همراه با کد در هر شکلی باشد، استخراج می‌کنیم)
        // این مرحله معمولاً داخل 2/3 هم پوشش داده می‌شود، اما برای ایمنی:
        if (files.isEmpty()) {
            files.addAll(parseInlineFileDirective(raw));
        }

        // 5) Fallback: اگر هنوز هیچ فایلی نداریم، همه‌ی raw را به یک md ذخیره کن
        if (files.isEmpty()) {
            String ts = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
            files.add(new CodeFile(
                    "docs/ai/GENERATED_" + ts + ".md",
                    "md",
                    raw
            ));
        }

        return new GenerateResult(meta, files, raw);
    }

    private CodeMeta parseMeta(String raw) {
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
        Matcher d = INLINE_FILE_DIRECTIVE.matcher(raw);
        int lastIdx = 0;
        while (d.find(lastIdx)) {
            String path = d.group(1).trim();
            int start = d.end();
            // محتوا تا بعدی یا پایان رشته
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
        // package com.example.demo;
        String pkg = null, type = "Generated";
        Matcher pm = PKG.matcher(content);
        if (pm.find()) {
            pkg = pm.group(1);
        }
        Matcher tm = TYPE_NAME.matcher(content);
        if (tm.find()) {
            type = tm.group(1);
        }
        String pkgPath = (pkg == null ? "" : pkg.replace('.', '/'));
        if (!pkgPath.isEmpty()) {
            return "src/main/java/" + pkgPath + "/" + type + ".java";
        }
        // اگر package نداریم، یک مسیر پیش‌فرض بده
        return "src/main/java/com/yourapp/generated/" + type + ".java";
    }

    private String guessLangFromPath(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".kt")) return "kotlin";
        if (lower.endsWith(".md")) return "md";
        if (lower.endsWith(".xml")) return "xml";
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) return "yaml";
        return "text";
    }
}
