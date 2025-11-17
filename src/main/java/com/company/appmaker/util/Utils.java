package com.company.appmaker.util;

import com.company.appmaker.ai.dto.CodeFile;
import com.company.appmaker.model.Project;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unified utilities for AI merge & scaffolding.
 * Merged from prior Utils and AiMergeUtils with duplication removed and logic hardened.
 */
public final class Utils {

    private enum Kind { CONTROLLER, SERVICE_INTERFACE, SERVICE_IMPL, DTO, ENTITY, REPOSITORY, CLIENT, OTHER }
    private record Detected(Kind kind, String className) {}
    private static final Set<String> SUPPORTED_ARTIFACT_TYPES = Set.of(
            "controller-method", "service-method", "service-impl-method"
    );
    public record Result(List<CodeFile> files) {}

    private Utils() {}

    public static String controllerStart() { return "// <AI-ENDPOINTS-START>"; }
    public static String controllerEnd()   { return "// <AI-ENDPOINTS-END>"; }
    public static String serviceRegionStart() { return "// <AI-SERVICE-REGION>"; }
    public static String serviceRegionEnd()   { return "// </AI-SERVICE-REGION>"; }
    public static String serviceStart()    { return "// <AI-SERVICE-REGION>"; }
    public static String serviceEnd()      { return "// </AI-SERVICE-REGION>"; }
    public static String toUpperCamel(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String[] parts = raw.replace('_',' ').replace('-',' ').trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
        }
        if (sb.length() == 0) {
            sb.append(Character.toUpperCase(raw.charAt(0)));
            if (raw.length() > 1) sb.append(raw.substring(1));
        }
        return sb.toString();
    }
    public static String ensureControllerName(String name) {
        String base = toUpperCamel(stripSuffixIgnoreCase(name, "Controller"));
        return (base.isBlank() ? "Generated" : base) + "Controller";
    }
    public static String stripControllerSuffix(String ctrlSimple) {
        if (ctrlSimple == null) return "Generated";
        return ctrlSimple.endsWith("Controller")
                ? ctrlSimple.substring(0, ctrlSimple.length() - "Controller".length())
                : ctrlSimple;
    }
    public static String ensureServiceName(String feature) {
        String base = toUpperCamel(stripSuffixIgnoreCase(feature, "Service"));
        return (base.isBlank() ? "Generated" : base) + "Service";
    }
    public static String ensureServiceImplName(String featureOrService) {
        String base = toUpperCamel(stripSuffixIgnoreCase(featureOrService, "Service"));
        return (base.isBlank() ? "Generated" : base) + "ServiceImpl";
    }
    public static String camel(String s) {
        String uc = toUpperCamel(s);
        if (uc.isEmpty()) return uc;
        return Character.toLowerCase(uc.charAt(0)) + uc.substring(1);
    }
    public static String resolveBasePackage(Project p){
        var ms = p.getMs();
        if (ms!=null && ms.getBasePackage()!=null && !ms.getBasePackage().isBlank())
            return ms.getBasePackage().trim();
        String group = (p.getCompanyName()==null || p.getCompanyName().isBlank())
                ? "com.example" : p.getCompanyName().trim();
        String artifact = (p.getProjectName()==null || p.getProjectName().isBlank())
                ? "app" : p.getProjectName().trim().toLowerCase().replaceAll("[^a-z0-9]+","-");
        return group + "." + artifact.replace('-', '.');
    }
    public static String pkgToPath(String basePackage) {
        return basePackage.replace('.', '/');
    }
    public static String controllerPath(String basePackage, String controllerSimpleName) {
        return "src/main/java/" + pkgToPath(basePackage) + "/controller/" + controllerSimpleName + ".java";
    }
    public static String servicePath(String basePackage, String serviceSimpleName) {
        return "src/main/java/" + pkgToPath(basePackage) + "/service/" + serviceSimpleName + ".java";
    }
    public static String serviceImplPath(String basePackage, String serviceImplSimpleName) {
        return "src/main/java/" + pkgToPath(basePackage) + "/service/" + serviceImplSimpleName + ".java";
    }
    public static String normalizeAiArtifactType(String raw) {
        if (raw == null) return "";
        String r = raw.trim().toLowerCase(Locale.ROOT);
        return switch (r) {
            case "controller", "controller-method", "controller_method" -> "controller-method";
            case "service", "service-method", "service_method", "service-interface", "service_interface", "service-interface-method"
                    -> "service-method";
            case "service-impl", "service_impl", "service-impl-method", "service_impl_method", "service-implementation-method"
                    -> "service-impl-method";
            default -> r;
        };
    }
    public static boolean isTestFile(String path, String content) {
        String p = (path == null) ? "" : path.toLowerCase(Locale.ROOT);
        boolean looksPath = p.contains("/test/") || p.contains("\\test\\") || p.contains("src/test/");
        boolean looksContent = content != null && (
                content.contains("@SpringBootTest") || content.contains("org.junit.jupiter")
        );
        return looksPath || looksContent;
    }
    public static String normalizeTestPath(String inputPath, String pkgPath) {
        if (inputPath == null || inputPath.isBlank()) {
            return "src/test/java/" + pkgPath + "/UnknownTest.java";
        }
        if (inputPath.contains("src/test")) return inputPath;
        String fileName = inputPath.replace('\\','/');
        int idx = fileName.lastIndexOf('/');
        fileName = (idx >= 0) ? fileName.substring(idx + 1) : fileName;
        if (!fileName.endsWith(".java")) fileName = fileName + ".java";
        return "src/test/java/" + pkgPath + "/controller/" + fileName;
    }
    public static boolean isControllerFile(String path, String content, String normalizedCtrlSimple) {
        boolean pathOk = path != null && path.replace('\\','/').contains("/controller/");
        boolean annot  = content != null && content.contains("@RestController");
        boolean nameOk = content != null && containsClassNamed(content, normalizedCtrlSimple);
        return (pathOk && annot) || nameOk || (annot && content != null && content.contains("@RequestMapping"));
    }
    public static boolean isServiceFile(String path, String content, String svcSimple) {
        if (content == null) return false;
        final String normPath = path == null ? "" : path.replace('\\','/');
        boolean inServiceFolder   = normPath.contains("/service/");
        boolean hasInterfaceDecl  = containsInterfaceNamed(content, svcSimple);
        boolean declaresClass     = Pattern.compile("\\bclass\\b").matcher(content).find();
        boolean annotatedService  = content.contains("@Service");
        boolean regionLooksIface  = content.contains(serviceRegionStart()) && content.contains("interface ");

        // Accept when it's in /service/, declares the interface, and does NOT look like a class/impl.
        return inServiceFolder && ( (hasInterfaceDecl && !declaresClass && !annotatedService) || regionLooksIface );
    }
    public static boolean isServiceImplFile(String path, String content, String implSimple) {
        if (content == null) return false;
        final String normPath = path == null ? "" : path.replace('\\','/');
        boolean inServiceFolder  = normPath.contains("/service/");
        boolean declaresClass    = Pattern.compile("\\bclass\\b").matcher(content).find();
        boolean hasInterfaceDecl = Pattern.compile("\\binterface\\b").matcher(content).find();
        boolean hasImplClassDecl = containsClassNamed(content, implSimple);
        boolean annotatedService = content.contains("@Service");
        boolean implementsService = Pattern.compile("\\bimplements\\s+\\w*Service\\b").matcher(content).find();

        return inServiceFolder
                && declaresClass
                && !hasInterfaceDecl
                && (hasImplClassDecl || annotatedService || implementsService);
    }
    public static String ensureControllerClassName(String content, String normalizedCtrlSimple) {
        if (content == null || content.isBlank()) return content;
        if (containsClassNamed(content, normalizedCtrlSimple)) return content;
        Pattern cls = Pattern.compile("(public\\s+class\\s+)([A-Za-z_][A-Za-z0-9_]*)(\\s*\\{)");
        Matcher m = cls.matcher(content);
        if (m.find()) {
            return new StringBuilder(content)
                    .replace(m.start(2), m.end(2), normalizedCtrlSimple)
                    .toString();
        }
        return content;
    }
    public static String extractControllerMethodByNameOrMapping(String controllerSrc, String epName) {
        if (controllerSrc == null || controllerSrc.isBlank()) return null;
        String camel = camel(epName);
        String byName = extractMethodByName(controllerSrc, camel);
        if (byName != null && !byName.isBlank()) return byName;
        String byMapping = extractMethodByMapping(controllerSrc, epName);
        if (byMapping != null && !byMapping.isBlank()) return byMapping;
        return null;
    }
    public static String extractServiceMethodByName(String serviceSrc, String epName) {
        if (serviceSrc == null || serviceSrc.isBlank()) return null;
        String camel = camel(epName);
        return extractMethodByName(serviceSrc, camel);
    }
    public static String trimMethod(String code) {
        if (code == null) return "";
        String t = code.trim();
        t = t.replaceAll("^```[a-zA-Z]*\\s*", "");
        t = t.replaceAll("\\s*```\\s*$", "");
        return t;
    }
    public static String ensureOnlyMethod(String srcOrMethod, String epName) {
        String method = extractServiceMethodByName(srcOrMethod, epName);
        if (method != null && !method.isBlank()) return method;
        method = extractControllerMethodByNameOrMapping(srcOrMethod, epName);
        if (method != null && !method.isBlank()) return method;
        return srcOrMethod;
    }
    public static List<Project.GeneratedFile> collectCommittedAiFiles(Project p) {
        if (p == null || p.getControllers() == null || p.getControllers().isEmpty()) {
            return List.of();
        }

        final String basePackage = resolveBasePackage(p);
        final String basePath    = resolveBasePath(p);

        List<Project.GeneratedFile> out = new ArrayList<>();

        for (var ctrl : p.getControllers()) {
            if (ctrl == null) continue;

            String ctrlSimple = ensureControllerName(ctrl.getName());
            String feature    = stripControllerSuffix(ctrlSimple);
            String svcSimple  = ensureServiceName(feature);
            String implSimple = ensureServiceImplName(feature);

            String controllerPath  = controllerPath(basePackage, ctrlSimple);
            String servicePath     = servicePath(basePackage, svcSimple);
            String serviceImplPath = serviceImplPath(basePackage, implSimple);

            List<String> controllerMethods  = new ArrayList<>();
            List<String> serviceSignatures  = new ArrayList<>();
            List<String> serviceImplMethods = new ArrayList<>();

            if (ctrl.getEndpoints() != null) {
                for (var ep : ctrl.getEndpoints()) {
                    if (ep == null) continue;
                    if (ep.getAiArtifacts() != null) {
                        for (var a : ep.getAiArtifacts()) {
                            if (a == null) continue;
                            String content = safe(a.getContent());
                            if (content.isBlank()) continue;

                            String t = safe(a.getType()).trim();
                            switch (t) {
                                case "controller-method" -> controllerMethods.add(trimMethod(content));
                                case "service-method", "service-interface" -> serviceSignatures.add(trimSignature(content));
                                case "service-impl-method" -> serviceImplMethods.add(trimMethod(content));
                            }
                        }
                    }
                }
            }

            String controllerSkeleton = ControllerSkeletonFactory.create(
                    basePackage,
                    (ctrl.getBasePath() != null && !ctrl.getBasePath().isBlank()) ? ctrl.getBasePath().trim() : basePath,
                    ctrlSimple,
                    svcSimple
            );
            String serviceSkeleton  = ServiceSkeletonFactory.create(basePackage, svcSimple);
            String serviceImplSkel  = ServiceImplSkeletonFactory.create(basePackage, svcSimple, implSimple);

            String mergedController = mergeIntoRegion(controllerSkeleton, controllerStart(), controllerEnd(), controllerMethods);
            String mergedService    = mergeIntoRegion(serviceSkeleton,  serviceRegionStart(), serviceRegionEnd(), serviceSignatures);
            String mergedServiceImpl= mergeIntoRegion(serviceImplSkel,  serviceRegionStart(), serviceRegionEnd(), serviceImplMethods);

            out.add(new Project.GeneratedFile(controllerPath,  mergedController));
            out.add(new Project.GeneratedFile(servicePath,     mergedService));
            out.add(new Project.GeneratedFile(serviceImplPath, mergedServiceImpl));

            if (ctrl.getEndpoints() != null) {
                for (var ep : ctrl.getEndpoints()) {
                    if (ep == null || ep.getAiFiles() == null) continue;
                    for (var f : ep.getAiFiles()) {
                        if (f == null) continue;
                        var path    = f.getPath();
                        var content = f.getContent();
                        if (path == null || content == null) continue;
                        if (!path.endsWith(".java")) continue;

                        boolean isController = path.replace('\\','/').endsWith("/controller/" + ctrlSimple + ".java");
                        boolean isService    = path.replace('\\','/').endsWith("/service/" + svcSimple + ".java");
                        boolean isImpl       = path.replace('\\','/').endsWith("/service/" + implSimple + ".java");
                        if (isController || isService || isImpl) continue;

                        path = normalizeServicePaths(path, pkgToPath(basePackage), ctrlSimple, svcSimple, implSimple);
                        out.add(new Project.GeneratedFile(path, content));
                    }
                }
            }
        }

        return out;
    }
    public static String wrapControllerTagged(String methodSrc) {
        String body = methodSrc == null ? "" : methodSrc.trim();
        return controllerStart() + "\n" + body + "\n" + controllerEnd();
    }
    public static String wrapServiceTagged(String methodSrc) {
        String body = methodSrc == null ? "" : methodSrc.trim();
        return serviceStart() + "\n" + body + "\n" + serviceEnd();
    }
    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
    public static String normalizeControllerName(String name) {
        if (name == null || name.isBlank()) return "GeneratedController";
        String n = name.trim();
        return n.endsWith("Controller") ? n : (n + "Controller");
    }
    public static Result run(List<CodeFile> parsedFiles,
                             String raw,
                             String basePackage,
                             String featureName,
                             String basePath,
                             int javaVersion) {

        List<CodeFile> files = new ArrayList<>();
        if (parsedFiles != null) files.addAll(parsedFiles);

        // 1) اگر parser چیزی نداد، از raw استخراج کن
        if (files.isEmpty()) {
            files.addAll(extractFromRaw(raw));
        }

        // 2) فقط .java نگه دار
        files.removeIf(f -> f == null || f.path() == null || !f.path().endsWith(".java"));

        // 3) نگاشت مسیر + پکیج
        var normalized = normalizePathsAndPackages(files, basePackage, featureName);

        // 4) اگر خواستی rename کلاس/فایل را هم اینجا انجام بده (فعلاً pass-through)
        var finalList = sanitizeJava(normalized, basePackage);

        return new Result(finalList);
    }
    private static String resolveBasePath(Project p){
        var ms = p.getMs();
        if (ms!=null && ms.getBasePath()!=null && !ms.getBasePath().isBlank())
            return ms.getBasePath().trim();
        String artifact = (p.getProjectName()==null || p.getProjectName().isBlank())
                ? "app" : p.getProjectName().trim().toLowerCase().replaceAll("[^a-z0-9]+","-");
        return "/api/" + artifact.replace("-","");
    }
    private static String stripSuffixIgnoreCase(String s, String suffix) {
        if (s == null) return "";
        if (s.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT))) {
            return s.substring(0, s.length() - suffix.length());
        }
        return s;
    }
    private static boolean containsClassNamed(String src, String simpleName) {
        Pattern p = Pattern.compile("\\bclass\\s+" + Pattern.quote(simpleName) + "\\b");
        return p.matcher(src).find();
    }
    private static boolean containsInterfaceNamed(String src, String simpleName) {
        Pattern p = Pattern.compile("\\binterface\\s+" + Pattern.quote(simpleName) + "\\b");
        return p.matcher(src).find();
    }
    private static int backToLineStart(String s, int idx) {
        int i = idx;
        while (i > 0 && s.charAt(i - 1) != '\n' && s.charAt(i - 1) != '\r') i--;
        return i;
    }
    private static int matchBrace(String s, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
    private static String extractMethodByName(String src, String methodName) {
        if (src == null || methodName == null || methodName.isBlank()) return null;

        Pattern sig = Pattern.compile(
                "(?m)(^[\\t ]*(?:@[^\r\\n]+\\s*)*[\\w\\s<>,\\[\\]\\.]*\\b"
                        + Pattern.quote(methodName) + "\\s*\\()"
        );
        Matcher m = sig.matcher(src);
        if (!m.find()) return null;

        int start = m.start();
        int openParen = m.end() - 1;

        int depth = 0, closeParen = -1;
        for (int i = openParen; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) { closeParen = i; break; }
            }
        }
        if (closeParen < 0) return null;

        int cursor = closeParen + 1;
        while (cursor < src.length() && Character.isWhitespace(src.charAt(cursor))) cursor++;

        if (src.regionMatches(true, cursor, "throws", 0, "throws".length())) {
            cursor += "throws".length();
            while (cursor < src.length()) {
                char c = src.charAt(cursor);
                if (c == '{' || c == ';') break;
                cursor++;
            }
        } else {
            while (cursor < src.length()) {
                char c = src.charAt(cursor);
                if (c == '{' || c == ';') break;
                cursor++;
            }
        }
        if (cursor >= src.length()) return null;

        if (src.charAt(cursor) == ';') {
            return src.substring(start, cursor + 1).trim();
        }

        int end = matchBrace(src, cursor);
        if (end < 0) return null;

        return src.substring(start, end + 1).trim();
    }
    private static String extractMethodByMapping(String src, String epName) {
        String ep = epName == null ? "" : epName.toLowerCase(Locale.ROOT);
        Pattern map = Pattern.compile("@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)\\s*\\(.*?\\)", Pattern.DOTALL);
        Matcher m = map.matcher(src);
        while (m.find()) {
            String anno = m.group();
            if (anno.toLowerCase(Locale.ROOT).contains(ep)) {
                int after = m.end();
                Pattern sig = Pattern.compile("(public|protected|private)\\s+[\\w\\<\\>\\[\\],\\s]+\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(");
                Matcher sm = sig.matcher(src.substring(after));
                if (sm.find()) {
                    int sigStart = after + backToLineStart(src.substring(after), sm.start());
                    int braceOpen = src.indexOf('{', after + sm.end());
                    if (braceOpen < 0) return null;
                    int end = matchBrace(src, braceOpen);
                    if (end < 0) return null;
                    return src.substring(sigStart, end + 1).trim();
                }
            }
        }
        return null;
    }
    private static String mergeIntoRegion(String skeleton, String startMarker, String endMarker, List<String> parts) {
        return merge(skeleton, startMarker, endMarker, parts);
    }
    private static String trimSignature(String s) {
        return s == null ? "" : s.trim().replaceAll(";+\\s*$", ";");
    }
    private static String normalizeServicePaths(String path, String pkgPath, String ctrlSimple, String svcSimple, String implSimple) {
        if (path.endsWith(svcSimple + ".java")) {
            return "src/main/java/" + pkgPath + "/service/" + svcSimple + ".java";
        }
        if (path.endsWith(implSimple + ".java")) {
            return "src/main/java/" + pkgPath + "/service/" + implSimple + ".java";
        }
        if (path.endsWith(ctrlSimple + ".java")) {
            return "src/main/java/" + pkgPath + "/controller/" + ctrlSimple + ".java";
        }
        return path.replace('\\','/');
    }
    private static final class ControllerSkeletonFactory {
        static String create(String basePackage, String basePath, String ctrlSimple, String svcSimple) {
            // Placeholder: your real factory implementation should be used.
            return """
                   package %s.controller;

                   import org.springframework.web.bind.annotation.*;
                   import lombok.RequiredArgsConstructor;

                   @RestController
                   @RequestMapping("%s")
                   @RequiredArgsConstructor
                   public class %s {

                       private final %s %s;

                       %s
                   }
                   """.formatted(
                    basePackage, basePath, ctrlSimple, svcSimple, Character.toLowerCase(svcSimple.charAt(0)) + svcSimple.substring(1),
                    controllerStart() + "\n" + controllerEnd()
            ).trim();
        }
    }
    private static final class ServiceSkeletonFactory {
        static String create(String basePackage, String svcSimple) {
            return """
                   package %s.service;

                   public interface %s {
                       %s
                   }
                   """.formatted(
                    basePackage, svcSimple,
                    serviceRegionStart() + "\n" + serviceRegionEnd()
            ).trim();
        }
    }
    private static final class ServiceImplSkeletonFactory {
        static String create(String basePackage, String svcSimple, String implSimple) {
            return """
                   package %s.service;

                   import org.springframework.stereotype.Service;
                   import lombok.RequiredArgsConstructor;

                   @Service
                   @RequiredArgsConstructor
                   public class %s implements %s {

                       %s
                   }
                   """.formatted(
                    basePackage, implSimple, svcSimple,
                    serviceRegionStart() + "\n" + serviceRegionEnd()
            ).trim();
        }
    }

    private static List<CodeFile> extractFromRaw(String raw) {
        var out = new ArrayList<CodeFile>();
        if (raw == null || raw.isBlank()) return out;

        // <FILE path="...">...</FILE>
        Pattern fileTag = Pattern.compile("(?s)<FILE\\s+path=\"([^\"]+)\">\\s*(.*?)\\s*</FILE>");
        Matcher m = fileTag.matcher(raw);
        while (m.find()) {
            String path = m.group(1).trim();
            String code = stripBackticks(m.group(2));
            out.add(new CodeFile(path, "java", code));
        }
        if (!out.isEmpty()) return out;

        // ```java ... ```
        Pattern fence = Pattern.compile("(?s)```(?:java|Java)?\\s*(.*?)\\s*```");
        Matcher f = fence.matcher(raw);
        int i = 0;
        while (f.find()) {
            String code = stripPackageDuplicates(stripBackticks(f.group(1)));
            String path = "Tmp" + (++i) + ".java";
            out.add(new CodeFile(path, "java", code));
        }
        return out;
    }
    private static String stripBackticks(String s){
        if (s == null) return "";
        return s.replaceAll("^```[a-zA-Z]*\\s*", "")
                .replaceAll("\\s*```\\s*$", "");
    }
    private static String stripPackageDuplicates(String code){
        if (code == null) return "";
        return code.replaceAll("(?m)^\\s*package\\s+[^;]+;\\s*", ""); // ساده‌ترین حالت: همهٔ packageها حذف
    }
    private static List<CodeFile> normalizePathsAndPackages(List<CodeFile> files,
                                                            String basePackage,
                                                            String feature) {
        String pkgDir = basePackage.replace('.', '/');
        List<CodeFile> out = new ArrayList<>(files.size());

        for (var f : files) {
            String content = f.content() == null ? "" : f.content();
            Detected d = detectKindAndName(content);

            // زیرپوشه مقصد
            String sub = switch (d.kind()) {
                case CONTROLLER        -> "controller";
                case SERVICE_INTERFACE,
                     SERVICE_IMPL      -> "service";
                case DTO               -> "dto";
                case ENTITY            -> "domain";
                case REPOSITORY        -> "repository";
                case CLIENT            -> "client";
                default                -> "misc";
            };

            // نام فایل از روی نام کلاس واقعی؛ اگر نداشت، fallback
            String fileName = (d.className() != null && !d.className().isBlank())
                    ? d.className() + ".java"
                    : suggestFileName(switch (d.kind()) {
                case CONTROLLER -> "CONTROLLER";
                case SERVICE_INTERFACE -> "SERVICE_INTERFACE";
                case SERVICE_IMPL -> "SERVICE_IMPL";
                case DTO -> "DTO";
                default -> "OTHER";
            }, feature);

            String newPath = "src/main/java/" + pkgDir + "/" + sub + "/" + fileName;

            // تضمین/اصلاح پکیج
            String newPackage = basePackage + "." + sub;
            String fixed = ensurePackage(content, newPackage);

            // اگر ایمپل سرویس annotation نداشت، اضافه‌اش کن (ایمن)
            if (d.kind() == Kind.SERVICE_IMPL && !fixed.contains("@Service")) {
                fixed = addServiceAnnotationIfMissing(fixed);
            }

            out.add(new CodeFile(newPath, "java", fixed));
        }
        return out;
    }
    private static Detected detectKindAndName(String code) {
        if (code == null) return new Detected(Kind.OTHER, null);
        String src = code;

        // الگوهای ساده و مقاوم
        // Controller
        var mCtrl = Pattern.compile("\\bclass\\s+([A-Za-z0-9_]+Controller)\\b").matcher(src);
        if (src.contains("@RestController") || src.contains("@Controller") || mCtrl.find()) {
            String name = mCtrl.find() ? mCtrl.group(1) : extractFirstClassName(src);
            return new Detected(Kind.CONTROLLER, name);
        }

        // Service Impl
        var mImpl = Pattern.compile("\\bclass\\s+([A-Za-z0-9_]+ServiceImpl)\\b").matcher(src);
        if (src.contains("@Service") || mImpl.find()) {
            String name = mImpl.find() ? mImpl.group(1) : extractFirstClassName(src);
            return new Detected(Kind.SERVICE_IMPL, name);
        }

        // Service Interface
        var mIface = Pattern.compile("\\binterface\\s+([A-Za-z0-9_]+Service)\\b").matcher(src);
        if (mIface.find()) {
            return new Detected(Kind.SERVICE_INTERFACE, mIface.group(1));
        }

        // DTO
        var mDto = Pattern.compile("\\b(class|record)\\s+([A-Za-z0-9_]+Dto)\\b").matcher(src);
        if (mDto.find()) return new Detected(Kind.DTO, mDto.group(2));

        // Entity
        if (src.contains("@Entity")) {
            String name = extractFirstClassName(src);
            return new Detected(Kind.ENTITY, name);
        }

        // Repository
        if (src.contains("extends JpaRepository") || src.contains("@Repository") || src.contains("SimpleJdbcCall")) {
            String name = extractFirstInterfaceOrClassName(src);
            return new Detected(Kind.REPOSITORY, name);
        }

        // Client (feign/rest template facade)
        if (src.contains("@FeignClient") || src.contains("RestTemplate") || src.contains("WebClient") || src.contains("ExternalClient")) {
            String name = extractFirstInterfaceOrClassName(src);
            return new Detected(Kind.CLIENT, name);
        }

        // پیش‌فرض
        return new Detected(Kind.OTHER, extractFirstInterfaceOrClassName(src));
    }
    private static String extractFirstClassName(String src) {
        var m = Pattern.compile("\\bclass\\s+([A-Za-z0-9_]+)\\b").matcher(src);
        return m.find() ? m.group(1) : null;
    }
    private static String extractFirstInterfaceOrClassName(String src) {
        var mi = Pattern.compile("\\binterface\\s+([A-Za-z0-9_]+)\\b").matcher(src);
        if (mi.find()) return mi.group(1);
        return extractFirstClassName(src);
    }
    private static String ensurePackage(String code, String pkg){
        String body = (code == null ? "" : code).trim();

        // اگر همین پکیج از قبل صحیح است، دوباره ننویسیم
        var pm = Pattern.compile("(?m)^\\s*package\\s+([^;]+);\\s*").matcher(body);
        if (pm.find()) {
            String current = pm.group(1).trim();
            if (current.equals(pkg)) return body;          // همان پکیج است
            // پکیج قبلی را حذف می‌کنیم
            body = pm.replaceFirst("");
            body = body.trim();
        }
        return "package " + pkg + ";\n\n" + body + "\n";
    }
    private static String addServiceAnnotationIfMissing(String src) {
        // اگر کلاس ایمپل @Service ندارد، قبل از عبارت 'public class' اضافه می‌کنیم
        var m = Pattern.compile("(?m)^\\s*(public\\s+class\\s+\\w+ServiceImpl\\b)").matcher(src);
        if (m.find()) {
            return src.substring(0, m.start()) + "@Service\n" + src.substring(m.start());
        }
        return src;
    }
    private static String capitalize(String s){
        if (s == null || s.isBlank()) return "Generated";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    private static String safe(String s){ return s==null ? "" : s; }
    private static List<CodeFile> sanitizeJava(List<CodeFile> files, String basePackage){
        // اگر خواستی اصلاحات بیشتری مثل imports یا @RequestMapping پایه را انجام بده
        return files;
    }
    private static String suggestFileName(String kind, String feature){
        String cap = capitalize(safe(feature));
        return switch (kind) {
            case "CONTROLLER"        -> cap + "Controller.java";
            case "SERVICE_INTERFACE" -> cap + "Service.java";
            case "SERVICE_IMPL"      -> cap + "ServiceImpl.java";
            case "DTO"               -> cap + "Dto.java";
            default                  -> cap + ".java";
        };
    }
    private static String merge(String skeleton,
                               String startMarker,
                               String endMarker,
                               List<String> parts) {
        if (skeleton == null || skeleton.isBlank()) return skeleton;
        int start = skeleton.indexOf(startMarker);
        int end   = skeleton.indexOf(endMarker);
        if (start < 0 || end < 0 || end < start) {
            // مارکر پیدا نشد، همون اسکلت رو برگردون
            return skeleton;
        }

        String before = skeleton.substring(0, start + startMarker.length());
        String after  = skeleton.substring(end);

        StringBuilder sb = new StringBuilder();
        sb.append(before).append("\n");
        if (parts != null) {
            for (String p : parts) {
                if (p == null || p.isBlank()) continue;
                sb.append(p.trim()).append("\n");
            }
        }
        sb.append(after);
        return sb.toString();
    }

}
