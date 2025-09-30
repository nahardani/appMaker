package com.company.appmaker.config;

import com.company.appmaker.model.*;
import com.company.appmaker.model.coctroller.ControllerDef;
import com.company.appmaker.model.coctroller.EndpointDef;
import com.company.appmaker.model.profile.ProfileSettings;
import com.company.appmaker.model.security.SecuritySettings;
import com.company.appmaker.model.swagger.SwaggerSettings;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;


@Service
public class ProjectScaffolder {

    public byte[] scaffoldZip(Project p) throws IOException {
        Path tmpRoot = Files.createTempDirectory("scaffold_");
        Path projectRoot = tmpRoot.resolve(artifactId(p)); // مثلا test1

        try {
            // پروژه را روی دیسک بساز
            scaffoldToDirectory(p, projectRoot);

            // زیپ کن
            try (var baos = new java.io.ByteArrayOutputStream();
                 var zos  = new java.util.zip.ZipOutputStream(baos)) {

                // محتویات projectRoot را با پیشوند فولدر پروژه داخل زیپ قرار بده
                zipDirectory(projectRoot, artifactId(p) + "/", zos);
                zos.finish();
                return baos.toByteArray();
            }
        } finally {
            // پاکسازی پوشه موقت (best-effort)
            try { deleteRecursively(tmpRoot); } catch (Exception ignored) {}
        }
    }

    private void zipDirectory(Path source, String zipPrefix, java.util.zip.ZipOutputStream zos) throws IOException {
        Files.walkFileTree(source, new java.nio.file.SimpleFileVisitor<>() {
            @Override
            public java.nio.file.FileVisitResult preVisitDirectory(Path dir, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                // برای هر دایرکتوری یک entry با / پایانی بنویس (به‌جز خود ریشه)
                String rel = source.relativize(dir).toString().replace('\\', '/');
                String name = zipPrefix + (rel.isEmpty() ? "" : rel + "/");
                if (!name.isEmpty()) {
                    zos.putNextEntry(new java.util.zip.ZipEntry(name));
                    zos.closeEntry();
                }
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                String rel = source.relativize(file).toString().replace('\\', '/');
                String name = zipPrefix + rel;
                zos.putNextEntry(new java.util.zip.ZipEntry(name));
                Files.copy(file, zos);
                zos.closeEntry();
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult visitFileFailed(Path file, IOException exc) {
                // اگر فایلی قابل خواندن نبود، صرفاً ازش عبور کن تا زیپ بسازد
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        Files.walkFileTree(root, new java.nio.file.SimpleFileVisitor<>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }

    public void scaffoldToDirectory(com.company.appmaker.model.Project p, java.nio.file.Path root) throws java.io.IOException {
        // --- متادیتا و مسیرها
        String groupId = sanitizeGroupId(p.getCompanyName());
        String artifact = artifactId(p);
        String pkgBase  = groupId + "." + sanitizeIdentifier(p.getProjectName()).toLowerCase(java.util.Locale.ROOT);
        String javaVer  = (p.getJavaVersion() != null && !p.getJavaVersion().isBlank()) ? p.getJavaVersion() : "17";

        java.nio.file.Path srcMain   = root.resolve("src/main/java");
        java.nio.file.Path resources = root.resolve("src/main/resources");
        java.nio.file.Files.createDirectories(srcMain);
        java.nio.file.Files.createDirectories(resources);

        // --- 1) ابتدا pom.xml را بساز تا بتوانیم بعداً آن را به‌روزرسانی کنیم
        java.nio.file.Path pomPath = root.resolve("pom.xml");
        write(pomPath, pomXml(groupId, artifact, javaVer, p.getPackages()));

        // --- 2) ساخت ساختار پکیج‌ها
        java.nio.file.Path pkgDir = srcMain.resolve(pkgBase.replace('.', '/'));
        java.nio.file.Files.createDirectories(pkgDir);
        write(pkgDir.resolve("App.java"), appJava(pkgBase));

        // پکیج‌های انتخابی (با package-info.java)
        java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
        if (p.getPackages() != null) {
            for (String s : p.getPackages()) {
                if (s == null) continue;
                String t = s.trim().toLowerCase(java.util.Locale.ROOT);
                if (!t.isEmpty()) unique.add(t);
            }
        }
        for (String s : unique) {
            var dir = pkgDir.resolve(s);
            java.nio.file.Files.createDirectories(dir);
            write(dir.resolve("package-info.java"), "package " + pkgBase + "." + s + ";\n");
        }

        // controller و dto حداقل ساخته شوند
        var ctrlDir = pkgDir.resolve("controller");
        java.nio.file.Files.createDirectories(ctrlDir);
        if (!java.nio.file.Files.exists(ctrlDir.resolve("package-info.java")))
            write(ctrlDir.resolve("package-info.java"), "package " + pkgBase + ".controller;\n");

        var dtoDir = pkgDir.resolve("dto");
        java.nio.file.Files.createDirectories(dtoDir);
        if (!java.nio.file.Files.exists(dtoDir.resolve("package-info.java")))
            write(dtoDir.resolve("package-info.java"), "package " + pkgBase + ".dto;\n");

        // --- 3) DTO ها بر اساس اندپوینت‌ها
        if (p.getControllers() != null) {
            for (var c : p.getControllers()) {
                if (c == null || c.getEndpoints() == null) continue;

                for (var ep : c.getEndpoints()) {
                    if (ep == null) continue;

                    String methodName = (ep.getName() == null || ep.getName().isBlank()) ? "Op" : ep.getName();
                    String pascal     = upperCamel(methodName);

                    // نام مدل مرکب خروجی (در صورت چند بخشی)
                    String compositeName = (ep.getResponseModelName() != null && !ep.getResponseModelName().isBlank())
                            ? upperCamel(ep.getResponseModelName().trim())
                            : pascal + "Response";

                    // Request DTO (وقتی بدنه دارد)
                    String http = ep.getHttpMethod() == null ? "GET" : ep.getHttpMethod().toUpperCase(java.util.Locale.ROOT);
                    boolean hasBody = http.equals("POST") || http.equals("PUT") || http.equals("PATCH");

                    if (hasBody && ep.getRequestFields() != null && !ep.getRequestFields().isEmpty()) {
                        write(dtoDir.resolve(pascal + "Request.java"),
                                genDtoJava(pkgBase + ".dto", pascal + "Request", ep.getRequestFields()));
                    }

                    // Response (چند بخشی یا ساده)
                    boolean hasParts = ep.getResponseParts() != null && !ep.getResponseParts().isEmpty();
                    if (hasParts) {
                        // ابتدا DTOهای آبجکتِ هر part
                        int idx = 0;
                        for (var part : ep.getResponseParts()) {
                            if (part == null) continue;
                            if (!"OBJECT".equalsIgnoreCase(part.getKind())) { idx++; continue; }

                            String fieldName = (part.getName() == null || part.getName().isBlank())
                                    ? ("part" + idx) : part.getName().trim();
                            String objName   = (part.getObjectName() != null && !part.getObjectName().isBlank())
                                    ? upperCamel(part.getObjectName().trim())
                                    : (upperCamel(fieldName) + "Dto");

                            write(dtoDir.resolve(objName + ".java"),
                                    genDtoJava(pkgBase + ".dto", objName, part.getFields()));
                            idx++;
                        }
                        // سپس مدل مرکب
                        write(dtoDir.resolve(compositeName + ".java"),
                                genCompositeDtoJava(pkgBase + ".dto", compositeName, ep.getResponseParts()));
                    } else {
                        // سازگاری با حالت قدیم: responseFields
                        if (ep.getResponseFields() != null && !ep.getResponseFields().isEmpty()) {
                            write(dtoDir.resolve(compositeName + ".java"),
                                    genDtoJava(pkgBase + ".dto", compositeName, ep.getResponseFields()));
                        }
                    }
                }
            }
        }

        // --- 4) کنترلرها
        if (p.getControllers() != null && !p.getControllers().isEmpty()) {
            write(ctrlDir.resolve("Controller.java"),
                    unifiedControllerJava(pkgBase + ".controller", p.getControllers(), pkgBase + ".dto"));
        }

        // --- 5) منابع (resources)
        write(resources.resolve("application.yml"),     baseAppYml(artifact));
        write(resources.resolve("application-dev.yml"), appDevYml());
        write(resources.resolve("application-test.yml"),appTestYml());
        write(resources.resolve("application-prod.yml"),appProdYml());

        // i18n (اگر در Project ست شده)
        writeI18nFromSettings(p, resources);

        // پیام‌ها/ثوابتِ پیش‌فرض (در صورت تمایل نگه‌داری)
        if (!java.nio.file.Files.exists(resources.resolve("messages.properties")))
            write(resources.resolve("messages.properties"), messagesDefault());
        if (!java.nio.file.Files.exists(resources.resolve("messages_en.properties")))
            write(resources.resolve("messages_en.properties"), messagesEn());
        if (!java.nio.file.Files.exists(resources.resolve("messages_fa.properties")))
            write(resources.resolve("messages_fa.properties"), messagesFa());
        if (!java.nio.file.Files.exists(resources.resolve("constants.properties")))
            write(resources.resolve("constants.properties"), constantsProps());

        // --- 6) به‌روزرسانی POM براساس امنیت (و هر چیز دیگری)
        updatePomForSecurity(p, pomPath);     // ⚠️ به pomPath بده، نه root
        // در صورت نیاز: updatePomForSwagger(p, pomPath);
    }


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

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent()); // ✅
        Files.writeString(file, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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

    public static String artifactId(Project p) {
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

    private String controllerJava(String pkg, ControllerDef c) {
        String className = safeTypeName(c.getName());      // مثلا OrderController
        String basePath  = nvl(c.getBasePath(), "/");
        String tagName   = className;

        // آیا در خروجی به List نیاز داریم (برای نوع بازگشتی List<...>)؟
        boolean needsListImport = c.getEndpoints() != null && c.getEndpoints().stream().anyMatch(ep -> {
            if (ep == null) return false;
            if (ep.getResponseParts() != null && !ep.getResponseParts().isEmpty()) {
                // در حالت پاسخ مرکب، خود کلاس مرکب return می‌شود (نه List مستقیم)
                return false;
            }
            return ep.isResponseList();
        });

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n")
                .append("import org.springframework.http.ResponseEntity;\n")
                .append("import org.springframework.web.bind.annotation.*;\n")
                // Swagger
                .append("import io.swagger.v3.oas.annotations.Operation;\n")
                .append("import io.swagger.v3.oas.annotations.Parameter;\n")
                .append("import io.swagger.v3.oas.annotations.enums.ParameterIn;\n")
                .append("import io.swagger.v3.oas.annotations.media.Content;\n")
                .append("import io.swagger.v3.oas.annotations.media.Schema;\n")
                .append("import io.swagger.v3.oas.annotations.responses.ApiResponse;\n")
                .append("import io.swagger.v3.oas.annotations.responses.ApiResponses;\n")
                .append("import io.swagger.v3.oas.annotations.tags.Tag;\n");
        if (needsListImport) sb.append("import java.util.List;\n");
        sb.append("\n");

        sb.append("@RestController\n")
                .append("@RequestMapping(\"").append(basePath).append("\")\n")
                .append("@Tag(name = \"").append(tagName).append("\", description = \"Endpoints for ").append(tagName).append("\")\n")
                .append("public class ").append(className).append(" {\n\n");

        if (c.getEndpoints() != null) {
            for (var ep : c.getEndpoints()) {
                if (ep == null) continue;
                sb.append(genEndpointMethod(pkg, className, ep, tagName)).append("\n");
            }
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
            Files.writeString(yml, "\n" + content, StandardOpenOption.APPEND);
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

    private void writeI18nFromSettings(Project p, Path resources) throws java.io.IOException {
        var i = p.getI18n();
        if (i == null) return;

        // baseName پیش‌فرض
        String base = (i.getBaseName() == null || i.getBaseName().isBlank())
                ? "messages"
                : i.getBaseName().trim();

        // لیست زبان‌ها
        java.util.List<String> langs = (i.getLanguages() == null || i.getLanguages().isEmpty())
                ? java.util.List.of("fa", "en")
                : i.getLanguages();

        // اطمینان از وجود دایرکتوری resources
        Files.createDirectories(resources);

        // فایل base بدون پسوند زبان (اگر از قبل نبود، یک قالب مینیمال بنویس)
        Path baseFile = resources.resolve(base + ".properties");
        if (!Files.exists(baseFile)) {
            Files.writeString(baseFile, "# i18n base\n");
        }

        // برای هر زبان یک فایل *.properties بساز
        for (String lang : langs) {
            Path f = resources.resolve(base + "_" + lang + ".properties");
            StringBuilder sb = new StringBuilder();

            if (i.getKeys() != null) {
                for (var k : i.getKeys()) {
                    if (k == null) continue;
                    String key = k.getKey(); // ← نام کلید (نه code)
                    if (key == null || key.isBlank()) continue;

                    String val = (k.getTranslations() != null) ? k.getTranslations().get(lang) : null;
                    if (val != null) {
                        sb.append(key)
                                .append("=")
                                .append(escapeProp(val))   // فرض بر اینکه متد escapeProp موجود است
                                .append("\n");
                    }
                }
            }

            if (sb.length() == 0) {
                sb.append("# ").append(lang).append("\n");
            }
            Files.writeString(f, sb.toString());
        }
    }

    private static String escapeProp(String s) {
        if (s == null) return "";
        // escape پایه‌ای برای فایل‌های .properties
        return s
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("=", "\\=")
                .replace(":", "\\:")
                .replace("#", "\\#")
                .replace("!", "\\!");
    }

    private void writeSecurityArtifacts(Project p, String pkgBase, Path srcMain, Path resources) throws IOException {
        var s = p.getSecurity();
        if (s == null || s.getAuthType() == null || "NONE".equalsIgnoreCase(s.getAuthType().name())) {
            // بدون امنیت: فقط یک SecurityConfig با permitAll (یا اصلاً نساختن)
            writeSecurityConfigNone(pkgBase, srcMain, p);
            return;
        }
        switch (s.getAuthType()) {
            case BASIC -> {
                writeSecurityConfigBasic(pkgBase, srcMain, p);
                writeUsersConfigBasic(pkgBase, srcMain, p);
            }
            case BEARER -> writeSecurityConfigBearer(pkgBase, srcMain, p);
            case JWT -> {
                writeJwtSupport(pkgBase, srcMain);            // JwtAuthFilter / JwtUtil
                writeSecurityConfigJwt(pkgBase, srcMain, p);  // SecurityConfig با فیلتر JWT
            }
            case OAUTH2 -> writeSecurityConfigOAuth2(pkgBase, srcMain, p); // اسکلت اولیه
            default -> writeSecurityConfigNone(pkgBase, srcMain, p);
        }
    }

    private Path pkgDir(Path srcMain, String pkg) throws IOException {
        Path dir = srcMain.resolve(pkg.replace('.','/'));
        Files.createDirectories(dir);
        return dir;
    }

    private void writeSecurityConfigNone(String pkgBase, Path srcMain, Project p) throws IOException {
        Path cfgDir = pkgDir(srcMain, pkgBase + ".config");
        String cls = """
        package %s.config;

        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;
        import org.springframework.security.config.annotation.web.builders.HttpSecurity;
        import org.springframework.security.web.SecurityFilterChain;

        @Configuration
        public class SecurityConfig {

            @Bean
            public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                    );
                return http.build();
            }
        }
        """.formatted(pkgBase);
        write(cfgDir.resolve("SecurityConfig.java"), cls);
    }

    private void writeSecurityConfigBasic(String pkgBase, Path srcMain, Project p) throws IOException {
        Path cfgDir = pkgDir(srcMain, pkgBase + ".config");

        String authorize = buildAuthorizeBlock(p); // ← متدی که از rules پروژه رشته‌ی جاوا تولید می‌کند

        String cls = """
        package %s.config;

        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;
        import org.springframework.security.config.annotation.web.builders.HttpSecurity;
        import org.springframework.security.web.SecurityFilterChain;

        @Configuration
        public class SecurityConfig {

            @Bean
            public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                    .csrf(csrf -> csrf.disable())
                    .httpBasic(basic -> {}) // Basic
                    .authorizeHttpRequests(auth -> {
                        %s
                    });
                return http.build();
            }
        }
        """.formatted(pkgBase, authorize);
        write(cfgDir.resolve("SecurityConfig.java"), cls);
    }

    private void writeSecurityConfigBearer(String pkgBase, Path srcMain, Project p) throws IOException {
        Path cfgDir = pkgDir(srcMain, pkgBase + ".config");

        String authorize = buildAuthorizeBlock(p);

        String filterCls = """
        package %s.config;

        import jakarta.servlet.*;
        import jakarta.servlet.http.HttpServletRequest;
        import jakarta.servlet.http.HttpServletResponse;
        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.stereotype.Component;
        import java.io.IOException;

        @Component
        public class BearerTokenFilter implements Filter {

            @Value("${security.bearer.token:}")
            private String expected;

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse res = (HttpServletResponse) response;

                String auth = req.getHeader("Authorization");
                if (auth != null && auth.startsWith("Bearer ")) {
                    String token = auth.substring(7);
                    if (expected != null && !expected.isBlank() && expected.equals(token)) {
                        chain.doFilter(request, response);
                        return;
                    }
                }
                // برای مسیرهای permitAll باید قبل از این فیلتر Skip شود؛ از SecurityMatcher استفاده می‌کنیم
                chain.doFilter(request, response);
            }
        }
        """.formatted(pkgBase);
        write(cfgDir.resolve("BearerTokenFilter.java"), filterCls);

        String cfgCls = """
        package %s.config;

        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;
        import org.springframework.security.config.annotation.web.builders.HttpSecurity;
        import org.springframework.security.web.SecurityFilterChain;
        import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

        @Configuration
        public class SecurityConfig {

            private final BearerTokenFilter bearerTokenFilter;

            public SecurityConfig(BearerTokenFilter f){ this.bearerTokenFilter = f; }

            @Bean
            public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                    .csrf(csrf -> csrf.disable())
                    .addFilterBefore(bearerTokenFilter, BasicAuthenticationFilter.class)
                    .authorizeHttpRequests(auth -> {
                        %s
                    });

                return http.build();
            }
        }
        """.formatted(pkgBase, authorize);

        write(cfgDir.resolve("SecurityConfig.java"), cfgCls);
    }

    private void writeJwtSupport(String pkgBase, Path srcMain) throws IOException {
        Path cfgDir = pkgDir(srcMain, pkgBase + ".config");

        String util = """
        package %s.config;

        import io.jsonwebtoken.*;
        import io.jsonwebtoken.security.Keys;
        import java.security.Key;
        import java.util.Date;

        public class JwtUtil {
            private final Key key;
            private final String issuer;
            private final String audience;
            private final long   expirationMillis;

            public JwtUtil(String secret, String issuer, String audience, long expSeconds){
                this.key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                this.issuer = issuer;
                this.audience = audience;
                this.expirationMillis = expSeconds * 1000L;
            }

            public String generate(String subject){
                long now = System.currentTimeMillis();
                return Jwts.builder()
                        .setSubject(subject)
                        .setIssuer(issuer)
                        .setAudience(audience)
                        .setIssuedAt(new Date(now))
                        .setExpiration(new Date(now + expirationMillis))
                        .signWith(key, SignatureAlgorithm.HS256)
                        .compact();
            }

            public Jws<Claims> parse(String token){
                return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            }
        }
        """.formatted(pkgBase);
        write(cfgDir.resolve("JwtUtil.java"), util);

        String filter = """
        package %s.config;

        import jakarta.servlet.*;
        import jakarta.servlet.http.HttpServletRequest;
        import jakarta.servlet.http.HttpServletResponse;
        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.stereotype.Component;
        import java.io.IOException;
        import io.jsonwebtoken.*;

        @Component
        public class JwtAuthFilter implements Filter {

            @Value("${security.jwt.secret:}")
            private String secret;
            @Value("${security.jwt.issuer:}")
            private String issuer;
            @Value("${security.jwt.audience:}")
            private String audience;
            @Value("${security.jwt.expSeconds:3600}")
            private long expSeconds;

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse res = (HttpServletResponse) response;

                String auth = req.getHeader("Authorization");
                if (auth != null && auth.startsWith("Bearer ")) {
                    String token = auth.substring(7);
                    try {
                        JwtUtil util = new JwtUtil(secret, issuer, audience, expSeconds);
                        util.parse(token); // فقط صحت امضا/انقضا؛ اینجا می‌توانی SecurityContext ست کنی
                    } catch (JwtException ex){
                        // Invalid → ادامه بده تا authorizeHttpRequests تصمیم بگیرد
                    }
                }
                chain.doFilter(request, response);
            }
        }
        """.formatted(pkgBase);
        write(cfgDir.resolve("JwtAuthFilter.java"), filter);
    }

    private void writeSecurityConfigJwt(String pkgBase, Path srcMain, Project p) throws IOException {
        Path cfgDir = pkgDir(srcMain, pkgBase + ".config");
        String authorize = buildAuthorizeBlock(p);

        String cfg = """
        package %s.config;

        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;
        import org.springframework.security.config.annotation.web.builders.HttpSecurity;
        import org.springframework.security.web.SecurityFilterChain;
        import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

        @Configuration
        public class SecurityConfig {

            private final JwtAuthFilter jwtAuthFilter;

            public SecurityConfig(JwtAuthFilter f){ this.jwtAuthFilter = f; }

            @Bean
            public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                    .csrf(csrf -> csrf.disable())
                    .addFilterBefore(jwtAuthFilter, BasicAuthenticationFilter.class)
                    .authorizeHttpRequests(auth -> {
                        %s
                    });
                return http.build();
            }
        }
        """.formatted(pkgBase, authorize);
        write(cfgDir.resolve("SecurityConfig.java"), cfg);
    }

    private void writeSecurityConfigOAuth2(String pkgBase, Path srcMain, Project p) throws IOException {
        Path cfgDir = pkgDir(srcMain, pkgBase + ".config");
        String authorize = buildAuthorizeBlock(p);
        String cfg = """
        package %s.config;

        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;
        import org.springframework.security.config.annotation.web.builders.HttpSecurity;
        import org.springframework.security.web.SecurityFilterChain;

        @Configuration
        public class SecurityConfig {

            @Bean
            public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                    .csrf(csrf -> csrf.disable())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                    .authorizeHttpRequests(auth -> {
                        %s
                    });
                return http.build();
            }
        }
        """.formatted(pkgBase, authorize);
        write(cfgDir.resolve("SecurityConfig.java"), cfg);
    }

    private String buildAuthorizeBlock(Project p) {
        // اگر هیچ rule نباشد → همه authenticated (به جز swagger/actuator اگر خواستی permitAll کنی)
        var s = p.getSecurity();
        var rules = (s != null ? s.getRules() : null);

        StringBuilder sb = new StringBuilder();
        // Swagger/OpenAPI را معمولاً آزاد می‌گذاریم:
        sb.append("auth.requestMatchers(\"/v3/api-docs/**\",\"/swagger-ui/**\",\"/swagger-ui.html\").permitAll();\n");

        if (rules == null || rules.isEmpty()) {
            sb.append("auth.anyRequest().authenticated();\n");
            return sb.toString();
        }

        for (var r : rules) {
            if (r == null || r.getPathPattern() == null || r.getPathPattern().isBlank()) continue;
            String pattern = r.getPathPattern().trim();
            String method = (r.getHttpMethod()==null || r.getHttpMethod().isBlank()) ? "ANY" : r.getHttpMethod().trim().toUpperCase();
            String req    = (r.getRequirement()==null? "" : r.getRequirement().trim());

            // نمونه‌های requirement: "permitAll", "authenticated", "hasRole('ADMIN')", "hasAnyRole('A','B')"
            String matcherLine;
            if ("ANY".equals(method)) {
                matcherLine = "auth.requestMatchers(\"" + pattern + "\")";
            } else {
                matcherLine = "auth.requestMatchers(org.springframework.http.HttpMethod." + method + ", \"" + pattern + "\")";
            }
            sb.append(matcherLine).append(".")
                    .append(req.isEmpty()? "authenticated()" : req)
                    .append(";\n");
        }
        sb.append("auth.anyRequest().authenticated();\n");
        return sb.toString();
    }

    private void writeSecurityProps(Project p, Path resources) throws IOException {
        var s = p.getSecurity();
        if (s == null) return;

        StringBuilder y = new StringBuilder();
        y.append("# --- security generated by AppMaker ---\n");

        switch (s.getAuthType()) {
            case BASIC -> {
                // بهتر است از in-memory user استفاده نکنی و با UserDetailsService بسازی؛
                // اما برای سادگی، کرنشیال‌ها را همین‌جا می‌گذاریم:
                y.append("security:\n")
                        .append("  basic:\n")
                        .append("    username: ").append(nvl(s.getBasicUsername(), "user")).append("\n")
                        .append("    password: ").append(nvl(s.getBasicPassword(), "pass")).append("\n");
                // توجه: SecurityConfig بالا از httpBasic استفاده می‌کند اما برای in-memory user باید بنویسیم؛
                // اگر خواستی کامل‌ترش کنم بگو (ایجاد یک @Bean UserDetailsService + PasswordEncoder).
            }
            case BEARER -> {
                y.append("security:\n")
                        .append("  bearer:\n")
                        .append("    token: ").append(nvl(s.getBearerToken(), "CHANGEME")).append("\n");
            }
            case JWT -> {
                y.append("security:\n")
                        .append("  jwt:\n")
                        .append("    secret: ").append(nvl(s.getJwtSecret(), "CHANGE_ME_256bit_secret")).append("\n")
                        .append("    issuer: ").append(nvl(s.getJwtIssuer(), "app-maker")).append("\n")
                        .append("    audience: ").append(nvl(s.getJwtAudience(), "app-clients")).append("\n")
                        .append("    expSeconds: ").append(s.getJwtExpirationSeconds()==null?3600:s.getJwtExpirationSeconds()).append("\n");
            }
            case OAUTH2 -> {
                y.append("spring:\n")
                        .append("  security:\n")
                        .append("    oauth2:\n")
                        .append("      resourceserver:\n")
                        .append("        jwt:\n")
                        .append("          issuer-uri: ").append(nvl(s.getOauth2Issuer(), "https://issuer.example.com")).append("\n")
                        .append("# client-id / client-secret اگر لازم باشد به صورت client credentials اضافه شود.\n");
            }
            default -> { /* NONE */ }
        }

        Path appYml = resources.resolve("application.yml");
        appendOrWrite(appYml, y.toString());
    }

    private static String nvl(String v, String d){ return (v==null || v.isBlank())? d : v; }

    private void appendOrWrite(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        if (Files.exists(file)) {
            Files.writeString(file, "\n" + content, java.nio.file.StandardOpenOption.APPEND);
        } else {
            Files.writeString(file, content);
        }
    }

    private void updatePomForSecurity(Project p, java.nio.file.Path pomPath) throws java.io.IOException {
        if (p == null || pomPath == null || !java.nio.file.Files.exists(pomPath)) return;

        var sec = p.getSecurity();
        if (sec == null || sec.getAuthType() == null || sec.getAuthType() == SecuritySettings.AuthType.NONE)
            return; // نیازی به تغییر POM نیست

        String pom = java.nio.file.Files.readString(pomPath);

        // مطمئن شو بلاک dependencies داریم
        if (!pom.contains("<dependencies>")) {
            pom = pom.replace("</project>",
                    "  <dependencies>\n  </dependencies>\n</project>");
        }

        java.util.List<String> depsToAdd = new java.util.ArrayList<>();

        // همه حالت‌ها (به‌جز NONE) به starter-security نیاز دارند
        String starterSecurity =
                """
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-security</artifactId>
                </dependency>
                """;
        if (!pom.contains("<artifactId>spring-boot-starter-security</artifactId>")) {
            depsToAdd.add(starterSecurity);
        }

        switch (sec.getAuthType()) {
            case BASIC, BEARER -> {
                // همین starter کافی است (برای BEARER ثابت هم نیازی به JWT نیست)
            }
            case JWT, OAUTH2 -> {
                // برای JWT/OAuth2: ریسورس سرور
                String oauth2Res =
                        """
                        <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
                        </dependency>
                        """;
                if (!pom.contains("<artifactId>spring-boot-starter-oauth2-resource-server</artifactId>")) {
                    depsToAdd.add(oauth2Res);
                }
                // اگر با JJWT کار می‌کنی (اختیاری)
                String jjwtApi =
                        """
                        <dependency>
                          <groupId>io.jsonwebtoken</groupId>
                          <artifactId>jjwt-api</artifactId>
                          <version>0.11.5</version>
                        </dependency>
                        """;
                String jjwtImpl =
                        """
                        <dependency>
                          <groupId>io.jsonwebtoken</groupId>
                          <artifactId>jjwt-impl</artifactId>
                          <version>0.11.5</version>
                          <scope>runtime</scope>
                        </dependency>
                        """;
                String jjwtJackson =
                        """
                        <dependency>
                          <groupId>io.jsonwebtoken</groupId>
                          <artifactId>jjwt-jackson</artifactId>
                          <version>0.11.5</version>
                          <scope>runtime</scope>
                        </dependency>
                        """;
                if (!pom.contains("<artifactId>jjwt-api</artifactId>"))     depsToAdd.add(jjwtApi);
                if (!pom.contains("<artifactId>jjwt-impl</artifactId>"))    depsToAdd.add(jjwtImpl);
                if (!pom.contains("<artifactId>jjwt-jackson</artifactId>")) depsToAdd.add(jjwtJackson);
            }
            default -> {}
        }

        if (!depsToAdd.isEmpty()) {
            pom = pom.replace("</dependencies>", String.join("\n", depsToAdd) + "\n  </dependencies>");
            java.nio.file.Files.writeString(pomPath, pom);
        }
    }


    private void updatePomForSwagger(Project p, Path pomPath) throws IOException {
        if (p == null) return;
        var sw = p.getSwagger();
        if (sw == null || !Boolean.TRUE.equals(sw.isEnabled())) return; // فقط وقتی فعال است

        if (!Files.exists(pomPath)) return;

        String pom = Files.readString(pomPath);

        // مطمئن شو بلاک های لازم وجود دارند
        pom = ensurePropertiesBlock(pom);
        pom = ensureDependenciesBlock(pom);

        // 1) property نسخه springdoc (اگر نبود)
        pom = ensureProperty(pom, "springdoc.version", "2.6.0"); // نسخه متداول؛ در صورت نیاز تغییر بده

        // 2) اطمینان از وجود وب استارتر (برای سرو کردن Swagger UI ضروری است)
        pom = ensureDependency(pom,
                "org.springframework.boot", "spring-boot-starter-web", null, null);

        // 3) وابستگی springdoc (WebMVC + UI)
        pom = ensureDependency(pom,
                "org.springdoc", "springdoc-openapi-starter-webmvc-ui", "${springdoc.version}", null);

        Files.writeString(pomPath, pom, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String genEndpointMethod(String pkg, String className, EndpointDef ep, String tagName) {
        String http = nvl(ep.getHttpMethod(), "GET").toUpperCase(java.util.Locale.ROOT);
        String mappingAnn = switch (http) {
            case "POST" -> "@PostMapping";
            case "PUT" -> "@PutMapping";
            case "PATCH" -> "@PatchMapping";
            case "DELETE" -> "@DeleteMapping";
            default -> "@GetMapping";
        };
        String methodName = safeMethodName(nvl(ep.getName(), suggestMethodName(http, nvl(ep.getPath(), ""))));
        String path = nvl(ep.getPath(), "");
        String mapping = path.isBlank() ? mappingAnn : mappingAnn + "(\"" + path + "\")";

        // نوع بازگشتی
        String returnType = resolveReturnTypeFor(ep);

        // پارامترهای امضا + @Parameter
        StringBuilder paramsSig = new StringBuilder();
        StringBuilder paramAnns  = new StringBuilder();

        if (ep.getParams() != null) {
            for (var p : ep.getParams()) {
                if (p == null || p.getName() == null || p.getName().isBlank()) continue;

                String pname = safeVarName(p.getName().trim());
                String jtype = mapJavaType(nvl(p.getJavaType(), "String"));
                String in    = nvl(p.getIn(), "QUERY").toUpperCase(java.util.Locale.ROOT);
                boolean req  = p.isRequired();

                switch (in) {
                    case "PATH" -> {
                        appendParam(paramsSig, "@PathVariable " + jtype + " " + pname);

                        paramAnns.append("@Parameter(name=\"").append(p.getName())
                                .append("\", in=ParameterIn.PATH, required=").append(req).append(")\n");
                    }
                    case "HEADER" -> {
                        appendParam(paramsSig,
                                "@RequestHeader(name=\"" + p.getName() + "\", required=" + req + ") "
                                        + jtype + " " + pname
                        );

                        paramAnns.append("@Parameter(name=\"").append(p.getName())
                                .append("\", in=ParameterIn.HEADER, required=").append(req).append(")\n");
                    }
                    default -> { // QUERY
                        appendParam(paramsSig,
                                "@RequestParam(name=\"" + p.getName() + "\", required=" + req + ") "
                                        + jtype + " " + pname
                        );

                        paramAnns.append("@Parameter(name=\"").append(p.getName())
                                .append("\", in=ParameterIn.QUERY, required=").append(req).append(")\n");
                    }
                }
            }
        }

        // Body برای POST/PUT/PATCH
        boolean hasBody = http.equals("POST") || http.equals("PUT") || http.equals("PATCH");
        if (hasBody) {
            String bodyType = null;
            if (ep.getRequestFields() != null && !ep.getRequestFields().isEmpty()) {
                bodyType = upperCamel(nvl(ep.getName(), "Op")) + "Request";
            } else if (ep.getRequestBodyType() != null && !ep.getRequestBodyType().isBlank()) {
                bodyType = ep.getRequestBodyType().trim();
            }
            if (bodyType != null) {
                appendParam(paramsSig, "@org.springframework.web.bind.annotation.RequestBody " + bodyType + " body");
            }
        }

        String summary = buildSummaryForOperation(http, path, methodName);

        StringBuilder b = new StringBuilder();
        b.append("  @Operation(summary = \"").append(escape(summary)).append("\", tags = {\"").append(tagName).append("\"})\n");
        // اگر می‌خواهی پارامترها هم ظاهر شوند
        if (paramAnns.length() > 0) b.append("  ").append(paramAnns);

        b.append("  @ApiResponses(value = {\n")
                .append("    @ApiResponse(responseCode=\"200\", description=\"OK\", content=@Content(mediaType=\"application/json\"))\n")
                .append("  })\n");

        b.append("  ").append(mapping).append("\n");
        b.append("  public ").append(returnType).append(" ").append(methodName).append("(").append(paramsSig).append(") {\n");

        // بدنه‌ی ساده
        if (returnType.contains("List<")) {
            b.append("    return ResponseEntity.ok(java.util.List.of());\n");
        } else if (returnType.contains("Response>") || returnType.contains("Dto") || returnType.contains("ResponseEntity<Object>")) {
            b.append("    return ResponseEntity.ok(null);\n");
        } else {
            b.append("    return ResponseEntity.ok(null);\n");
        }
        b.append("  }\n");

        return b.toString();
    }

    private String resolveReturnTypeFor(EndpointDef ep) {
        String methodNamePascal = upperCamel(nvl(ep.getName(), "Op"));
        String compositeName = (ep.getResponseModelName()!=null && !ep.getResponseModelName().isBlank())
                ? upperCamel(ep.getResponseModelName().trim())
                : methodNamePascal + "Response";

        boolean hasParts = ep.getResponseParts()!=null && !ep.getResponseParts().isEmpty();
        if (hasParts) {
            return "ResponseEntity<" + compositeName + ">";
        } else {
            String responseDtoName = (ep.getResponseFields()!=null && !ep.getResponseFields().isEmpty())
                    ? compositeName : null;
            String respType = (responseDtoName != null)
                    ? responseDtoName
                    : (ep.getResponseType()==null || ep.getResponseType().isBlank() ? "String" : ep.getResponseType());
            boolean list = ep.isResponseList();
            return list ? "ResponseEntity<List<" + respType + ">>" : "ResponseEntity<" + respType + ">";
        }
    }

    private String safeVarName(String name) {
        String n = name.replaceAll("[^A-Za-z0-9_]","");
        if (n.isEmpty()) n = "p";
        if (!Character.isJavaIdentifierStart(n.charAt(0))) n = "p" + n;
        return n.substring(0,1).toLowerCase() + n.substring(1);
    }

    private static String escape(String s){ return s.replace("\"","\\\""); }

    private void appendParam(StringBuilder b, String piece){
        if (b.length() > 0) b.append(", ");
        b.append(piece);
    }

    private String safeTypeName(String s){
        if (s == null || s.isBlank()) return "ApiController";
        String t = s.replaceAll("[^A-Za-z0-9]",""); // ساده
        if (!Character.isJavaIdentifierStart(t.charAt(0))) t = "C" + t;
        return t;
    }
    private String safeMethodName(String s){
        if (s == null || s.isBlank()) return "op";
        String t = s.replaceAll("[^A-Za-z0-9_]","");
        if (!Character.isJavaIdentifierStart(t.charAt(0))) t = "m" + t;
        return t.substring(0,1).toLowerCase() + t.substring(1);
    }

    private String suggestMethodName(String http, String path){
        String base = http.toLowerCase(Locale.ROOT);
        if (path==null || path.isBlank()) return base;
        String cleaned = path.replaceAll("[{}]","").replaceAll("[^A-Za-z0-9]+","-");
        StringBuilder sb = new StringBuilder(base);
        for (String part : cleaned.split("-")) {
            if (part.isBlank()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }
    private String mapJavaType(String t){
        return switch (t) {
            case "Long","Integer","Double","Boolean","UUID","LocalDate","LocalDateTime","String" -> t;
            default -> "String";
        };
    }
    private String buildSummaryForOperation(String http, String path, String method){
        if (path==null || path.isBlank()) return http + " " + method;
        return http + " " + path + " → " + method;
    }
    private String swaggerParamImportsBlock(){
        // برای اطمینان: اگر از @Parameter با Position enum استفاده کردی:
        return "import io.swagger.v3.oas.annotations.enums.ParameterIn;\nimport io.swagger.v3.oas.annotations.Parameter;";
    }

    private String ensureDependency(String pom, String groupId, String artifactId, String version) {
        return ensureDependency(pom, groupId, artifactId, version, null);
    }

    private String ensureDependency(String pom, String groupId, String artifactId, String version, String scope) {
        // اگر قبلاً اضافه شده بود، دست نزن
        if (pom.contains("<groupId>" + groupId + "</groupId>") && pom.contains("<artifactId>" + artifactId + "</artifactId>")) {
            return pom;
        }

        StringBuilder dep = new StringBuilder();
        dep.append("    <dependency>\n")
                .append("      <groupId>").append(groupId).append("</groupId>\n")
                .append("      <artifactId>").append(artifactId).append("</artifactId>\n");
        if (version != null && !version.isBlank()) {
            dep.append("      <version>").append(version).append("</version>\n");
        }
        if (scope != null && !scope.isBlank()) {
            dep.append("      <scope>").append(scope).append("</scope>\n");
        }
        dep.append("    </dependency>\n");

        // قبل از </dependencies> قرار بده
        int idx = pom.indexOf("</dependencies>");
        if (idx >= 0) {
            return pom.substring(0, idx) + dep + pom.substring(idx);
        } else {
            // اگر به هر دلیلی نبود، در انتهای فایل بگذار
            return pom + "\n  <dependencies>\n" + dep + "  </dependencies>\n";
        }
    }

    private String ensurePropertiesBlock(String pom) {
        if (!pom.contains("<properties>")) {
            pom = pom.replace("</project>", "  <properties>\n  </properties>\n</project>");
        }
        return pom;
    }

    private String ensureDependenciesBlock(String pom) {
        if (!pom.contains("<dependencies>")) {
            pom = pom.replace("</project>", "  <dependencies>\n  </dependencies>\n</project>");
        }
        return pom;
    }

    private String ensureProperty(String pom, String name, String value) {
        // اگر همین property قبلاً وجود دارد، دست نزن
        if (pom.contains("<" + name + ">")) return pom;

        int idx = pom.indexOf("</properties>");
        if (idx >= 0) {
            String insert = "    <" + name + ">" + value + "</" + name + ">\n";
            return pom.substring(0, idx) + insert + pom.substring(idx);
        } else {
            // اگر properties نبود (نباید به اینجا برسیم چون ensurePropertiesBlock را صدا زدیم)
            return pom + "\n  <properties>\n    <" + name + ">" + value + "</" + name + ">\n  </properties>\n";
        }
    }


    // در writeSecurityConfigBasic کنار SecurityConfig بساز:
    private void writeUsersConfigBasic(String pkgBase, Path srcMain, Project p) throws IOException {
        Path cfgDir = pkgDir(srcMain, pkgBase + ".config");
        String cls = """
        package %s.config;

        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;
        import org.springframework.security.core.userdetails.User;
        import org.springframework.security.core.userdetails.UserDetailsService;
        import org.springframework.security.provisioning.InMemoryUserDetailsManager;
        import org.springframework.security.crypto.password.NoOpPasswordEncoder;
        import org.springframework.security.crypto.password.PasswordEncoder;

        @Configuration
        public class SecurityUsersConfig {

            @Value("${security.basic.username:user}")
            private String username;

            @Value("${security.basic.password:pass}")
            private String password;

            @Bean
            public UserDetailsService userDetailsService(){
                return new InMemoryUserDetailsManager(
                    User.withUsername(username).password(password).roles("USER").build()
                );
            }

            @Bean
            public PasswordEncoder passwordEncoder(){
                // برای دِمو؛ در عمل از BCrypt استفاده کن
                return NoOpPasswordEncoder.getInstance();
            }
        }
        """.formatted(pkgBase);
        write(cfgDir.resolve("SecurityUsersConfig.java"), cls);
    }


    private String unifiedControllerJava(String pkg,
                                         java.util.List<ControllerDef> controllers,
                                         String dtoPackage) {

        StringBuilder sb = new StringBuilder();
        boolean needsListImport = controllers != null && controllers.stream().anyMatch(c ->
                c.getEndpoints() != null && c.getEndpoints().stream().anyMatch(ep ->
                        ep.isResponseList() || (ep.getResponseParts()!=null && !ep.getResponseParts().isEmpty())
                )
        );

        sb.append("package ").append(pkg).append(";\n\n")
                .append("import org.springframework.http.ResponseEntity;\n")
                .append("import org.springframework.web.bind.annotation.*;\n")
                .append("import io.swagger.v3.oas.annotations.*;\n")
                .append("import io.swagger.v3.oas.annotations.media.*;\n")
                .append("import io.swagger.v3.oas.annotations.responses.*;\n")
                .append("import io.swagger.v3.oas.annotations.tags.Tag;\n");
        if (needsListImport) sb.append("import java.util.List;\nimport java.util.Collections;\n");
        sb.append('\n');

        // اگر DTO تولید می‌کنی:
        sb.append("import ").append(dtoPackage).append(".*;\n\n");

        sb.append("@RestController\n")
                .append("@RequestMapping(\"/api\")\n")
                .append("public class Controller {\n\n");

        if (controllers != null) {
            for (var c : controllers) {
                String basePath = nvl(c.getBasePath(), "/");          // مثل /api/orders
                String tagName  = nvl(c.getName(), "API");             // برای Swagger

                if (c.getEndpoints() == null) continue;
                for (var ep : c.getEndpoints()) {

                    // --- HTTP method و آنتیشن ---
                    String http = nvl(ep.getHttpMethod(), "GET").toUpperCase(java.util.Locale.ROOT);
                    String mappingAnn = switch (http) {
                        case "POST"   -> "@PostMapping";
                        case "PUT"    -> "@PutMapping";
                        case "PATCH"  -> "@PatchMapping";
                        case "DELETE" -> "@DeleteMapping";
                        default       -> "@GetMapping";
                    };

                    // --- مسیر نهایی: basePath + endpointPath (اگر تعریف شده) ---
                    String fullPath = combinePaths(basePath, ep.getPath());

                    // --- نام متد جاوا ---
                    String methodName = (ep.getName()==null || ep.getName().isBlank())
                            ? suggestMethodName(http, fullPath)
                            : safeVarName(ep.getName().trim());

                    // --- خروجی: ResponseEntity<...> ---
                    String returnType;
                    boolean hasParts = ep.getResponseParts()!=null && !ep.getResponseParts().isEmpty();
                    if (hasParts) {
                        // اسم مدل مرکب خروجی که قبلاً ساختیم
                        String composite = upperCamel(methodName) + "Response";
                        if (ep.getResponseModelName()!=null && !ep.getResponseModelName().isBlank())
                            composite = upperCamel(ep.getResponseModelName().trim());
                        returnType = "ResponseEntity<" + composite + ">";
                    } else {
                        String scalar = nvl(ep.getResponseType(), "String");
                        returnType = ep.isResponseList()
                                ? "ResponseEntity<java.util.List<" + mapJavaType(scalar) + ">>"
                                : "ResponseEntity<" + mapJavaType(scalar) + ">";
                    }

                    // --- پارامترها ---
                    StringBuilder sig = new StringBuilder();        // پارامترهای امضای متد
                    StringBuilder swaggerParamAnns = new StringBuilder(); // @Parameter ها

                    if (ep.getParams()!=null) {
                        for (var p : ep.getParams()) {
                            if (p==null || p.getName()==null || p.getName().isBlank()) continue;
                            String pName = safeVarName(p.getName().trim());
                            String jType = mapJavaType(nvl(p.getJavaType(), "String"));
                            String loc   = nvl(p.getIn(), "QUERY").toUpperCase(java.util.Locale.ROOT);
                            boolean req  = p.isRequired();

                            switch (loc) {
                                case "PATH" -> {
                                    appendParam(sig, "@PathVariable(\""+p.getName()+"\") " + jType + " " + pName);
                                    swaggerParamAnns.append("@Parameter(name=\"").append(p.getName())
                                            .append("\", in=ParameterIn.PATH, required=").append(req).append(")\n");
                                }
                                case "HEADER" -> {
                                    appendParam(sig, "@RequestHeader(\""+p.getName()+"\") " + jType + " " + pName);
                                    swaggerParamAnns.append("@Parameter(name=\"").append(p.getName())
                                            .append("\", in=ParameterIn.HEADER, required=").append(req).append(")\n");
                                }
                                default -> { // QUERY
                                    String ann = req
                                            ? "@RequestParam(name=\""+p.getName()+"\") "
                                            : "@RequestParam(name=\""+p.getName()+"\", required=false) ";
                                    appendParam(sig, ann + jType + " " + pName);
                                    swaggerParamAnns.append("@Parameter(name=\"").append(p.getName())
                                            .append("\", in=ParameterIn.QUERY, required=").append(req).append(")\n");
                                }
                            }
                        }
                    }

                    // --- Body ---
                    boolean hasBody = http.equals("POST") || http.equals("PUT") || http.equals("PATCH");
                    if (hasBody) {
                        if (ep.getRequestFields()!=null && !ep.getRequestFields().isEmpty()) {
                            String reqDto = upperCamel(methodName) + "Request";
                            appendParam(sig, "@RequestBody " + reqDto + " body");
                        } else if (ep.getRequestBodyType()!=null && !ep.getRequestBodyType().isBlank()) {
                            appendParam(sig, "@RequestBody " + ep.getRequestBodyType().trim() + " body");
                        }
                    }

                    // --- آنتیشن‌های Swagger ---
                    sb.append("    @Tag(name = \"").append(tagName).append("\")\n");
                    sb.append("    @Operation(summary = \"").append(methodName).append("\")\n");
                    if (swaggerParamAnns.length() > 0) {
                        sb.append("    ").append(swaggerParamAnns);
                    }
                    sb.append("    @ApiResponses({\n")
                            .append("        @ApiResponse(responseCode = \"200\", description = \"OK\",\n")
                            .append("            content = @Content(mediaType = \"application/json\"))\n")
                            .append("    })\n");

                    // --- امضای متد ---
                    sb.append("    ").append(mappingAnn).append("(\"").append(fullPath).append("\")\n");
                    sb.append("    public ").append(returnType).append(" ")
                            .append(methodName).append("(").append(sig).append(") {\n");

                    // --- بدنه‌ی اولیه‌ی متد ---
                    if (hasParts) {
                        String composite = upperCamel(methodName) + "Response";
                        if (ep.getResponseModelName()!=null && !ep.getResponseModelName().isBlank())
                            composite = upperCamel(ep.getResponseModelName().trim());
                        sb.append("        ").append(composite).append(" dto = new ").append(composite).append("();\n")
                                .append("        return ResponseEntity.ok(dto);\n");
                    } else if (ep.isResponseList()) {
                        sb.append("        return ResponseEntity.ok(java.util.Collections.emptyList());\n");
                    } else {
                        String scalar = mapJavaType(nvl(ep.getResponseType(), "String"));
                        String zero = scalar.equals("String") ? "\"\"" :
                                (scalar.equals("Boolean") ? "Boolean.FALSE" :
                                        (scalar.equals("Integer") || scalar.equals("Long") || scalar.equals("Double")) ? "0" : "null");
                        sb.append("        return ResponseEntity.ok(").append(zero).append(");\n");
                    }

                    sb.append("    }\n\n");
                }
            }
        }

        sb.append("}\n");
        return sb.toString();
    }




    private static String combinePaths(String base, String rel){
        String b = (base==null?"":base.trim());
        String r = (rel==null?"":rel.trim());
        if (b.isEmpty()) b = "/";
        if (!b.startsWith("/")) b = "/"+b;
        if (b.endsWith("/")) b = b.substring(0, b.length()-1);
        if (r.isEmpty()) return b;
        if (!r.startsWith("/")) r = "/"+r;
        return b + r;
    }





}
