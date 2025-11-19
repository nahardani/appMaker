package com.company.appmaker.config;

import com.company.appmaker.model.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.company.appmaker.service.TemplateService;
import org.springframework.stereotype.Service;


@Service
public class ProjectScaffolder {

    private final TemplateService templateService;

    public ProjectScaffolder(TemplateService templateService) {
        this.templateService = templateService;
    }

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

    public void scaffoldToDirectory(Project p, Path root) throws IOException {
        Objects.requireNonNull(p, "project is null");
        Objects.requireNonNull(root, "root is null");

        // --- Settings & metadata
        var ms = (p.getMs() != null) ? p.getMs() : new MicroserviceSettings();
        String group = (p.getCompanyName() == null || p.getCompanyName().isBlank())
                ? "com.example" : p.getCompanyName().trim();
        String artifact = (p.getProjectName() == null || p.getProjectName().isBlank())
                ? "app" : p.getProjectName().trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        String basePkg = (p.getMs() != null && p.getMs().getBasePackage() != null && !p.getMs().getBasePackage().isBlank())
                ? p.getMs().getBasePackage().trim()
                : group + "." + sanitizeIdentifier(p.getProjectName()).toLowerCase(Locale.ROOT);
        String basePath = (ms.getBasePath() == null || ms.getBasePath().isBlank())
                ? "/api/" + artifact.replaceAll("-", "") : ms.getBasePath().trim();
        String jv = (p.getJavaVersion() == null || p.getJavaVersion().isBlank())
                ? "17" : p.getJavaVersion().trim();
        String jvNorm = normalizeJavaVer(jv); // "8"|"11"|"17"|"21"
        String apiVer = (ms.getApiVersion() == null || ms.getApiVersion().isBlank()) ? "v1" : ms.getApiVersion().trim();
        String service = (ms.getServiceName() == null || ms.getServiceName().isBlank()) ? artifact : ms.getServiceName().trim();

        // --- Folders
        Path srcMain = root.resolve("src/main/java");
        Path resources = root.resolve("src/main/resources");
        Files.createDirectories(srcMain);
        Files.createDirectories(resources);

        // --- Base package dir
        Path pkgDir = srcMain.resolve(basePkg.replace('.', '/'));
        Files.createDirectories(pkgDir);

        LinkedHashSet<String> mandatory = new LinkedHashSet<>(List.of(
                "controller",
                "service",
                "repository",
                "dto",
                "config",
                "exception",
                "domain",
                "mapper",
                "common",
                "client",
                "security",
                "configprops",
                "i18n"
        ));

        if (p.getPackages() != null) {
            for (String s : p.getPackages()) {
                if (s == null) continue;
                String t = s.trim();
                if (!t.isEmpty()) mandatory.add(t);
            }
        }

        ensurePackages(pkgDir, mandatory);


        // Standard subpackages
        Path ctrlDir = ensureDir(pkgDir.resolve("controller"));
        Path svcDir = ensureDir(pkgDir.resolve("service"));
        Path repoDir = ensureDir(pkgDir.resolve("repository"));
        Path dtoDir = ensureDir(pkgDir.resolve("dto"));
        Path cfgDir = ensureDir(pkgDir.resolve("config"));
        Path excDir = ensureDir(pkgDir.resolve("exception"));

        // --- POM (dependencies from Service Profile)
        List<String> coords = new ArrayList<>();
        coords.add("org.springframework.boot:spring-boot-starter-web");
        if (Boolean.TRUE.equals(ms.isUseMongo()))
            coords.add("org.springframework.boot:spring-boot-starter-data-mongodb");
        if (Boolean.TRUE.equals(ms.isEnableActuator()))
            coords.add("org.springframework.boot:spring-boot-starter-actuator");
        if (Boolean.TRUE.equals(ms.isEnableValidation()))
            coords.add("org.springframework.boot:spring-boot-starter-validation");
        if (Boolean.TRUE.equals(ms.isEnableMetrics())) coords.add("io.micrometer:micrometer-registry-prometheus");
        if (Boolean.TRUE.equals(ms.isEnableOpenApi()))
            coords.add("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0");

        templateService.writeFromTpl(
                root.resolve("pom.xml"),
                "pom", "pom", jvNorm, null, p,
                Map.of(
                        "groupId", group,
                        "artifactId", artifact,
                        "java.version", (ms.getJavaVersion() != null && !ms.getJavaVersion().isBlank()) ? ms.getJavaVersion() : jv,
                        "dependencies", toDependenciesXml(coords)
                )
        );

        // --- application*.yml
        templateService.writeFromTpl(
                resources.resolve("application.yml"),
                "profiles", "application.base", "any", null, p,
                Map.of("serviceName", service)
        );
        templateService.writeFromTpl(
                resources.resolve("application-dev.yml"),
                "profiles", "application-dev", "any", null, p,
                Map.of() // در صورت نیاز متغیر اضافه کن
        );
        templateService.writeFromTpl(
                resources.resolve("application-test.yml"),
                "profiles", "application-test", "any", null, p,
                Map.of()
        );
        templateService.writeFromTpl(
                resources.resolve("application-prod.yml"),
                "profiles", "application-prod", "any", null, p,
                Map.of()
        );

        // --- logging
        templateService.writeFromTpl(
                resources.resolve("logback-spring.xml"),
                "logging", "logback-spring", "any", null, p,
                Map.of()
        );

        // --- config/exception
        templateService.writeFromTpl(
                cfgDir.resolve("WebConfig.java"),
                "config", "WebConfig", "any", null, p,
                Map.of("basePackage", basePkg)
        );

        if (Boolean.TRUE.equals(ms.isEnableOpenApi())) {
            templateService.writeFromTpl(
                    cfgDir.resolve("OpenApiConfig.java"),
                    "config", "OpenApiConfig", "any", null, p,
                    Map.of(
                            "basePackage", basePkg,
                            "serviceName", service,
                            "apiVersion", apiVer
                    )
            );
        }

        if (Boolean.TRUE.equals(ms.isEnableSecurityBasic())) {
            templateService.writeFromTpl(
                    cfgDir.resolve("SecurityConfig.java"),
                    "config", "SecurityConfig", "any", null, p,
                    Map.of("basePackage", basePkg)
            );
        }

        templateService.writeFromTpl(
                excDir.resolve("GlobalExceptionHandler.java"),
                "exception", "GlobalExceptionHandler", "any", null, p,
                Map.of("basePackage", basePkg)
        );

        // --- App.java
        templateService.writeFromTpl(
                pkgDir.resolve("App.java"),
                "app", "App", "any", null, p,
                Map.of("basePackage", basePkg)
        );

        // --- Optional docker files
        if (Boolean.TRUE.equals(ms.isAddDockerfile())) {
            templateService.writeFromTpl(
                    root.resolve("Dockerfile"),
                    "docker", "Dockerfile", "any", null, p,
                    Map.of("java.version", (ms.getJavaVersion() == null || ms.getJavaVersion().isBlank()) ? jv : ms.getJavaVersion())
            );
        }
        if (Boolean.TRUE.equals(ms.isAddCompose())) {
            templateService.writeFromTpl(
                    root.resolve("docker-compose.yml"),
                    "docker", "docker-compose-mongo", "any", null, p,
                    Map.of()
            );
        }

        // --- I18n (اختیاری؛ اگر مکانیزم‌ات داری، نگه دار؛ اینجا فقط مطمئن می‌شویم فایل پایه خالی موجود است)
        Path messages = resources.resolve("messages.properties");
        if (!Files.exists(messages)) Files.writeString(messages, "", StandardCharsets.UTF_8);


        var allGen = new ArrayList<Project.GeneratedFile>();


        // به‌جای حلقه‌ی قبلی روی p.getGeneratedFiles():
        if (p.getControllers() != null) {
            for (var c : p.getControllers()) {
                if (c == null || c.getEndpoints() == null) continue;
                for (var e : c.getEndpoints()) {
                    if (e == null || e.getAiFiles() == null) continue;
                    for (var gf : e.getAiFiles()) {
                        allGen.addAll(c.getAiFiles());
                    }
                }
            }
        }


        if (p.getGeneratedFiles() != null) {
            allGen.addAll(p.getGeneratedFiles());
        }

        for (var gf : allGen) {
            if (gf == null) continue;
            String path = gf.getPath();
            String content = gf.getContent();
            if (path == null || path.isBlank() || content == null) continue;
            if (!path.endsWith(".java")) continue; // فقط کلاس‌های جاوا

            String declaredPkg = extractPackage(content);
            Path target;
            if (declaredPkg != null && !declaredPkg.isBlank()) {
                Path targetDir = srcMain.resolve(declaredPkg.replace('.', '/'));
                Files.createDirectories(targetDir);
                target = targetDir.resolve(fileName(path));
            } else {
                String name = fileName(path);
                Path sub = guessSubPackageDir(name, ctrlDir, svcDir, repoDir, dtoDir);
                Files.createDirectories(sub);
                target = sub.resolve(name);
                content = ensurePackageHeader(content, packageForDir(pkgDir, sub));
            }

            Files.writeString(target, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

    }

    private Path ensureDir(Path p) throws IOException {
        Files.createDirectories(p);
        return p;
    }

    private String toDependenciesXml(List<String> coords) {
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

    private static String normalizeJavaVer(String v) {
        if (v == null) return "17";
        String s = v.trim();
        if (s.startsWith("1.")) s = s.substring(2); // 1.8 → 8
        return switch (s) {
            case "8", "11", "17", "21" -> s;
            default -> "17";
        };
    }

    private String extractPackage(String content) {
        Matcher m = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;\\s*$", Pattern.MULTILINE).matcher(content);
        return m.find() ? m.group(1) : null;
    }

    private String fileName(String path) {
        int idx = path.lastIndexOf('/');
        if (idx < 0) idx = path.lastIndexOf('\\');
        return (idx >= 0) ? path.substring(idx + 1) : path;
    }

    private Path guessSubPackageDir(String fileName, Path ctrlDir, Path svcDir, Path repoDir, Path dtoDir) {
        String n = fileName.toLowerCase(Locale.ROOT);
        if (n.endsWith("controller.java")) return ctrlDir;
        if (n.endsWith("service.java")) return svcDir;
        if (n.endsWith("repository.java")) return repoDir;
        if (n.endsWith("repo.java")) return repoDir;
        if (n.endsWith("dto.java") || n.contains("dto")) return dtoDir;
        // پیش‌فرض: controller
        return ctrlDir;
    }

    private String packageForDir(Path pkgRoot, Path subDir) {
        // پیدا کردن ریشه src/main/java
        Path javaRoot = pkgRoot;
        while (javaRoot != null && !javaRoot.endsWith("java")) {
            javaRoot = javaRoot.getParent();
        }
        if (javaRoot == null) {
            // fallback: base package از pkgRoot استنباط شود
            String base = pkgRoot.toString().replace('\\', '/');
            int idx = base.indexOf("/src/main/java/");
            if (idx >= 0) {
                String tail = base.substring(idx + "/src/main/java/".length()).replace('/', '.').replace('\\', '.');
                if (!tail.isBlank()) {
                    String rel = subDir.toString().replace('\\', '/').substring(base.indexOf("/src/main/java/") + "/src/main/java/".length());
                    String relPkg = rel.replace('/', '.').replace('\\', '.');
                    return relPkg;
                }
            }
            return null;
        }
        // basePkg = بخش بعد از src/main/java
        String basePkg = pkgRoot.toString().replace('\\', '/');
        int cut = basePkg.indexOf("/src/main/java/");
        if (cut >= 0) basePkg = basePkg.substring(cut + "/src/main/java/".length());
        basePkg = basePkg.replace('/', '.');
        String rel = subDir.toString().replace('\\', '/');
        cut = rel.indexOf("/src/main/java/");
        if (cut >= 0) rel = rel.substring(cut + "/src/main/java/".length());
        rel = rel.replace('/', '.');
        return rel;
    }

    private String ensurePackageHeader(String content, String pkg) {
        if (pkg == null || pkg.isBlank()) return content;
        // اگر خودش package دارد، دست نزن
        if (extractPackage(content) != null) return content;
        return "package " + pkg + ";\n\n" + content;
    }

    private void ensurePackages(Path pkgDir, java.util.Collection<String> packages) throws IOException {
        if (packages == null) return;
        for (String raw : packages) {
            if (raw == null) continue;
            String name = raw.trim();
            if (name.isEmpty()) continue;

            // پوشه را بساز
            Path dir = pkgDir.resolve(name.replace('.', '/'));
            Files.createDirectories(dir);

            // package-info.java حداقلی
            Path pi = dir.resolve("package-info.java");
            if (!Files.exists(pi)) {
                // pkgDir شبیه src/main/java/com/x/y است → نام کامل پکیج پایه را دربیاوریم
                String basePkg = pkgDir.toString().replace('\\', '/');
                int cut = basePkg.indexOf("/src/main/java/");
                String rootPkg = (cut >= 0)
                        ? basePkg.substring(cut + "/src/main/java/".length()).replace('/', '.')
                        : ""; // fallback

                String full = rootPkg.isBlank() ? name : (rootPkg + "." + name);
                Files.writeString(pi, "package " + full + ";\n");
            }
        }
    }

    public String artifactId(Project p) {
        String name = sanitizeIdentifier(nvl(p.getProjectName(), "app"));
        return name.toLowerCase(Locale.ROOT);
    }

    public String sanitizeIdentifier(String s) {
        if (s == null || s.isBlank()) return "app";
        String x = s.replaceAll("[^A-Za-z0-9_]+", "_");
        if (!Character.isJavaIdentifierStart(x.charAt(0))) x = "_" + x;
        return x;
    }

    public String nvl(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }


}
