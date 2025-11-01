package com.company.appmaker.config;

import com.company.appmaker.model.*;
import com.company.appmaker.model.coctroller.ControllerDef;
import com.company.appmaker.model.security.SecuritySettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * تمام متدهای کمکی تولید متن/فایل/کد در این کلاس هستند.
 * ProjectScaffolder فقط orchestration انجام می‌دهد.
 */
public class ScaffoldHelpers {

    /* ======================= IO helpers ======================= */

    public void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content == null ? "" : content);
    }

    public void writeIfMissing(Path file, String content) throws IOException {
        if (!Files.exists(file)) {
            write(file, content);
        }
    }

    /* ======================= String / Name utils ======================= */

    public String artifactId(Project p) {
        String name = sanitizeIdentifier(nvl(p.getProjectName(), "app"));
        return name.toLowerCase(Locale.ROOT);
    }

    public String sanitizeGroupId(String s) {
        String x = nvl(s, "com.example");
        x = x.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]+", ".");
        x = x.replaceAll("\\.+", ".");
        if (x.startsWith(".")) x = "com" + x;
        if (x.endsWith(".")) x = x + "company";
        return x;
    }

    public String sanitizeIdentifier(String s) {
        if (s == null || s.isBlank()) return "app";
        String x = s.replaceAll("[^A-Za-z0-9_]+", "_");
        if (!Character.isJavaIdentifierStart(x.charAt(0))) x = "_" + x;
        return x;
    }

    public String safeTypeName(String s) {
        String u = upperCamel(nvl(s, "Controller"));
        if (!Character.isJavaIdentifierStart(u.charAt(0))) u = "_" + u;
        return u;
    }

    public String safeVarName(String s){
        String a = nvl(s,"_").replaceAll("[^A-Za-z0-9_]", "_");
        if (a.isEmpty() || !Character.isJavaIdentifierStart(a.charAt(0))) a = "_"+a;
        return a;
    }

    public String upperCamel(String s){
        if (s==null || s.isBlank()) return "Op";
        String[] parts = s.replaceAll("[^A-Za-z0-9]+"," ").trim().split("\\s+");
        StringBuilder b=new StringBuilder();
        for (String p: parts){ if (p.isEmpty()) continue; b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)); }
        return b.length()==0 ? "Op" : b.toString();
    }

    public String nvl(String s, String def){ return (s==null || s.isBlank()) ? def : s; }

    public String mapJavaType(String t){
        String x = nvl(t, "String").trim();
        return switch (x) {
            case "int"     -> "Integer";
            case "long"    -> "Long";
            case "double"  -> "Double";
            case "bool", "boolean" -> "Boolean";
            default -> x;
        };
    }

    public String combinePaths(String base, String rel){
        String b = (base==null?"":base.trim());
        String r = (rel==null?"":rel.trim());
        if (b.isEmpty()) b = "/";
        if (!b.startsWith("/")) b = "/"+b;
        if (b.endsWith("/")) b = b.substring(0, b.length()-1);
        if (r.isEmpty()) return b;
        if (!r.startsWith("/")) r = "/"+r;
        return b + r;
    }

    public String suggestMethodName(String http, String path){
        String base = (http==null? "get" : http.toLowerCase());
        String cleaned = (path==null? "" : path).replaceAll("[{}]","").replaceAll("[^A-Za-z0-9]+","-");
        String[] parts = cleaned.split("-");
        StringBuilder sb = new StringBuilder(base);
        for (String p: parts){
            if (p.isBlank()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    /* ======================= POM & bootstraps ======================= */

    public static String pomXml(String groupId, String artifactId, String javaVersion, List<String> pkgs) {
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

    public String appJava(String pkgBase){
        return "package " + pkgBase + ";\n\n" +
                "import org.springframework.boot.SpringApplication;\n" +
                "import org.springframework.boot.autoconfigure.SpringBootApplication;\n\n" +
                "@SpringBootApplication\n" +
                "public class App {\n" +
                "  public static void main(String[] args){ SpringApplication.run(App.class, args); }\n" +
                "}\n";
    }

    /* ======================= Controller (unified) ======================= */

    public String unifiedControllerJava(String pkg, List<ControllerDef> controllers, String dtoPackage) {
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

        sb.append("import ").append(dtoPackage).append(".*;\n\n");

        sb.append("@RestController\n")
                .append("@RequestMapping(\"/api\")\n")
                .append("public class Controller {\n\n");

        if (controllers != null) {
            for (var c : controllers) {
                String basePath = nvl(c.getBasePath(), "/");
                String tagName  = nvl(c.getName(), "API");

                if (c.getEndpoints() == null) continue;
                for (var ep : c.getEndpoints()) {

                    String http = nvl(ep.getHttpMethod(), "GET").toUpperCase(Locale.ROOT);
                    String mappingAnn = switch (http) {
                        case "POST"   -> "@PostMapping";
                        case "PUT"    -> "@PutMapping";
                        case "PATCH"  -> "@PatchMapping";
                        case "DELETE" -> "@DeleteMapping";
                        default       -> "@GetMapping";
                    };

                    String fullPath  = combinePaths(basePath, ep.getPath());
                    String methodName = (ep.getName()==null || ep.getName().isBlank())
                            ? suggestMethodName(http, fullPath)
                            : safeVarName(ep.getName().trim());

                    boolean hasParts = ep.getResponseParts()!=null && !ep.getResponseParts().isEmpty();
                    String returnType;
                    if (hasParts) {
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

                    StringBuilder sig = new StringBuilder();
                    StringBuilder swaggerParamAnns = new StringBuilder();
                    if (ep.getParams()!=null) {
                        for (var pdef : ep.getParams()) {
                            if (pdef==null || pdef.getName()==null || pdef.getName().isBlank()) continue;
                            String pName = safeVarName(pdef.getName().trim());
                            String jType = mapJavaType(nvl(pdef.getJavaType(), "String"));
                            String loc   = nvl(pdef.getIn(), "QUERY").toUpperCase(Locale.ROOT);
                            boolean req  = pdef.isRequired();

                            switch (loc) {
                                case "PATH" -> {
                                    appendParam(sig, "@PathVariable(\""+pdef.getName()+"\") " + jType + " " + pName);
                                    swaggerParamAnns.append("@Parameter(name=\"").append(pdef.getName())
                                            .append("\", in=ParameterIn.PATH, required=").append(req).append(")\n");
                                }
                                case "HEADER" -> {
                                    appendParam(sig, "@RequestHeader(\""+pdef.getName()+"\") " + jType + " " + pName);
                                    swaggerParamAnns.append("@Parameter(name=\"").append(pdef.getName())
                                            .append("\", in=ParameterIn.HEADER, required=").append(req).append(")\n");
                                }
                                default -> { // QUERY
                                    String ann = req
                                            ? "@RequestParam(name=\""+pdef.getName()+"\") "
                                            : "@RequestParam(name=\""+pdef.getName()+"\", required=false) ";
                                    appendParam(sig, ann + jType + " " + pName);
                                    swaggerParamAnns.append("@Parameter(name=\"").append(pdef.getName())
                                            .append("\", in=ParameterIn.QUERY, required=").append(req).append(")\n");
                                }
                            }
                        }
                    }

                    boolean hasBody = http.equals("POST") || http.equals("PUT") || http.equals("PATCH");
                    if (hasBody) {
                        String methodPascal = upperCamel(methodName);
                        if (ep.getRequestFields()!=null && !ep.getRequestFields().isEmpty()) {
                            appendParam(sig, "@RequestBody " + methodPascal + "Request body");
                        } else if (ep.getRequestBodyType()!=null && !ep.getRequestBodyType().isBlank()) {
                            appendParam(sig, "@RequestBody " + ep.getRequestBodyType().trim() + " body");
                        }
                    }

                    sb.append("    @Tag(name = \"").append(tagName).append("\")\n");
                    sb.append("    @Operation(summary = \"").append(methodName).append("\")\n");
                    if (swaggerParamAnns.length() > 0) {
                        sb.append("    ").append(swaggerParamAnns);
                    }
                    sb.append("    @ApiResponses({\n")
                            .append("        @ApiResponse(responseCode = \"200\", description = \"OK\",\n")
                            .append("            content = @Content(mediaType = \"application/json\"))\n")
                            .append("    })\n");

                    sb.append("    ").append(mappingAnn).append("(\"").append(fullPath).append("\")\n");
                    sb.append("    public ").append(returnType).append(" ").append(methodName)
                            .append("(").append(sig).append(") {\n");

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

    private void appendParam(StringBuilder sig, String piece){
        if (sig.length() > 0) sig.append(", ");
        sig.append(piece);
    }

    /* ======================= DTO generators ======================= */

    public String genDtoJava(String pkg, String typeName, List<FieldDef> fields){
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n")
                .append("public class ").append(typeName).append(" {\n");
        if (fields!=null) {
            for (var f : fields) {
                String t = mapJavaType(nvl(f.getJavaType(),"String"));
                String n = safeVarName(nvl(f.getName(),"field"));
                sb.append("  private ").append(t).append(" ").append(n).append(";\n");
            }
            sb.append('\n');
            for (var f : fields) {
                String t = mapJavaType(nvl(f.getJavaType(),"String"));
                String n = safeVarName(nvl(f.getName(),"field"));
                String U = upperCamel(n);
                sb.append("  public ").append(t).append(" get").append(U).append("(){ return ").append(n).append("; }\n");
                sb.append("  public void set").append(U).append("(").append(t).append(" v){ this.").append(n).append(" = v; }\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    public String genCompositeDtoJava(String pkg, String typeName, List<ResponsePartDef> parts){
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n")
                .append("import java.util.*;\n")
                .append("public class ").append(typeName).append(" {\n");

        if (parts!=null) {
            for (var part : parts) {
                if (part==null) continue;
                String fname = safeVarName(nvl(part.getName(), "result"));
                String kind  = nvl(part.getKind(),"SCALAR").toUpperCase(Locale.ROOT);
                String container = nvl(part.getContainer(),"SINGLE").toUpperCase(Locale.ROOT);

                String type;
                if ("OBJECT".equals(kind)) {
                    String obj = nvl(part.getObjectName(), upperCamel(fname) + "Dto");
                    type = obj;
                } else {
                    type = mapJavaType(nvl(part.getScalarType(),"String"));
                }
                String finalType = "LIST".equals(container) ? "java.util.List<" + type + ">" : type;
                sb.append("  private ").append(finalType).append(" ").append(fname).append(";\n");
            }

            sb.append('\n');
            for (var part : parts) {
                if (part==null) continue;
                String fname = safeVarName(nvl(part.getName(), "result"));
                String U     = upperCamel(fname);
                String kind  = nvl(part.getKind(),"SCALAR").toUpperCase(Locale.ROOT);
                String container = nvl(part.getContainer(),"SINGLE").toUpperCase(Locale.ROOT);
                String type;
                if ("OBJECT".equals(kind)) {
                    String obj = nvl(part.getObjectName(), upperCamel(fname) + "Dto");
                    type = obj;
                } else {
                    type = mapJavaType(nvl(part.getScalarType(),"String"));
                }
                String finalType = "LIST".equals(container) ? "java.util.List<" + type + ">" : type;

                sb.append("  public ").append(finalType).append(" get").append(U).append("(){ return ").append(fname).append("; }\n");
                sb.append("  public void set").append(U).append("(").append(finalType).append(" v){ this.").append(fname).append(" = v; }\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    /* ======================= application*.yml & props ======================= */

    public String baseAppYml(String appName){
        return "spring:\n" +
                "  application:\n" +
                "    name: " + appName + "\n" +
                "  messages:\n" +
                "    basename: messages\n" +
                "    fallback-to-system-locale: false\n" +
                "server:\n" +
                "  port: 8080\n";
    }
    public String appDevYml(){  return "spring:\n  profiles: dev\n"; }
    public String appTestYml(){ return "spring:\n  profiles: test\n"; }
    public String appProdYml(){ return "spring:\n  profiles: prod\n"; }

//    public String messagesDefault(){ return "# default messages\nhello=Hello\n"; }
//    public String messagesEn(){      return "# en messages\nhello=Hello\n"; }
    public String messagesFa(){      return "# fa messages\nhello=سلام\n"; }

    public String constantsProps(){  return "# constants\napp.constant.sample=value\n"; }

    /* ======================= i18n writer from settings ======================= */

    public void writeI18nFromSettings(Project p, Path resources) throws java.io.IOException {
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

    public String escapeProp(String s){
        return s.replace("\\", "\\\\").replace("\n","\\n").replace("\r","");
    }

    /* ======================= Security & Swagger (روی خروجی) ======================= */

    public void updatePomForSecurity(Project p, Path root) throws IOException {
        // اگر نیاز داشتی بر اساس p.getSecurity() dependency اضافه کنی/حذف کنی
        // فعلاً spring-boot-starter-security داخل pomXml پیش‌فرض هست.
        // اگر می‌خواهی در صورت NONE حذف شود، این‌جا pom.xml را بخوان/بازنویسی کن.
    }

    public void updatePomForSwagger(Project p, Path root) throws IOException {
        // اگر p.getSwagger()!=null و enabled=true بود ولی در pom نبود، اضافه کن.
        // در pomXml پیش‌فرض، springdoc اضافه شده؛ بنابراین می‌توانی این را خالی بگذاری.
    }

    public void writeSecurityArtifacts(Project p, Path root, String pkgBase) throws IOException {
        var s = p.getSecurity();
        if (s==null || s.getAuthType()==null || s.getAuthType()== SecuritySettings.AuthType.NONE) return;

        Path pkgDir = root.resolve("src/main/java").resolve(pkgBase.replace('.','/'));
        Path configDir = pkgDir.resolve("config");
        Files.createDirectories(configDir);

        // مینیمال SecurityConfig
        String cls = "package " + pkgBase + ".config;\n\n" +
                "import org.springframework.context.annotation.Bean;\n" +
                "import org.springframework.context.annotation.Configuration;\n" +
                "import org.springframework.security.config.annotation.web.builders.HttpSecurity;\n" +
                "import org.springframework.security.web.SecurityFilterChain;\n\n" +
                "@Configuration\n" +
                "public class SecurityConfig {\n" +
                "  @Bean\n" +
                "  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {\n" +
                "    http.csrf(csrf->csrf.disable());\n" +
                "    http.authorizeHttpRequests(auth->auth.anyRequest().authenticated());\n" +
                "    http.httpBasic(b->{});\n" + // می‌توانی بر اساس AuthType تغییر دهی
                "    return http.build();\n" +
                "  }\n" +
                "}\n";
        write(configDir.resolve("SecurityConfig.java"), cls);

        // اگر Basic بود و کاربر/رمز دادی:
        if (s.getAuthType()==SecuritySettings.AuthType.BASIC) {
            writeUsersConfigBasic(p, root, pkgBase, s);
        }
        // اگر JWT/Bearer/OAuth2 داری، این‌جا فایل‌های لازم را تولید کن (TODO)
    }

    public void writeUsersConfigBasic(Project p, Path root, String pkgBase, SecuritySettings s) throws IOException {
        Path resources = root.resolve("src/main/resources");
        String u = nvl(s.getBasicUsername(),"admin");
        String pw = nvl(s.getBasicPassword(),"admin123");
        // Spring Boot 3: in-memory user از طریق properties:
        String yml = "\n" +
                "spring:\n" +
                "  security:\n" +
                "    user:\n" +
                "      name: " + u + "\n" +
                "      password: " + pw + "\n";
        // append به application.yml
        Path app = resources.resolve("application.yml");
        String old = Files.exists(app) ? Files.readString(app) : "";
        write(app, old + "\n" + yml);
    }

    public void writeSwaggerArtifacts(Project p, Path root, String pkgBase) throws IOException {
        var sw = p.getSwagger();
        if (sw==null || !Boolean.TRUE.equals(sw.isEnabled())) return;
        // معمولاً springdoc بدون کانفیگ هم کفایت می‌کند.
        // اگر خواستی یک کلاس کانفیگ سبک اضافه کن:
        Path pkgDir = root.resolve("src/main/java").resolve(pkgBase.replace('.','/'));
        Path configDir = pkgDir.resolve("config");
        Files.createDirectories(configDir);
        String cls = "package " + pkgBase + ".config;\n\n" +
                "import org.springframework.context.annotation.Configuration;\n" +
                "@Configuration\n" +
                "public class OpenApiConfig {}\n";
        write(configDir.resolve("OpenApiConfig.java"), cls);
    }

    /* ======================= END ======================= */


    public void writePomWithDb2AndSpringdoc(Path root, com.company.appmaker.model.Project p) throws IOException {
        String javaVer = (p.getJavaVersion()==null || p.getJavaVersion().isBlank()) ? "17" : p.getJavaVersion().trim();
        String artifact = (p.getProjectName()==null?"app":p.getProjectName().trim().toLowerCase()).replaceAll("[^a-z0-9-]","-");
        write(root.resolve("pom.xml"), pomXml(sanitizeGroupId(p.getCompanyName()),artifact,javaVer));
    }


    /* ---------- application.yml / application-dev.yml ---------- */

    public String applicationYml(String artifact) {
        // فایل application.yml عمومی که پروفایل پیش‌فرض را مشخص می‌کند
        return """
        spring:
          application:
            name: %s
          profiles:
            active: dev
        management:
          endpoints:
            web:
              exposure:
                include: health,info,metrics
        """.formatted(escapeXml(artifact));
    }

    public String applicationDevYml() {
        return """
        spring:
          datasource:
            url: jdbc:h2:mem:devdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
            username: sa
            password:
            driver-class-name: org.h2.Driver
          jpa:
            hibernate:
              ddl-auto: update
            show-sql: true
        logging:
          level:
            root: INFO
            org.springframework: INFO
            com.company: DEBUG
        """;
    }

    public String applicationProdYml() {
        // نمونه برای prod: DB2 (jdbc) مثال
        return """
        spring:
          datasource:
            url: jdbc:db2://db2-host:50000/DBNAME
            username: dbuser
            password: dbpass
            driver-class-name: com.ibm.db2.jcc.DB2Driver
          jpa:
            hibernate:
              ddl-auto: validate
            show-sql: false

        server:
          port: 8080

        logging:
          level:
            root: INFO
        """;
    }

    /* ---------- Logback (logback-spring.xml) ---------- */

    public String logbackXml() {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <configuration scan="true">
          <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss} %-5level [%thread] %logger{36} - %msg%n"/>

          <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
              <pattern>${LOG_PATTERN}</pattern>
            </encoder>
          </appender>

          <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/application.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
              <fileNamePattern>logs/application.%d{yyyy-MM-dd}.log</fileNamePattern>
              <maxHistory>30</maxHistory>
            </rollingPolicy>
            <encoder>
              <pattern>${LOG_PATTERN}</pattern>
            </encoder>
          </appender>

          <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="FILE"/>
          </root>

          <!-- package overrides -->
          <logger name="org.springframework" level="INFO"/>
          <logger name="com.company" level="DEBUG"/>
        </configuration>
        """;
    }

    /* ---------- AppConfig.java (global config: DataSource, MessageSource, Locale, Jackson) ---------- */

    public String appConfigJava(String pkgBase) {
        return """
        package %s.config;

        import org.springframework.context.MessageSource;
        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;
        import org.springframework.context.support.ReloadableResourceBundleMessageSource;
        import org.springframework.web.servlet.LocaleResolver;
        import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
        import org.springframework.boot.jdbc.DataSourceBuilder;
        import javax.sql.DataSource;
        import java.util.Locale;

        @Configuration
        public class AppConfig {

            @Bean
            public DataSource dataSource() {
                // Default: H2 for dev; in prod user should override via application-prod.yml
                return DataSourceBuilder.create()
                        .driverClassName("org.h2.Driver")
                        .url("jdbc:h2:mem:devdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                        .username("sa")
                        .password("")
                        .build();
            }

            @Bean
            public MessageSource messageSource() {
                ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
                ms.setBasename("classpath:messages");
                ms.setDefaultEncoding("UTF-8");
                ms.setFallbackToSystemLocale(false);
                ms.setCacheSeconds(3600);
                return ms;
            }

            @Bean
            public LocaleResolver localeResolver() {
                AcceptHeaderLocaleResolver lr = new AcceptHeaderLocaleResolver();
                lr.setDefaultLocale(new Locale(\"fa\"));
                return lr;
            }

            // اگر به ObjectMapper / Jackson خاص نیاز داری، می‌توانی اینجا اضافه کنی
        }
        """.formatted(pkgBase);
    }

    /* ---------- Exception handling: ApiError + GlobalExceptionHandler ---------- */

    public String apiErrorJava(String pkgBase) {
        return """
        package %s.exception;

        import java.time.OffsetDateTime;
        import java.util.List;

        public class ApiError {
            private int status;
            private String error;
            private String message;
            private OffsetDateTime timestamp;
            private List<String> details;

            public ApiError() {}

            public ApiError(int status, String error, String message) {
                this.status = status;
                this.error = error;
                this.message = message;
                this.timestamp = OffsetDateTime.now();
            }

            // getters/setters
            public int getStatus() { return status; }
            public void setStatus(int status) { this.status = status; }
            public String getError() { return error; }
            public void setError(String error) { this.error = error; }
            public String getMessage() { return message; }
            public void setMessage(String message) { this.message = message; }
            public OffsetDateTime getTimestamp() { return timestamp; }
            public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
            public List<String> getDetails() { return details; }
            public void setDetails(List<String> details) { this.details = details; }
        }
        """.formatted(pkgBase);
    }

    public String globalExceptionHandlerJava(String pkgBase) {
        return """
        package %s.exception;

        import org.springframework.http.HttpHeaders;
        import org.springframework.http.HttpStatus;
        import org.springframework.http.ResponseEntity;
        import org.springframework.web.bind.annotation.ControllerAdvice;
        import org.springframework.web.bind.annotation.ExceptionHandler;
        import org.springframework.web.context.request.WebRequest;
        import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
        import org.springframework.web.bind.MethodArgumentNotValidException;

        import java.util.stream.Collectors;

        @ControllerAdvice
        public class GlobalExceptionHandler {

            @ExceptionHandler(Exception.class)
            public ResponseEntity<Object> handleAll(Exception ex, WebRequest request) {
                ApiError err = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        \"InternalServerError\", ex.getMessage());
                return new ResponseEntity<>(err, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }

            @ExceptionHandler(MethodArgumentNotValidException.class)
            public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
                ApiError err = new ApiError(HttpStatus.BAD_REQUEST.value(), \"ValidationError\", \"Validation failed\");
                err.setDetails(ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fe.getField() + \": \" + fe.getDefaultMessage()).collect(Collectors.toList()));
                return new ResponseEntity<>(err, HttpStatus.BAD_REQUEST);
            }

            @ExceptionHandler(MethodArgumentTypeMismatchException.class)
            public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
                ApiError err = new ApiError(HttpStatus.BAD_REQUEST.value(), \"TypeMismatch\", ex.getMessage());
                return new ResponseEntity<>(err, HttpStatus.BAD_REQUEST);
            }
        }
        """.formatted(pkgBase);
    }

    /* ---------- constants.properties + example messages (i18n) ---------- */

    public String constantsProperties() {
        return """
        # Application constants
        app.name=Generated App
        app.version=0.0.1-SNAPSHOT
        app.company=YourCompany
        """;
    }

    public String messagesDefault() {
        return """
        hello=سلام
        login.username=نام کاربری
        login.password=کلمه عبور
        login.submit=ورود
        error.404=یافت نشد
        """;
    }

    public String messagesEn() {
        return """
        hello=Hello
        login.username=Username
        login.password=Password
        login.submit=Sign in
        error.404=Not found
        """;
    }

    /* ---------- Security skeleton: SecurityConfig.java + UserDetailsService placeholder ---------- */

    public String securityConfigJava(String pkgBase) {
        return """
        package %s.config;

        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;
        import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
        import org.springframework.security.config.annotation.web.builders.HttpSecurity;
        import org.springframework.security.web.SecurityFilterChain;
        import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
        import org.springframework.security.crypto.password.PasswordEncoder;
        import org.springframework.security.provisioning.JdbcUserDetailsManager;
        import javax.sql.DataSource;

        @Configuration
        @EnableMethodSecurity
        public class SecurityConfig {

            @Bean
            public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                    .csrf().disable()
                    .authorizeHttpRequests(authz -> authz
                        .requestMatchers(\"/swagger-ui/**\", \"/v3/api-docs/**\", \"/actuator/**\").permitAll()
                        .anyRequest().authenticated()
                    )
                    .httpBasic();

                return http.build();
            }

            @Bean
            public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
            }

            @Bean
            public JdbcUserDetailsManager users(DataSource ds) {
                JdbcUserDetailsManager mgr = new JdbcUserDetailsManager(ds);
                // در prod، می‌توانید queryها را تنظیم کنید یا UserRepo خود را پیاده‌سازی کنید
                return mgr;
            }
        }
        """.formatted(pkgBase);
    }

    /* ---------- Helper: escapeXml (for safe artifact names in yml/xml) ---------- */
    public String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<","&lt;").replace(">", "&gt;");
    }



    public String pomXml(String groupId, String artifactId, String javaVersion) {
        return """
        <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
          <modelVersion>4.0.0</modelVersion>

          <parent>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-parent</artifactId>
            <version>3.3.4</version>
            <relativePath/> <!-- lookup parent from repository -->
          </parent>

          <groupId>%s</groupId>
          <artifactId>%s</artifactId>
          <version>0.0.1-SNAPSHOT</version>
          <name>%s</name>
          <description>Generated by AppMaker</description>

          <properties>
            <java.version>%s</java.version>
            <mapstruct.version>1.5.5.Final</mapstruct.version>
            <springdoc.version>2.6.0</springdoc.version>
            <jjwt.version>0.11.5</jjwt.version>
          </properties>

          <dependencies>
            <!-- Core starters -->
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-web</artifactId>
            </dependency>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-validation</artifactId>
            </dependency>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-data-jpa</artifactId>
            </dependency>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-security</artifactId>
            </dependency>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-actuator</artifactId>
            </dependency>

            <!-- Swagger / OpenAPI (springdoc) -->
            <dependency>
              <groupId>org.springdoc</groupId>
              <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
              <version>${springdoc.version}</version>
            </dependency>

            <!-- Lombok (compile-only + annotation processor) -->
            <dependency>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
              <optional>true</optional>
            </dependency>

            <!-- MapStruct -->
            <dependency>
              <groupId>org.mapstruct</groupId>
              <artifactId>mapstruct</artifactId>
              <version>${mapstruct.version}</version>
            </dependency>
            <dependency>
              <groupId>org.mapstruct</groupId>
              <artifactId>mapstruct-processor</artifactId>
              <version>${mapstruct.version}</version>
              <scope>provided</scope>
            </dependency>

            <!-- JWT (اختیاری برای Security/JWT) -->
            <dependency>
              <groupId>io.jsonwebtoken</groupId>
              <artifactId>jjwt-api</artifactId>
              <version>${jjwt.version}</version>
            </dependency>
            <dependency>
              <groupId>io.jsonwebtoken</groupId>
              <artifactId>jjwt-impl</artifactId>
              <version>${jjwt.version}</version>
              <scope>runtime</scope>
            </dependency>
            <dependency>
              <groupId>io.jsonwebtoken</groupId>
              <artifactId>jjwt-jackson</artifactId>
              <version>${jjwt.version}</version>
              <scope>runtime</scope>
            </dependency>

            <!-- DBs -->
            <!-- H2 برای dev/test -->
            <dependency>
              <groupId>com.h2database</groupId>
              <artifactId>h2</artifactId>
              <scope>runtime</scope>
            </dependency>
            <!-- IBM DB2 JDBC -->
            <dependency>
              <groupId>com.ibm.db2</groupId>
              <artifactId>jcc</artifactId>
              <version>11.5.9.0</version>
              <scope>runtime</scope>
            </dependency>

            <!-- تست‌ها -->
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-test</artifactId>
              <scope>test</scope>
            </dependency>
          </dependencies>

          <build>
            <plugins>
              <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
              </plugin>

              <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                  <!-- برای جاوا 8 از "source/target" استفاده کن؛ از 11 به بالا "release" هم قابل استفاده است -->
                  <source>${java.version}</source>
                  <target>${java.version}</target>
                  <annotationProcessorPaths>
                    <path>
                      <groupId>org.projectlombok</groupId>
                      <artifactId>lombok</artifactId>
                      <version>${lombok.version}</version>
                    </path>
                    <path>
                      <groupId>org.mapstruct</groupId>
                      <artifactId>mapstruct-processor</artifactId>
                      <version>${mapstruct.version}</version>
                    </path>
                  </annotationProcessorPaths>
                </configuration>
              </plugin>
            </plugins>
          </build>

          <profiles>
            <!-- مثال: استفاده از H2 در dev/test -->
            <profile>
              <id>dev</id>
              <activation>
                <activeByDefault>true</activeByDefault>
              </activation>
              <properties>
                <spring.profiles.active>dev</spring.profiles.active>
              </properties>
            </profile>
            <profile>
              <id>test</id>
              <properties>
                <spring.profiles.active>test</spring.profiles.active>
              </properties>
            </profile>
            <profile>
              <id>prod</id>
              <properties>
                <spring.profiles.active>prod</spring.profiles.active>
              </properties>
            </profile>
          </profiles>
        </project>
        """.formatted(groupId, artifactId, artifactId, javaVersion);
    }


    // داخل ProjectScaffolder
    public String defaultPomFor(String groupId, String artifactId, String javaVersion) {
        String jv = (javaVersion == null || javaVersion.isBlank()) ? "17" : javaVersion.trim();
        // نسخه‌ها را در صورت نیاز از config بخوانید؛ اینجا ثابت‌های معقول گذاشته‌ایم
        String bootVersion     = "3.3.4";
        String springdocVer    = "2.6.0";
        String mapstructVer    = "1.5.5.Final";

        return ""
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 "
                + "https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "\n"
                + "  <parent>\n"
                + "    <groupId>org.springframework.boot</groupId>\n"
                + "    <artifactId>spring-boot-starter-parent</artifactId>\n"
                + "    <version>" + bootVersion + "</version>\n"
                + "    <relativePath/>\n"
                + "  </parent>\n"
                + "\n"
                + "  <groupId>" + groupId + "</groupId>\n"
                + "  <artifactId>" + artifactId + "</artifactId>\n"
                + "  <version>0.0.1-SNAPSHOT</version>\n"
                + "  <name>" + artifactId + "</name>\n"
                + "  <description>Generated by AppMaker</description>\n"
                + "\n"
                + "  <properties>\n"
                + "    <java.version>" + jv + "</java.version>\n"
                + "    <springdoc.version>" + springdocVer + "</springdoc.version>\n"
                + "    <mapstruct.version>" + mapstructVer + "</mapstruct.version>\n"
                + "  </properties>\n"
                + "\n"
                + "  <dependencies>\n"
                + "    <!-- Web/API -->\n"
                + "    <dependency>\n"
                + "      <groupId>org.springframework.boot</groupId>\n"
                + "      <artifactId>spring-boot-starter-web</artifactId>\n"
                + "    </dependency>\n"
                + "\n"
                + "    <!-- Validation -->\n"
                + "    <dependency>\n"
                + "      <groupId>org.springframework.boot</groupId>\n"
                + "      <artifactId>spring-boot-starter-validation</artifactId>\n"
                + "    </dependency>\n"
                + "\n"
                + "    <!-- Security (اختیاری ولی معمولاً نیاز داریم) -->\n"
                + "    <dependency>\n"
                + "      <groupId>org.springframework.boot</groupId>\n"
                + "      <artifactId>spring-boot-starter-security</artifactId>\n"
                + "    </dependency>\n"
                + "\n"
                + "    <!-- OpenAPI/Swagger UI -->\n"
                + "    <dependency>\n"
                + "      <groupId>org.springdoc</groupId>\n"
                + "      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>\n"
                + "      <version>${springdoc.version}</version>\n"
                + "    </dependency>\n"
                + "\n"
                + "    <!-- MapStruct (برای mapperهای DTO/Domain) -->\n"
                + "    <dependency>\n"
                + "      <groupId>org.mapstruct</groupId>\n"
                + "      <artifactId>mapstruct</artifactId>\n"
                + "      <version>${mapstruct.version}</version>\n"
                + "    </dependency>\n"
                + "    <dependency>\n"
                + "      <groupId>org.mapstruct</groupId>\n"
                + "      <artifactId>mapstruct-processor</artifactId>\n"
                + "      <version>${mapstruct.version}</version>\n"
                + "      <scope>provided</scope>\n"
                + "    </dependency>\n"
                + "\n"
                + "    <!-- Lombok (اختیاری) -->\n"
                + "    <dependency>\n"
                + "      <groupId>org.projectlombok</groupId>\n"
                + "      <artifactId>lombok</artifactId>\n"
                + "      <optional>true</optional>\n"
                + "    </dependency>\n"
                + "\n"
                + "    <!-- Test -->\n"
                + "    <dependency>\n"
                + "      <groupId>org.springframework.boot</groupId>\n"
                + "      <artifactId>spring-boot-starter-test</artifactId>\n"
                + "      <scope>test</scope>\n"
                + "    </dependency>\n"
                + "  </dependencies>\n"
                + "\n"
                + "  <build>\n"
                + "    <plugins>\n"
                + "      <plugin>\n"
                + "        <groupId>org.springframework.boot</groupId>\n"
                + "        <artifactId>spring-boot-maven-plugin</artifactId>\n"
                + "      </plugin>\n"
                + "      <plugin>\n"
                + "        <groupId>org.apache.maven.plugins</groupId>\n"
                + "        <artifactId>maven-compiler-plugin</artifactId>\n"
                + "        <configuration>\n"
                + "          <release>${java.version}</release>\n"
                + "          <annotationProcessorPaths>\n"
                + "            <path>\n"
                + "              <groupId>org.mapstruct</groupId>\n"
                + "              <artifactId>mapstruct-processor</artifactId>\n"
                + "              <version>${mapstruct.version}</version>\n"
                + "            </path>\n"
                + "            <path>\n"
                + "              <groupId>org.projectlombok</groupId>\n"
                + "              <artifactId>lombok</artifactId>\n"
                + "              <version>1.18.32</version>\n"
                + "            </path>\n"
                + "          </annotationProcessorPaths>\n"
                + "        </configuration>\n"
                + "      </plugin>\n"
                + "    </plugins>\n"
                + "  </build>\n"
                + "</project>\n";
    }


//    public static String toDependenciesXml(List<String> coords) {
//        StringBuilder sb = new StringBuilder();
//        for (String c : coords) {
//            // شکل‌های قابل‌قبول:
//            // groupId:artifactId
//            // groupId:artifactId:version
//            String[] parts = c.split(":");
//            if (parts.length < 2) continue;
//            String g = parts[0], a = parts[1];
//            String v = (parts.length >= 3) ? parts[2] : null;
//
//            sb.append("    <dependency>\n")
//                    .append("      <groupId>").append(g).append("</groupId>\n")
//                    .append("      <artifactId>").append(a).append("</artifactId>\n");
//            if (v != null && !v.isBlank()) {
//                sb.append("      <version>").append(v).append("</version>\n");
//            }
//            sb.append("    </dependency>\n");
//        }
//        // تمیز: داخل <dependencies> در خود template قرار می‌گیرد
//        return sb.toString();
//    }


    public static String toDependenciesXml(List<String> coords) {
        StringBuilder sb = new StringBuilder();
        for (String c : coords) {
            if (c == null || c.isBlank()) continue;
            String[] parts = c.split(":");
            if (parts.length < 2) continue;
            String g = parts[0].trim();
            String a = parts[1].trim();
            String v = (parts.length >= 3) ? parts[2].trim() : null;

            sb.append("    <dependency>\n")
                    .append("      <groupId>").append(g).append("</groupId>\n")
                    .append("      <artifactId>").append(a).append("</artifactId>\n");
            if (v != null && !v.isBlank()) {
                sb.append("      <version>").append(v).append("</version>\n");
            }
            sb.append("    </dependency>\n");
        }
        return sb.toString();
    }





}
