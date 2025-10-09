package com.company.appmaker.config;

import com.company.appmaker.model.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;

import com.company.appmaker.config.ScaffoldHelpers.*;
import com.company.appmaker.service.TemplateService;
import org.springframework.stereotype.Service;

/**
 * Orchestrator: فقط APIهای سطح بالا برای اسکیفولد.
 * تمامی متدهای کمکی و تولید متن/فایل در ScaffoldHelpers قرار گرفته‌اند.
 */
@Service
public class ProjectScaffolder {

    private final ScaffoldHelpers H = new ScaffoldHelpers();

    private final TemplateService templateService;

    public ProjectScaffolder(TemplateService templateService) {
        this.templateService = templateService;
    }

    /* ======================= Public API ======================= */

    /**
     * ساخت ZIP خروجی (دایرکتوری‌های خالی هم داخل ZIP لحاظ می‌شوند)
     */
    public byte[] scaffoldZip(Project p) throws IOException {
        Path tmp = Files.createTempDirectory("scaffold");
        Path root = tmp.resolve(H.artifactId(p));
        scaffoldToDirectory(p, root);

        try (var baos = new java.io.ByteArrayOutputStream();
             var zos  = new java.util.zip.ZipOutputStream(baos)) {

            Files.walk(root).forEach(path -> {
                try {
                    String rel = root.relativize(path).toString().replace('\\', '/');
                    String entryName = H.artifactId(p) + "/" + rel;

                    if (Files.isDirectory(path)) {
                        if (!rel.isEmpty()) {
                            if (!entryName.endsWith("/")) entryName += "/";
                            zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                            zos.closeEntry();
                        }
                    } else {
                        zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    }
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });

            zos.finish();
            return baos.toByteArray();
        }
    }

    public void scaffoldToDirectory(Project p, Path root) throws IOException {
        // === متادیتا و مسیرها
        var h        = new ScaffoldHelpers();
        String group = h.sanitizeGroupId(p.getCompanyName());
        String art   = h.artifactId(p);
        String pkg   = group + "." + h.sanitizeIdentifier(p.getProjectName()).toLowerCase(java.util.Locale.ROOT);
        String jv    = (p.getJavaVersion() == null || p.getJavaVersion().isBlank()) ? "17" : p.getJavaVersion().trim();
        String jvNorm= normalizeJavaVer(p); // 8/11/17/21

        java.nio.file.Path srcMain   = root.resolve("src/main/java");
        java.nio.file.Path resources = root.resolve("src/main/resources");
        Files.createDirectories(srcMain);
        Files.createDirectories(resources);

        // === ساخت ساختار پکیج‌ها
        java.nio.file.Path pkgDir = srcMain.resolve(pkg.replace('.', '/'));
        Files.createDirectories(pkgDir);

        // App.java (می‌توانی بعداً این را هم tpl کنی)
        h.write(pkgDir.resolve("App.java"), h.appJava(pkg));

        // پکیج‌های انتخابی + package-info
        var unique = new java.util.LinkedHashSet<String>();
        if (p.getPackages() != null) {
            for (String s : p.getPackages()) {
                if (s == null) continue;
                String t = s.trim().toLowerCase(java.util.Locale.ROOT);
                if (!t.isEmpty()) unique.add(t);
            }
        }
        for (String s : unique) {
            var dir = pkgDir.resolve(s);
            Files.createDirectories(dir);
            h.write(dir.resolve("package-info.java"), "package " + pkg + "." + s + ";\n");
        }

        // === لایه‌های استاندارد (اگر تمپلیت قبلاً ساخته، فقط ensure می‌کنیم)
        var controllerDir = pkgDir.resolve("controller");
        var serviceDir    = pkgDir.resolve("service");
        var dtoDir        = pkgDir.resolve("dto");
        var repoDir       = pkgDir.resolve("repository");
        var entityDir     = pkgDir.resolve("entity"); // اگر در تمپلیت شما domain است: "domain"

        Files.createDirectories(controllerDir);
        Files.createDirectories(serviceDir);
        Files.createDirectories(dtoDir);
        Files.createDirectories(repoDir);
        Files.createDirectories(entityDir);

        if (!Files.exists(controllerDir.resolve("package-info.java")))
            h.write(controllerDir.resolve("package-info.java"), "package " + pkg + ".controller;\n");
        if (!Files.exists(serviceDir.resolve("package-info.java")))
            h.write(serviceDir.resolve("package-info.java"), "package " + pkg + ".service;\n");
        if (!Files.exists(dtoDir.resolve("package-info.java")))
            h.write(dtoDir.resolve("package-info.java"), "package " + pkg + ".dto;\n");
        if (!Files.exists(repoDir.resolve("package-info.java")))
            h.write(repoDir.resolve("package-info.java"), "package " + pkg + ".repository;\n");
        if (!Files.exists(entityDir.resolve("package-info.java")))
            h.write(entityDir.resolve("package-info.java"), "package " + pkg + ".entity;\n"); // یا ".domain"

        // === DTOها بر اساس اندپوینت‌ها
        if (p.getControllers() != null) {
            for (var c : p.getControllers()) {
                if (c == null || c.getEndpoints() == null) continue;
                for (var ep : c.getEndpoints()) {
                    if (ep == null) continue;

                    String methodName = (ep.getName() == null || ep.getName().isBlank()) ? "Op" : ep.getName();
                    String pascal     = h.upperCamel(methodName);

                    // مدل خروجی مرکب (در حالت چندبخشی)
                    String compositeName =
                            (ep.getResponseModelName() != null && !ep.getResponseModelName().isBlank())
                                    ? h.upperCamel(ep.getResponseModelName().trim())
                                    : pascal + "Response";

                    // Request DTO وقتی بدنه دارد
                    String http = ep.getHttpMethod() == null ? "GET" : ep.getHttpMethod().toUpperCase(java.util.Locale.ROOT);
                    boolean hasBody = http.equals("POST") || http.equals("PUT") || http.equals("PATCH");
                    if (hasBody && ep.getRequestFields() != null && !ep.getRequestFields().isEmpty()) {
                        h.write(dtoDir.resolve(pascal + "Request.java"),
                                h.genDtoJava(pkg + ".dto", pascal + "Request", ep.getRequestFields()));
                    }

                    // Response: چندبخشی یا ساده
                    boolean hasParts = ep.getResponseParts() != null && !ep.getResponseParts().isEmpty();
                    if (hasParts) {
                        int idx = 0;
                        for (var part : ep.getResponseParts()) {
                            if (part == null) { idx++; continue; }
                            if (!"OBJECT".equalsIgnoreCase(part.getKind())) { idx++; continue; }

                            String fieldName = (part.getName() == null || part.getName().isBlank())
                                    ? ("part" + idx) : part.getName().trim();
                            String objName   = (part.getObjectName() != null && !part.getObjectName().isBlank())
                                    ? h.upperCamel(part.getObjectName().trim())
                                    : (h.upperCamel(fieldName) + "Dto");

                            h.write(dtoDir.resolve(objName + ".java"),
                                    h.genDtoJava(pkg + ".dto", objName, part.getFields()));
                            idx++;
                        }
                        // مدل مرکب
                        h.write(dtoDir.resolve(compositeName + ".java"),
                                h.genCompositeDtoJava(pkg + ".dto", compositeName, ep.getResponseParts()));
                    } else {
                        // سازگاری حالت قدیمی
                        if (ep.getResponseFields() != null && !ep.getResponseFields().isEmpty()) {
                            h.write(dtoDir.resolve(compositeName + ".java"),
                                    h.genDtoJava(pkg + ".dto", compositeName, ep.getResponseFields()));
                        }
                    }
                }
            }
        }

        // === کنترلر واحد (از مدل پروژه)
        if (p.getControllers() != null && !p.getControllers().isEmpty()) {
            h.write(controllerDir.resolve("Controller.java"),
                    h.unifiedControllerJava(pkg + ".controller", p.getControllers(), pkg + ".dto"));
        }

        // === فایل‌های پیکربندی/منابع از tpl
        // pom.xml
        writeFromTpl(root.resolve("pom.xml"), "pom", "pom", jvNorm, null, p, java.util.Map.of(
                "groupId", group,
                "artifactId", art,
                "java.version", jv
        ));

        // application*.yml
        writeFromTpl(resources.resolve("application.yml"),      "profiles", "application.base", "any", null, p, null);
        writeFromTpl(resources.resolve("application-dev.yml"),  "profiles", "application-dev",  "any", null, p, null);
        writeFromTpl(resources.resolve("application-test.yml"), "profiles", "application-test", "any", null, p, null);
        writeFromTpl(resources.resolve("application-prod.yml"), "profiles", "application-prod", "any", null, p, null);

        // logback-spring.xml
        writeFromTpl(resources.resolve("logback-spring.xml"), "logging", "logback-spring", "any", null, p, null);

        // i18n از Settings (کلیدها/زبان‌ها) + فایل‌های پیش‌فرض اگر تهی بود
        H.writeI18nFromSettings(p, resources);
        if (!Files.exists(resources.resolve("messages.properties")))
            writeFromTpl(resources.resolve("messages.properties"), "i18n", "messages", "any", null, p, null);
        if (!Files.exists(resources.resolve("messages_fa.properties")))
            writeFromTpl(resources.resolve("messages_fa.properties"), "i18n", "messages_fa", "any", null, p, null);
        if (!Files.exists(resources.resolve("messages_en.properties")))
            writeFromTpl(resources.resolve("messages_en.properties"), "i18n", "messages_en", "any", null, p, null);

        // constants.properties (از Project)
        Files.writeString(resources.resolve("constants.properties"), constantsPropsFrom(p));

        // === کلاس‌های config/exception از tpl
        var cfgDir = pkgDir.resolve("config");
        Files.createDirectories(cfgDir);

        writeFromTpl(cfgDir.resolve("OpenApiConfig.java"), "config", "OpenApiConfig", "any", null, p, null);
        writeFromTpl(cfgDir.resolve("WebConfig.java"),     "config", "WebConfig",     jvNorm, null, p, null);
        writeFromTpl(cfgDir.resolve("SecurityConfig.java"),"config", "SecurityConfig","any",  null, p, null);

        var excDir = pkgDir.resolve("exception");
        Files.createDirectories(excDir);
        writeFromTpl(excDir.resolve("GlobalExceptionHandler.java"), "exception", "GlobalExceptionHandler", "any", null, p, null);

        // (اختیاری) نمونه تنظیمات DB2
        var dbDir = resources.resolve("db");
        Files.createDirectories(dbDir);
        writeFromTpl(dbDir.resolve("application-db2-jdbc.yml"), "db", "application-db2-jdbc", "any", null, p, null);
        writeFromTpl(dbDir.resolve("application-db2-jndi.yml"), "db", "application-db2-jndi", "any", null, p, null);


        // --- AI generated files (MERGE with template structure; OVERWRITE on conflict; Java-only)
        if (p.getGeneratedFiles() != null && !p.getGeneratedFiles().isEmpty()) {
            for (var gf : p.getGeneratedFiles()) {
                if (gf == null) continue;

                // فقط فایل جاوا می‌خواهیم
                String pathStr = gf.getPath();
                if (pathStr == null || !pathStr.toLowerCase(java.util.Locale.ROOT).endsWith(".java")) {
                    continue; // non-java: skip
                }

                String original = (gf.getContent() == null) ? "" : gf.getContent();

                // ایمنی مسیر ورودی (ما فقط از نام فایلش استفاده می‌کنیم)
                Path suggested = root.resolve(pathStr).normalize();
                if (!suggested.startsWith(root)) {
                    System.err.println("⚠️  Skip AI file (path escapes root): " + pathStr);
                    continue;
                }

                // استخراج نام نوع
                String typeName = extractPublicTypeName(original);
                if (typeName == null || typeName.isBlank()) {
                    String file = suggested.getFileName().toString();
                    typeName = file.endsWith(".java") ? file.substring(0, file.length() - 5) : "Generated";
                }

                // تشخیص نقش فایل و تعیین پکیج مقصد
                Role role = detectRole(original, typeName);
                String targetPackage = switch (role) {
                    case CONTROLLER -> pkg + ".controller";
                    case SERVICE, SERVICE_IMPL -> pkg + ".service";
                    case DTO -> pkg + ".dto";
                    case REPOSITORY -> pkg + ".repository";
                    case ENTITY -> pkg + ".entity"; // اگر domain دارید، به ".domain" تغییر دهید
                    default -> pkg; // fallback
                };

                // بازنویسی package + ساخت مسیر مقصد از روی package
                String rewritten = rewritePackage(original, targetPackage);
                Path targetDir = srcMain.resolve(targetPackage.replace('.', '/'));
                Files.createDirectories(targetDir);
                Path dest = targetDir.resolve(typeName + ".java").normalize();

                if (!dest.startsWith(root)) {
                    System.err.println("⚠️  Skip AI file (normalized path escapes root): " + dest);
                    continue;
                }

                // سیاست تعارض: OVERWRITE
                Files.createDirectories(dest.getParent());
                Files.writeString(dest, rewritten);
                System.out.println("✅ AI merged: " + root.relativize(dest));
            }
        }
    }



    private static String extractPublicTypeName(String content) {
        var m = java.util.regex.Pattern
                .compile("(?m)^\\s*public\\s+(class|interface|enum)\\s+([A-Za-z0-9_]+)")
                .matcher(content);
        return m.find() ? m.group(2) : null;
    }

    private enum Role { CONTROLLER, SERVICE, SERVICE_IMPL, DTO, REPOSITORY, ENTITY, UNKNOWN }

    private static Role detectRole(String content, String typeName) {
        String lc = content.toLowerCase(java.util.Locale.ROOT);
        if (lc.contains("@restcontroller") || lc.contains("@controller")) return Role.CONTROLLER;
        if (lc.contains("@service")) return Role.SERVICE;
        if (lc.contains("@repository")) return Role.REPOSITORY;
        if (lc.contains("@entity") || lc.contains("@table")) return Role.ENTITY;
        String tn = typeName.toLowerCase(java.util.Locale.ROOT);
        if (tn.endsWith("controller")) return Role.CONTROLLER;
        if (tn.endsWith("serviceimpl")) return Role.SERVICE_IMPL;
        if (tn.endsWith("service")) return Role.SERVICE;
        if (tn.endsWith("repository")) return Role.REPOSITORY;
        if (tn.endsWith("dto") || tn.endsWith("request") || tn.endsWith("response")) return Role.DTO;
        return Role.UNKNOWN;
    }

    private static String rewritePackage(String content, String targetPkg) {
        var pkgMatcher = java.util.regex.Pattern
                .compile("(?m)^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;\\s*")
                .matcher(content);
        if (pkgMatcher.find()) {
            return pkgMatcher.replaceFirst("package " + targetPkg + ";");
        } else {
            return "package " + targetPkg + ";\n\n" + content;
        }
    }





    private String fill(String tpl, Project p, Map<String,String> extra) {
        Map<String,String> m = new LinkedHashMap<>();
        ScaffoldHelpers scaffoldHelpers = new ScaffoldHelpers();
        m.put("groupId", scaffoldHelpers.sanitizeGroupId(p.getCompanyName()));
        m.put("artifactId", scaffoldHelpers.artifactId(p));
        m.put("pkgBase", m.get("groupId")+"."+scaffoldHelpers.sanitizeIdentifier(p.getProjectName()).toLowerCase(Locale.ROOT));
        m.put("appName", p.getProjectName());
        m.put("javaVersion", p.getJavaVersion()==null?"17":p.getJavaVersion());
        m.put("defaultLocale", p.getI18n()!=null && p.getI18n().getDefaultLocale()!=null ? p.getI18n().getDefaultLocale() : "fa");
        if (extra!=null) m.putAll(extra);

        String out = tpl;
        for (var e : m.entrySet()) {
            out = out.replace("{{"+e.getKey()+"}}", e.getValue());
        }
        // مقدار پیش‌فرض برای placeholderهای دارای default: {{key:default}}
        out = out.replaceAll("\\{\\{([a-zA-Z0-9_]+):([^}]+)}}", "$2");
        return out;
    }

    /** دریافت tpl از TemplateService، پر کردن placeholderها با fill و نوشتن روی دیسک */
    private void writeFromTpl(Path target,
                              String section,
                              String key,
                              String javaVer,          // مثلاً "17" یا "any"
                              String languageOrNull,   // معمولاً null
                              Project p,
                              Map<String,String> extra) throws IOException {

        String tpl = templateService.getSnippet(section, key, javaVer, languageOrNull);
        if (tpl == null || tpl.isBlank()) {
            // اگر چیزی در DB/classpath پیدا نشد، یک فایل خالی نگذاریم
            tpl = "";
        }
        String content = fill(tpl, p, (extra == null ? java.util.Map.of() : extra));
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
    }

    /** نرمال‌سازی ورژن جاوا به یکی از 8/11/17/21 برای انتخاب tpl صحیح */
//    private String normalizeJavaVer(Project p) {
//        String v = (p.getJavaVersion()==null || p.getJavaVersion().isBlank()) ? "17" : p.getJavaVersion().trim();
//        if (v.startsWith("8")) return "8";
//        if (v.startsWith("11")) return "11";
//        if (v.startsWith("17")) return "17";
//        if (v.startsWith("21")) return "21";
//        return "17";
//    }


//    private String constantsPropsFrom(Project p) {
//        var cs = p.getConstants();
//        if (cs == null || cs.getEntries() == null || cs.getEntries().isEmpty()) {
//            return "# constants\n";
//        }
//        StringBuilder sb = new StringBuilder();
//        cs.getEntries().forEach((k, v) -> {
//            sb.append(k).append("=").append(new ScaffoldHelpers().escapeProp(v)).append("\n");
//        });
//        return sb.toString();
//    }
    /* ==================== helpers (می‌توانند private در همین کلاس باشند) ==================== */

    private static String normalizeJavaVer(Project p) {
        String v = (p.getJavaVersion() == null ? "" : p.getJavaVersion().trim());
        if (v.startsWith("1.")) v = v.substring(2);
        return switch (v) {
            case "8","11","17","21" -> v;
            default -> "17";
        };
    }


    private static String constantsPropsFrom(Project p) {
        // همان منطق فعلی خودت را نگه دار؛ این یک placeholder ساده است
        StringBuilder sb = new StringBuilder();
        sb.append("project.name=").append(p.getProjectName()).append('\n');
        sb.append("company.name=").append(p.getCompanyName()).append('\n');
        return sb.toString();
    }


}
