package com.company.appmaker.config;

import com.company.appmaker.model.*;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;


@Service
public class ProjectScaffolder {

    /* ======================= Public API ======================= */

    /**
     * ساخت ZIP خروجی (دایرکتوری‌های خالی هم داخل ZIP لحاظ می‌شوند)
     */
    public byte[] scaffoldZip(Project p) throws IOException {
        Path tmp = Files.createTempDirectory("scaffold");
        Path root = tmp.resolve(artifactId(p));
        scaffoldToDirectory(p, root);


        try (var baos = new java.io.ByteArrayOutputStream();
             var zos = new java.util.zip.ZipOutputStream(baos)) {
            Files.walk(root).forEach(path -> {
                try {
                    String rel = root.relativize(path).toString().replace('\\', '/');
                    String entryName = artifactId(p) + "/" + rel;
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


    /**
     * ساخت پروژه روی دیسک (بدون ZIP)
     */
    public void scaffoldToDirectory(com.company.appmaker.model.Project p, java.nio.file.Path root) throws java.io.IOException {
        String groupId = sanitizeGroupId(p.getCompanyName());
        String artifact = artifactId(p);
        String pkgBase = groupId + "." + sanitizeIdentifier(p.getProjectName()).toLowerCase(java.util.Locale.ROOT);
        String javaVer = (p.getJavaVersion() != null && !p.getJavaVersion().isBlank()) ? p.getJavaVersion() : "17";

        java.nio.file.Path srcMain = root.resolve("src/main/java");
        java.nio.file.Path resources = root.resolve("src/main/resources");
        java.nio.file.Path pkgDir = srcMain.resolve(pkgBase.replace('.', '/'));
        java.nio.file.Files.createDirectories(pkgDir);
        java.nio.file.Files.createDirectories(resources);

        write(root.resolve("pom.xml"), pomXml(groupId, artifact, javaVer, p.getPackages()));
        write(pkgDir.resolve("App.java"), appJava(pkgBase));

        java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
        if (p.getPackages() != null) for (String s : p.getPackages())
            if (s != null) {
                String t = s.trim().toLowerCase(java.util.Locale.ROOT);
                if (!t.isEmpty()) unique.add(t);
            }
        for (String s : unique) {
            var dir = pkgDir.resolve(s);
            java.nio.file.Files.createDirectories(dir);
            write(dir.resolve("package-info.java"), "package " + pkgBase + "." + s + ";\n");
        }

        var ctrlDir = pkgDir.resolve("controller");
        java.nio.file.Files.createDirectories(ctrlDir);
        if (!java.nio.file.Files.exists(ctrlDir.resolve("package-info.java")))
            write(ctrlDir.resolve("package-info.java"), "package " + pkgBase + ".controller;\n");
        var dtoDir = pkgDir.resolve("dto");
        java.nio.file.Files.createDirectories(dtoDir);
        if (!java.nio.file.Files.exists(dtoDir.resolve("package-info.java")))
            write(dtoDir.resolve("package-info.java"), "package " + pkgBase + ".dto;\n");

        // DTO ها
        if (p.getControllers() != null) {
            for (var c : p.getControllers()) {
                if (c.getEndpoints() == null) continue;
                for (var ep : c.getEndpoints()) {
                    String methodName = (ep.getName() == null || ep.getName().isBlank()) ? "Op" : ep.getName();
                    String pascal = upperCamel(methodName);
                    String compositeName = (ep.getResponseModelName() != null && !ep.getResponseModelName().isBlank())
                            ? upperCamel(ep.getResponseModelName().trim()) : pascal + "Response";

                    boolean hasParts = ep.getResponseParts() != null && !ep.getResponseParts().isEmpty();

                    // Request DTO (برای بدنه)
                    String http = ep.getHttpMethod() == null ? "GET" : ep.getHttpMethod().toUpperCase(java.util.Locale.ROOT);
                    boolean hasBody = http.equals("POST") || http.equals("PUT") || http.equals("PATCH");
                    if (hasBody && ep.getRequestFields() != null && !ep.getRequestFields().isEmpty()) {
                        write(dtoDir.resolve(pascal + "Request.java"),
                                genDtoJava(pkgBase + ".dto", pascal + "Request", ep.getRequestFields()));
                    }

                    if (hasParts) {
                        // اول DTOهای جزء (Object Parts)
                        int idx = 0;
                        for (var part : ep.getResponseParts()) {
                            if (part == null) continue;
                            if (!"OBJECT".equalsIgnoreCase(part.getKind())) continue;
                            String fieldName = (part.getName() == null || part.getName().isBlank()) ? ("part" + idx) : part.getName().trim();
                            String objName = (part.getObjectName() != null && !part.getObjectName().isBlank())
                                    ? upperCamel(part.getObjectName().trim()) : (upperCamel(fieldName) + "Dto");
                            write(dtoDir.resolve(objName + ".java"),
                                    genDtoJava(pkgBase + ".dto", objName, part.getFields()));
                            idx++;
                        }
                        // سپس DTO مرکب
                        write(dtoDir.resolve(compositeName + ".java"),
                                genCompositeDtoJava(pkgBase + ".dto", compositeName, ep.getResponseParts()));
                    } else {
                        // سازگاری: پاسخ قدیمی (اسکالر یا DTO تک‌مدلی)
                        if (ep.getResponseFields() != null && !ep.getResponseFields().isEmpty()) {
                            write(dtoDir.resolve(compositeName + ".java"),
                                    genDtoJava(pkgBase + ".dto", compositeName, ep.getResponseFields()));
                        }
                    }
                }
            }
        }

        // کنترلرها
        if (p.getControllers() != null) {
            for (var c : p.getControllers()) {
                write(ctrlDir.resolve(c.getName() + ".java"), controllerJava(pkgBase + ".controller", c));
            }
        }

        // resources
        write(resources.resolve("application.yml"), baseAppYml(artifact));

        writeProfilesYamlFromSettings(p, resources);

        write(resources.resolve("messages.properties"), messagesDefault());
        write(resources.resolve("messages_en.properties"), messagesEn());
        write(resources.resolve("messages_fa.properties"), messagesFa());
        write(resources.resolve("constants.properties"), constantsProps());

        // 4) Swagger/OpenAPI (مسیر UI + OpenApiConfig + SecurityScheme)
        writeSwagger(p, root, pkgBase); // ← اینجا صدا بزن

        writeI18nFromSettings(p, resources);



    }



    /* ======================= Resources ======================= */

    private static String baseAppYml(String artifactId) {
        return ""
                + "spring:\n"
                + "  application:\n"
                + "    name: " + artifactId + "\n"
                + "  profiles:\n"
                + "    # تغییر پروفایل هنگام اجرا: SPRING_PROFILES_ACTIVE=dev|test|prod\n"
                + "    default: dev\n"
                + "  messages:\n"
                + "    basename: messages\n"
                + "    encoding: UTF-8\n"
                + "server:\n"
                + "  port: 8080\n";
    }

    private static String appDevYml() {
        return ""
                + "# تنظیمات محیط توسعه\n"
                + "logging:\n"
                + "  level:\n"
                + "    root: INFO\n"
                + "server:\n"
                + "  port: 8080\n";
    }

    private static String appTestYml() {
        return ""
                + "# تنظیمات محیط تست\n"
                + "logging:\n"
                + "  level:\n"
                + "    root: WARN\n"
                + "server:\n"
                + "  port: 0  # پورت تصادفی برای تست‌های موازی\n";
    }

    private static String appProdYml() {
        return ""
                + "# تنظیمات محیط عملیاتی\n"
                + "logging:\n"
                + "  level:\n"
                + "    root: INFO\n"
                + "server:\n"
                + "  port: 8080\n"
                + "management:\n"
                + "  endpoints:\n"
                + "    web:\n"
                + "      exposure:\n"
                + "        include: \"health,info\"\n";
    }

    private static String messagesDefault() {
        return ""
                + "app.title=Sample Service\n"
                + "app.greeting=Hello!\n"
                + "error.notfound=Resource not found.\n";
    }

    private static String messagesEn() {
        return ""
                + "app.title=Sample Service\n"
                + "app.greeting=Hello!\n"
                + "error.notfound=Resource not found.\n";
    }

    private static String messagesFa() {
        return ""
                + "app.title=سرویس نمونه\n"
                + "app.greeting=سلام!\n"
                + "error.notfound=منبع پیدا نشد.\n";
    }

    private static String constantsProps() {
        return ""
                + "# مقادیر ثابت قابل استفاده با @Value یا Environment\n"
                + "app.version=0.0.1-SNAPSHOT\n"
                + "cors.allowedOrigins=*\n"
                + "security.jwt.enabled=false\n";
    }

    /* ======================= Helpers (FS/Strings) ======================= */

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private static String sanitizeGroupId(String company) {
        if (company == null || company.isBlank()) return "com.company";
        String base = company.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        if (base.isBlank()) base = "company";
        return "com." + base;
    }

    private static String sanitizeIdentifier(String s) {
        if (s == null) return "app";
        String cleaned = s.replaceAll("[^A-Za-z0-9]+", "");
        if (cleaned.isBlank()) cleaned = "app";
        if (Character.isDigit(cleaned.charAt(0))) cleaned = "app" + cleaned;
        return cleaned;
    }

    private static String artifactId(Project p) {
        String a = (p.getProjectName() == null ? "app" :
                p.getProjectName().trim().toLowerCase(Locale.ROOT))
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("^-+|-+$", ""); // رفع dangling + در الگو
        if (a.isBlank()) a = "app";
        return a;
    }

    private static String pomXml(String groupId, String artifactId, String javaVersion, List<String> pkgs) {
        boolean includeSecurity = pkgs != null && pkgs.stream().anyMatch(s -> s.equalsIgnoreCase("security"));
        return "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n\n"
                + "  <parent>\n"
                + "    <groupId>org.springframework.boot</groupId>\n"
                + "    <artifactId>spring-boot-starter-parent</artifactId>\n"
                + "    <version>3.3.4</version>\n"
                + "    <relativePath/>\n"
                + "  </parent>\n\n"
                + "  <groupId>" + groupId + "</groupId>\n"
                + "  <artifactId>" + artifactId + "</artifactId>\n"
                + "  <version>0.0.1-SNAPSHOT</version>\n"
                + "  <name>" + artifactId + "</name>\n"
                + "  <description>Generated by AppMaker</description>\n\n"
                + "  <properties>\n"
                + "    <java.version>" + javaVersion + "</java.version>\n"
                + "  </properties>\n\n"
                + "  <dependencies>\n"
                + "    <dependency>\n"
                + "      <groupId>org.springframework.boot</groupId>\n"
                + "      <artifactId>spring-boot-starter-web</artifactId>\n"
                + "    </dependency>\n"
                + "    <dependency>\n"
                + "      <groupId>org.springframework.boot</groupId>\n"
                + "      <artifactId>spring-boot-starter-validation</artifactId>\n"
                + "    </dependency>\n"
                + (includeSecurity ?
                "    <dependency>\n"
                        + "      <groupId>org.springframework.boot</groupId>\n"
                        + "      <artifactId>spring-boot-starter-security</artifactId>\n"
                        + "    </dependency>\n" : "")
                + "    <dependency>\n"
                + "      <groupId>org.springframework.boot</groupId>\n"
                + "      <artifactId>spring-boot-starter-test</artifactId>\n"
                + "      <scope>test</scope>\n"
                + "    </dependency>\n"
                + "  </dependencies>\n\n"
                + "  <build>\n"
                + "    <plugins>\n"
                + "      <plugin>\n"
                + "        <groupId>org.springframework.boot</groupId>\n"
                + "        <artifactId>spring-boot-maven-plugin</artifactId>\n"
                + "      </plugin>\n"
                + "      <plugin>\n"
                + "        <artifactId>maven-compiler-plugin</artifactId>\n"
                + "        <configuration>\n"
                + "          <release>${java.version}</release>\n"
                + "        </configuration>\n"
                + "      </plugin>\n"
                + "    </plugins>\n"
                + "  </build>\n"
                + "</project>\n";
    }

    private static String appJava(String pkg) {
        return "package " + pkg + ";\n\n"
                + "import org.springframework.boot.SpringApplication;\n"
                + "import org.springframework.boot.autoconfigure.SpringBootApplication;\n\n"
                + "@SpringBootApplication\n"
                + "public class App {\n"
                + "  public static void main(String[] args) { SpringApplication.run(App.class, args); }\n"
                + "}\n";
    }

    /* ======================= Codegen (Controller & DTO) ======================= */

    private static String upperCamel(String s) {
        if (s == null || s.isBlank()) return "Model";
        String[] parts = s.replaceAll("[^A-Za-z0-9]+", " ").trim().split("\\s+");
        StringBuilder r = new StringBuilder();
        for (String p : parts) {
            r.append(Character.toUpperCase(p.charAt(0)))
                    .append(p.substring(1).toLowerCase(java.util.Locale.ROOT));
        }
        return r.toString();
    }


    private static String genDtoJava(String pkg, String className, java.util.List<com.company.appmaker.model.FieldDef> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n")
                .append("public class ").append(className).append(" {\n");
        if (fields != null) {
            for (var f : fields) {
                String type = (f.getJavaType() == null || f.getJavaType().isBlank()) ? "String" : f.getJavaType();
                String name = (f.getName() == null || f.getName().isBlank()) ? "field" : f.getName();
                sb.append("  private ").append(type).append(" ").append(name).append(";\n")
                        .append("  public ").append(type).append(" get").append(upperCamel(name)).append("(){return ").append(name).append(";}\n")
                        .append("  public void set").append(upperCamel(name)).append("(").append(type).append(" v){this.").append(name).append("=v;}\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }


    /**
     * تولید کلاس کنترلر بر اساس endpoints؛ در صورت نبود endpoints، از methods قدیمی استفاده می‌کند
     */
    private static String controllerJava(String pkg, com.company.appmaker.model.ControllerDef c) {
        boolean needsListImport =
                c.getEndpoints() != null && c.getEndpoints().stream().anyMatch(ep -> {
                    if (ep.getResponseParts() != null && !ep.getResponseParts().isEmpty()) {
                        // ممکن است فیلدهای داخلی لیست باشند، اما returnType Composite است (نیاز به import List فقط در بدنه نیست)
                        return false;
                    }
                    return ep.isResponseList();
                });

        String basePkg = pkg.substring(0, pkg.lastIndexOf('.'));
        String dtoPkg = basePkg + ".dto";

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n")
                .append("import org.springframework.http.ResponseEntity;\n")
                .append("import org.springframework.web.bind.annotation.*;\n");
        if (needsListImport) sb.append("import java.util.List;\nimport java.util.Collections;\n");
        sb.append("\n@RestController\n")
                .append("@RequestMapping(\"").append(c.getBasePath()).append("\")\n")
                .append("public class ").append(c.getName()).append(" {\n\n");

        if (c.getEndpoints() != null) for (var ep : c.getEndpoints()) {
            String http = ep.getHttpMethod() == null ? "GET" : ep.getHttpMethod().toUpperCase(java.util.Locale.ROOT);
            String ann = switch (http) {
                case "POST" -> "@PostMapping";
                case "PUT" -> "@PutMapping";
                case "PATCH" -> "@PatchMapping";
                case "DELETE" -> "@DeleteMapping";
                default -> "@GetMapping";
            };
            String methodName = (ep.getName() == null || ep.getName().isBlank())
                    ? "op" + http.substring(0, 1) + http.substring(1).toLowerCase(java.util.Locale.ROOT)
                    : ep.getName();
            String path = (ep.getPath() == null || ep.getPath().isBlank()) ? "" : "(\"" + ep.getPath() + "\")";

            // Return type
            String pascal = upperCamel(methodName);
            String compositeName = (ep.getResponseModelName() != null && !ep.getResponseModelName().isBlank())
                    ? upperCamel(ep.getResponseModelName().trim()) : pascal + "Response";

            boolean hasParts = ep.getResponseParts() != null && !ep.getResponseParts().isEmpty();
            String returnType;
            if (hasParts) {
                returnType = "ResponseEntity<" + compositeName + ">";
            } else {
                String responseDtoName = (ep.getResponseFields() != null && !ep.getResponseFields().isEmpty())
                        ? compositeName : null;
                String respType = (responseDtoName != null)
                        ? responseDtoName
                        : (ep.getResponseType() == null || ep.getResponseType().isBlank() ? "String" : ep.getResponseType());
                boolean list = ep.isResponseList();
                returnType = list ? "ResponseEntity<java.util.List<" + respType + ">>" : "ResponseEntity<" + respType + ">";
            }

            // Params
            StringBuilder params = new StringBuilder();
            if (ep.getParams() != null) {
                for (var pdef : ep.getParams()) {
                    if (pdef.getName() == null || pdef.getName().isBlank()) continue;
                    String name = pdef.getName().trim();
                    String type = (pdef.getJavaType() == null || pdef.getJavaType().isBlank()) ? "String" : pdef.getJavaType();
                    String loc = pdef.getIn() == null ? "QUERY" : pdef.getIn().toUpperCase(java.util.Locale.ROOT);
                    String annParam = switch (loc) {
                        case "PATH" -> "@PathVariable(\"" + name + "\")";
                        case "HEADER" -> "@RequestHeader(\"" + name + "\")";
                        default -> (pdef.isRequired() ? "@RequestParam(name=\"" + name + "\")"
                                : "@RequestParam(name=\"" + name + "\", required=false)");
                    };
                    if (params.length() > 0) params.append(", ");
                    params.append(annParam).append(" ").append(type).append(" ").append(name);
                }
            }

            boolean hasBody = http.equals("POST") || http.equals("PUT") || http.equals("PATCH");
            if (hasBody) {
                if (ep.getRequestFields() != null && !ep.getRequestFields().isEmpty()) {
                    if (params.length() > 0) params.append(", ");
                    params.append("@RequestBody ").append(pascal).append("Request body");
                } else if (ep.getRequestBodyType() != null && !ep.getRequestBodyType().isBlank()) {
                    if (params.length() > 0) params.append(", ");
                    params.append("@RequestBody ").append(ep.getRequestBodyType().trim()).append(" body");
                }
            }

            // Method body
            sb.append("  ").append(ann).append(path).append("\n")
                    .append("  public ").append(returnType).append(" ").append(methodName).append("(").append(params).append(") {\n");
            if (hasParts) {
                sb.append("    return ResponseEntity.ok(new ").append(compositeName).append("());\n");
            } else if (ep.isResponseList()) {
                sb.append("    return ResponseEntity.ok(java.util.Collections.emptyList());\n");
            } else {
                sb.append("    return ResponseEntity.ok(null);\n");
            }
            sb.append("  }\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }


    private static String genCompositeDtoJava(String pkg, String className,
                                              java.util.List<com.company.appmaker.model.ResponsePartDef> parts) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n")
                .append("import java.util.List;\n")
                .append("public class ").append(className).append(" {\n");

        if (parts != null) {
            for (var part : parts) {
                if (part == null || part.getName() == null || part.getName().isBlank()) continue;
                String fieldName = part.getName().trim();
                String type;
                if ("SCALAR".equalsIgnoreCase(part.getKind())) {
                    String scalar = (part.getScalarType() == null || part.getScalarType().isBlank()) ? "String" : part.getScalarType();
                    type = scalar;
                } else {
                    String obj = (part.getObjectName() == null || part.getObjectName().isBlank())
                            ? (upperCamel(fieldName) + "Dto") : upperCamel(part.getObjectName());
                    type = obj;
                }
                boolean list = "LIST".equalsIgnoreCase(part.getContainer());
                String fieldType = list ? "List<" + type + ">" : type;

                // فیلد
                sb.append("  private ").append(fieldType).append(" ").append(fieldName).append(";\n");
                // getter/setter
                sb.append("  public ").append(fieldType).append(" get").append(upperCamel(fieldName)).append("(){return ").append(fieldName).append(";}\n");
                sb.append("  public void set").append(upperCamel(fieldName)).append("(").append(fieldType).append(" v){this.").append(fieldName).append("=v;}\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    private void writeSwagger(Project p, Path root, String basePackage) throws IOException {
        var s = p.getSwagger();
        // اگر کاربر هنوز تنظیمات Swagger نداده، می‌تونی رد شی (یا پیش‌فرض بنویسی)
        if (s == null) {
            // پیش‌فرض‌ها (اختیاری)
            s = new SwaggerSettings();
            s.setEnabled(true);
            s.setTitle(nz(p.getProjectName(), "Project API"));
            s.setVersion("v1");
            s.setUiPath("/swagger-ui");
            p.setSwagger(s);
        }

        // 2.1) application.yml ← springdoc
        Path yml = root.resolve("src/main/resources/application.yml");
        Files.createDirectories(yml.getParent());
        String uiPath = nz(s.getUiPath(), "/swagger-ui");
        boolean enabled = s.isEnabled();

        String yaml = """
                springdoc:
                  api-docs:
                    path: /v3/api-docs
                  swagger-ui:
                    enabled: %s
                    path: %s
                    display-request-duration: true

                """.formatted(enabled, uiPath);

        appendOrCreateYaml(yml, yaml);

        // 2.2) OpenApiConfig.java
        Path cfgDir = root.resolve("src/main/java/" + basePackage.replace('.', '/') + "/config");
        Files.createDirectories(cfgDir);

        String title = esc(nz(s.getTitle(), "Project API"));
        String version = esc(nz(s.getVersion(), "v1"));

        String descLine = isBlank(s.getDescription()) ? "" : (".description(\"" + esc(s.getDescription()) + "\")");
        String contactLine = (isBlank(s.getContactName()) && isBlank(s.getContactEmail())) ? "" :
                (".contact(new Contact()" +
                        (isBlank(s.getContactName()) ? "" : ".name(\"" + esc(s.getContactName()) + "\")") +
                        (isBlank(s.getContactEmail()) ? "" : ".email(\"" + esc(s.getContactEmail()) + "\")") +
                        ")");

        String licenseLine = (isBlank(s.getLicenseName()) && isBlank(s.getLicenseUrl())) ? "" :
                (".license(new License()" +
                        (isBlank(s.getLicenseName()) ? "" : ".name(\"" + esc(s.getLicenseName()) + "\")") +
                        (isBlank(s.getLicenseUrl()) ? "" : ".url(\"" + esc(s.getLicenseUrl()) + "\")") +
                        ")");

        // Security scheme از تنظیمات امنیت پروژه (در صورت وجود)
        String securityBlock = buildOpenApiSecurityBlock(p.getSecurity());

        String java = """
                package %s.config;

                import io.swagger.v3.oas.models.OpenAPI;
                import io.swagger.v3.oas.models.info.*;
                import io.swagger.v3.oas.models.Components;
                import io.swagger.v3.oas.models.security.*;
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;

                @Configuration
                public class OpenApiConfig {

                  @Bean
                  public OpenAPI customOpenAPI() {
                    Info info = new Info()
                        .title("%s")
                        .version("%s")%s%s%s;

                    OpenAPI api = new OpenAPI().info(info);

                    %s

                    return api;
                  }
                }
                """.formatted(basePackage, title, version, descLine, contactLine, licenseLine, securityBlock);

        Files.writeString(cfgDir.resolve("OpenApiConfig.java"), java);
    }

    // === کمکی‌ها ===
    private static String nz(String v, String def) {
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    private static String esc(String v) {
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void appendOrCreateYaml(Path yml, String content) throws IOException {
        if (Files.exists(yml)) {
            Files.writeString(yml, "\n" + content, java.nio.file.StandardOpenOption.APPEND);
        } else {
            Files.createDirectories(yml.getParent());
            Files.writeString(yml, content);
        }
    }

    private String buildOpenApiSecurityBlock(SecuritySettings sec) {
        if (sec == null || sec.getAuthType() == null || sec.getAuthType() == SecuritySettings.AuthType.NONE) return "";
        switch (sec.getAuthType()) {
            case BASIC:
                return """
                        api.components(new Components().addSecuritySchemes("basicAuth",
                            new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")
                        ));
                        api.addSecurityItem(new SecurityRequirement().addList("basicAuth"));
                        """;
            case BEARER:
            case JWT:
                return """
                        api.components(new Components().addSecuritySchemes("bearerAuth",
                            new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        ));
                        api.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
                        """;
            case OAUTH2:
                // اگر URL/flow دقیق دستت نیست، حداقل اسکیمه رو ثبت کن
                return """
                        api.components(new Components().addSecuritySchemes("oauth2",
                            new SecurityScheme().type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows()
                                    .clientCredentials(new OAuthFlow()
                                        .tokenUrl("https://issuer.example/oauth/token")
                                    )
                                )
                        ));
                        api.addSecurityItem(new SecurityRequirement().addList("oauth2"));
                        """;
            default:
                return "";
        }
    }


    private void writeProfilesYamlFromSettings(Project p, Path resources) throws IOException {
        var ps = p.getProfiles();
        // fallback اگر کاربر هنوز چیزی ست نکرده:
        if (ps == null) return;

        writeEnv(resources.resolve("application-dev.yml"),  ps.getDev());
        writeEnv(resources.resolve("application-test.yml"), ps.getTest());
        writeEnv(resources.resolve("application-prod.yml"), ps.getProd());

        // default active را در application.yml بنویس
        if (ps.getDefaultActive()!=null && !ps.getDefaultActive().isBlank()) {
            Path appYml = resources.resolve("application.yml");
            appendOrCreateYaml(appYml, "spring:\n  profiles:\n    active: " + ps.getDefaultActive().trim() + "\n");
        }
    }

    private void writeEnv(Path file, ProfileSettings.EnvProfile env) throws IOException {
        if (env == null) return;
        StringBuilder yml = new StringBuilder();

        if (env.getServerPort()!=null) {
            yml.append("server:\n  port: ").append(env.getServerPort()).append("\n");
        }
        if (env.getLoggingLevelRoot()!=null && !env.getLoggingLevelRoot().isBlank()) {
            yml.append("logging:\n  level:\n    root: ").append(env.getLoggingLevelRoot().trim()).append("\n");
        }
        if (env.getIncludes()!=null && !env.getIncludes().isEmpty()) {
            yml.append("spring:\n  profiles:\n    include: ");
            yml.append(String.join(",", env.getIncludes())).append("\n");
        }
        if (env.getExtraYaml()!=null && !env.getExtraYaml().isBlank()) {
            yml.append("\n").append(env.getExtraYaml().trim()).append("\n");
        }

        if (yml.length() == 0) {
            // خالی نذار؛ یک کامنت بگذار
            yml.append("# no custom props\n");
        }
        Files.createDirectories(file.getParent());
        Files.writeString(file, yml.toString());
    }

    private void writeI18nFromSettings(Project p, java.nio.file.Path resources) throws java.io.IOException {
        var i = p.getI18n();
        if (i == null) return;

        var base = (i.getBaseName()==null || i.getBaseName().isBlank()) ? "messages" : i.getBaseName().trim();
        var locales = (i.getLocales()==null || i.getLocales().isEmpty()) ? java.util.List.of("fa","en") : i.getLocales();

        // یک فایل base هم (بدون پسوند) بنویس
        java.nio.file.Path baseFile = resources.resolve(base + ".properties");
        if (!java.nio.file.Files.exists(baseFile)) java.nio.file.Files.writeString(baseFile, "# i18n base\n");

        // هر زبان
        for (String loc : locales){
            java.nio.file.Path f = resources.resolve(base + "_" + loc + ".properties");
            StringBuilder sb = new StringBuilder();
            if (i.getKeys()!=null){
                for (var k : i.getKeys()){
                    String val = (k.getTranslations()!=null) ? k.getTranslations().get(loc) : null;
                    if (k.getCode()!=null && !k.getCode().isBlank() && val!=null){
                        sb.append(k.getCode()).append("=").append(escapeProp(val)).append("\n");
                    }
                }
            }
            if (sb.length()==0) sb.append("# ").append(loc).append("\n");
            java.nio.file.Files.writeString(f, sb.toString());
        }
    }

    private static String escapeProp(String s){
        return s.replace("\\","\\\\").replace("\n","\\n").replace("\r","").replace("=", "\\=");
    }




}
