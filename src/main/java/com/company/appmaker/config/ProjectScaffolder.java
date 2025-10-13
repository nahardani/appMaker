package com.company.appmaker.config;

import com.company.appmaker.model.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import com.company.appmaker.config.ScaffoldHelpers.*;
import com.company.appmaker.service.TemplateService;
import org.springframework.stereotype.Service;

import static com.company.appmaker.config.ScaffoldHelpers.toDependenciesXml;

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

        // ===== 0) Helpers & metadata
        var H         = new ScaffoldHelpers();
        String group  = H.sanitizeGroupId(p.getCompanyName());
        String art    = H.artifactId(p);
        String pkg    = group + "." + H.sanitizeIdentifier(p.getProjectName()).toLowerCase(java.util.Locale.ROOT);
        String jv     = (p.getJavaVersion() == null || p.getJavaVersion().isBlank()) ? "17" : p.getJavaVersion().trim();
        String jvNorm = normalizeJavaVer(p); // "8" | "11" | "17" | "21"

        // Microservice settings (با مقادیر پیش‌فرض امن)
        MicroserviceSettings ms = p.getMs();
        if (ms == null) {
            ms = new MicroserviceSettings();
            p.setMs(ms);
        }
        if (ms.getServiceName() == null || ms.getServiceName().isBlank())
            ms.setServiceName(H.sanitizeIdentifier(p.getProjectName()));
        if (ms.getBasePackage() == null || ms.getBasePackage().isBlank())
            ms.setBasePackage(pkg);
        if (ms.getBasePath() == null || ms.getBasePath().isBlank())
            ms.setBasePath("/api/" + ms.getServiceName().toLowerCase(java.util.Locale.ROOT));
        if (ms.getJavaVersion() == null || ms.getJavaVersion().isBlank())
            ms.setJavaVersion(jv);

        String basePkg = ms.getBasePackage();

        // ===== 1) create dirs
        Path srcMain   = root.resolve("src/main/java");
        Path resources = root.resolve("src/main/resources");
        Files.createDirectories(srcMain);
        Files.createDirectories(resources);

        Path pkgDir    = srcMain.resolve(basePkg.replace('.', '/'));
        Files.createDirectories(pkgDir);

        // App.java (Bootstrap)
        H.write(pkgDir.resolve("App.java"), H.appJava(basePkg));

        // folders (standard layers)
        Path ctrlDir = pkgDir.resolve("controller");
        Path svcDir  = pkgDir.resolve("service");
        Path repoDir = pkgDir.resolve("repository");
        Path dtoDir  = pkgDir.resolve("dto");
        Path excDir  = pkgDir.resolve("exception");
        Path cfgDir  = pkgDir.resolve("config");
        Files.createDirectories(ctrlDir);
        Files.createDirectories(svcDir);
        Files.createDirectories(repoDir);
        Files.createDirectories(dtoDir);
        Files.createDirectories(excDir);
        Files.createDirectories(cfgDir);

        // package-info for main subpackages (idempotent)
        if (!Files.exists(ctrlDir.resolve("package-info.java")))
            H.write(ctrlDir.resolve("package-info.java"), "package " + basePkg + ".controller;\n");
        if (!Files.exists(svcDir.resolve("package-info.java")))
            H.write(svcDir.resolve("package-info.java"),  "package " + basePkg + ".service;\n");
        if (!Files.exists(repoDir.resolve("package-info.java")))
            H.write(repoDir.resolve("package-info.java"), "package " + basePkg + ".repository;\n");
        if (!Files.exists(dtoDir.resolve("package-info.java")))
            H.write(dtoDir.resolve("package-info.java"),  "package " + basePkg + ".dto;\n");
        if (!Files.exists(excDir.resolve("package-info.java")))
            H.write(excDir.resolve("package-info.java"),  "package " + basePkg + ".exception;\n");
        if (!Files.exists(cfgDir.resolve("package-info.java")))
            H.write(cfgDir.resolve("package-info.java"),   "package " + basePkg + ".config;\n");

// ===== 2) POM (deps بر اساس toggles)
        List<String> coords = new ArrayList<>();
        coords.add("org.springframework.boot:spring-boot-starter-web");
        if (Boolean.TRUE.equals(ms.isEnableValidation())) coords.add("org.springframework.boot:spring-boot-starter-validation");
        if (Boolean.TRUE.equals(ms.isUseMongo()))         coords.add("org.springframework.boot:spring-boot-starter-data-mongodb");
        if (Boolean.TRUE.equals(ms.isEnableActuator()))   coords.add("org.springframework.boot:spring-boot-starter-actuator");
        if (Boolean.TRUE.equals(ms.isEnableMetrics()))    coords.add("io.micrometer:micrometer-registry-prometheus");
        if (Boolean.TRUE.equals(ms.isEnableOpenApi()))    coords.add("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0");

// همه چیز باید String باشد:
        Map<String, String> pomVars = new LinkedHashMap<>();
        pomVars.put("groupId", group);
        pomVars.put("artifactId", art);
        pomVars.put("java.version", (ms.getJavaVersion() != null ? ms.getJavaVersion() : jv));
// dependencies به صورت XML fragment
        pomVars.put("dependencies", toDependenciesXml(coords));

// اگر POM تمپلیتی داری:
        writeFromTpl(
                root.resolve("pom.xml"),
                "pom",          // section
                "pom",          // key
                jvNorm,         // variant (۸/۱۱/۱۷/۲۱)
                null,           // language
                p,              // project (اگر امضای شما این پارامتر را می‌گیرد)
                pomVars         // <-- Map<String,String>
        );

        // ===== 3) application.yml (+profiles) بر اساس toggles
        String appYml = """
        spring:
          application:
            name: %s
        """.formatted(ms.getServiceName());

        if (Boolean.TRUE.equals(ms.isUseMongo())) {
            appYml += """
          data:
            mongodb:
              uri: mongodb://localhost:27017/%s
        """.formatted(ms.getServiceName().toLowerCase(java.util.Locale.ROOT));
        }

        if (Boolean.TRUE.equals(ms.isEnableActuator())) {
            appYml += """
        management:
          endpoints:
            web:
              exposure:
                include: "*"
        """;
        }

        if (Boolean.TRUE.equals(ms.isEnableOpenApi())) {
            appYml += """
        springdoc:
          api-docs:
            enabled: true
          swagger-ui:
            path: /swagger
        """;
        }

        Files.writeString(resources.resolve("application.yml"), appYml);

        // سایر پروفایل‌ها (اگر قبلاً تمپلیت داری، همان رو نگه‌دار)
        writeFromTpl(resources.resolve("application-dev.yml"),  "profiles", "application-dev",  "any", null, p, null);
        writeFromTpl(resources.resolve("application-test.yml"), "profiles", "application-test", "any", null, p, null);
        writeFromTpl(resources.resolve("application-prod.yml"), "profiles", "application-prod", "any", null, p, null);

        // logging & i18n (مثل گذشته)
        writeFromTpl(resources.resolve("logback-spring.xml"), "logging", "logback-spring", "any", null, p, null);
        H.writeI18nFromSettings(p, resources);
        if (!Files.exists(resources.resolve("messages.properties")))
            writeFromTpl(resources.resolve("messages.properties"), "i18n", "messages", "any", null, p, null);

        // ===== 4) ثابت‌های Config و Exception
        writeFromTpl(cfgDir.resolve("OpenApiConfig.java"), "config", "OpenApiConfig", "any", null, p, Map.of(
                "basePackage", basePkg,
                "apiVersion",  ms.getApiVersion() == null ? "v1" : ms.getApiVersion(),
                "serviceName", ms.getServiceName(),
                "enableOpenApi", String.valueOf(ms.isEnableOpenApi())
        ));
        writeFromTpl(excDir.resolve("GlobalExceptionHandler.java"), "exception", "GlobalExceptionHandler", "any", null, p, null);

        if (Boolean.TRUE.equals(ms.isEnableSecurityBasic())) {
            writeFromTpl(cfgDir.resolve("SecurityConfig.java"), "config", "SecurityConfig", "any", null, p, null);
        }

        // ===== 5) (اختیاری) Dockerfile / docker-compose.yml
        if (Boolean.TRUE.equals(ms.isAddDockerfile())) {
            String docker = """
            FROM eclipse-temurin:%s-jre
            WORKDIR /app
            COPY target/*.jar app.jar
            ENTRYPOINT ["java","-jar","/app/app.jar"]
            """.formatted(ms.getJavaVersion());
            Files.writeString(root.resolve("Dockerfile"), docker);
        }
        if (Boolean.TRUE.equals(ms.isAddCompose())) {
            String compose = """
            version: "3.9"
            services:
              mongo:
                image: mongo:6
                ports: ["27017:27017"]
                volumes: ["mongo_data:/data/db"]
            volumes:
              mongo_data: {}
            """;
            Files.writeString(root.resolve("docker-compose.yml"), compose);
        }

        // ===== 6) DTO/Controller unified (از مدل پروژه—در صورت وجود)
        if (p.getControllers() != null && !p.getControllers().isEmpty()) {
            // DTO ها (سازگار با مدل قبلی)
            for (var c : p.getControllers()) {
                if (c == null || c.getEndpoints() == null) continue;
                for (var ep : c.getEndpoints()) {
                    if (ep == null) continue;
                    String methodName = (ep.getName() == null || ep.getName().isBlank()) ? "Op" : ep.getName();
                    String pascal     = H.upperCamel(methodName);

                    // Request DTO وقتی بدنه دارد
                    String http = ep.getHttpMethod() == null ? "GET" : ep.getHttpMethod().toUpperCase(java.util.Locale.ROOT);
                    boolean hasBody = http.equals("POST") || http.equals("PUT") || http.equals("PATCH");
                    if (hasBody && ep.getRequestFields() != null && !ep.getRequestFields().isEmpty()) {
                        H.write(dtoDir.resolve(pascal + "Request.java"),
                                H.genDtoJava(basePkg + ".dto", pascal + "Request", ep.getRequestFields()));
                    }

                    // Response
                    boolean hasParts = ep.getResponseParts() != null && !ep.getResponseParts().isEmpty();
                    String compositeName =
                            (ep.getResponseModelName() != null && !ep.getResponseModelName().isBlank())
                                    ? H.upperCamel(ep.getResponseModelName().trim())
                                    : pascal + "Response";
                    if (hasParts) {
                        int idx = 0;
                        for (var part : ep.getResponseParts()) {
                            if (part == null) { idx++; continue; }
                            if (!"OBJECT".equalsIgnoreCase(part.getKind())) { idx++; continue; }
                            String fieldName = (part.getName() == null || part.getName().isBlank())
                                    ? ("part" + idx) : part.getName().trim();
                            String objName   = (part.getObjectName() != null && !part.getObjectName().isBlank())
                                    ? H.upperCamel(part.getObjectName().trim())
                                    : (H.upperCamel(fieldName) + "Dto");
                            H.write(dtoDir.resolve(objName + ".java"),
                                    H.genDtoJava(basePkg + ".dto", objName, part.getFields()));
                            idx++;
                        }
                        H.write(dtoDir.resolve(compositeName + ".java"),
                                H.genCompositeDtoJava(basePkg + ".dto", compositeName, ep.getResponseParts()));
                    } else if (ep.getResponseFields() != null && !ep.getResponseFields().isEmpty()) {
                        H.write(dtoDir.resolve(compositeName + ".java"),
                                H.genDtoJava(basePkg + ".dto", compositeName, ep.getResponseFields()));
                    }
                }
            }

            // کنترلر یکپارچه (از مدل پروژه)
            H.write(ctrlDir.resolve("Controller.java"),
                    H.unifiedControllerJava(basePkg + ".controller", p.getControllers(), basePkg + ".dto"));
        }

        // ===== 7) AI generated files (overwrite, java-only, path normalization)
        if (p.getGeneratedFiles() != null) {
            for (var gf : p.getGeneratedFiles()) {
                if (gf == null || gf.getPath() == null || gf.getPath().isBlank()) continue;

                // فقط فایل‌های جاوا
                String path = gf.getPath().trim();
                if (!path.endsWith(".java")) continue;

                Path target;
                // اگر path مطلقِ سورس نبود، آن را زیر base package ببریم.
                if (path.startsWith("src/main/java/")) {
                    target = root.resolve(path);
                } else if (path.contains("/controller/")) {
                    String simple = path.substring(path.lastIndexOf('/') + 1);
                    target = ctrlDir.resolve(simple);
                } else if (path.contains("/service/")) {
                    String simple = path.substring(path.lastIndexOf('/') + 1);
                    target = svcDir.resolve(simple);
                } else if (path.contains("/repository/")) {
                    String simple = path.substring(path.lastIndexOf('/') + 1);
                    target = repoDir.resolve(simple);
                } else if (path.contains("/dto/")) {
                    String simple = path.substring(path.lastIndexOf('/') + 1);
                    target = dtoDir.resolve(simple);
                } else if (path.contains("/config/")) {
                    String simple = path.substring(path.lastIndexOf('/') + 1);
                    target = cfgDir.resolve(simple);
                } else if (path.contains("/exception/")) {
                    String simple = path.substring(path.lastIndexOf('/') + 1);
                    target = excDir.resolve(simple);
                } else {
                    // پیش‌فرض: زیر basePkg بنداز
                    String simple = path.substring(path.lastIndexOf('/') + 1);
                    target = pkgDir.resolve(simple);
                }

                Files.createDirectories(target.getParent());
                Files.writeString(target, gf.getContent()); // overwrite = OK
            }
        }

        // ===== 8) extras (نمونه فایل‌های db config قدیمی اگر لازم باشد)
        var dbDir = resources.resolve("db");
        Files.createDirectories(dbDir);
        writeFromTpl(dbDir.resolve("application-db2-jdbc.yml"), "db", "application-db2-jdbc", "any", null, p, null);
        writeFromTpl(dbDir.resolve("application-db2-jndi.yml"), "db", "application-db2-jndi", "any", null, p, null);

        // constants.properties از Project (مثل قبل)
        Files.writeString(resources.resolve("constants.properties"), constantsPropsFrom(p));
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
