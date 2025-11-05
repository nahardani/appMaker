//package com.company.appmaker.ai.util;
//
//import java.util.Locale;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public final class AiMergeUtils {
//
//    private AiMergeUtils() {}
//
//    // ===== مارکرها =====
//    public static String controllerStart() { return "// <AI-ENDPOINTS-START>"; }
//    public static String controllerEnd()   { return "// <AI-ENDPOINTS-END>"; }
//    public static String serviceStart()    { return "// <AI-SERVICE-REGION>"; }
//    public static String serviceEnd()      { return "// </AI-SERVICE-REGION>"; }
//
//    // ===== نام‌سازی استاندارد =====
//    public static String ensureControllerName(String name) {
//        if (name == null || name.isBlank()) return "GeneratedController";
//        String n = upperCamel(name.trim());
//        return n.endsWith("Controller") ? n : (n + "Controller");
//    }
//
//    public static String stripControllerSuffix(String name) {
//        if (name == null) return "Generated";
//        return name.endsWith("Controller") ? name.substring(0, name.length() - "Controller".length()) : name;
//    }
//
//    public static String ensureServiceName(String feature) {
//        String f = upperCamel(feature);
//        return f.endsWith("Service") ? f : (f + "Service");
//    }
//
//    public static String ensureServiceImplName(String feature) {
//        String f = upperCamel(feature);
//        String base = f.endsWith("Service") ? f : (f + "Service");
//        return base.endsWith("Impl") ? base : (base + "Impl");
//    }
//
//    // ===== تست/مسیر =====
//    public static boolean isTestFile(String path, String content) {
//        String p = (path == null) ? "" : path.toLowerCase(Locale.ROOT);
//        boolean looksPath = p.contains("/test/") || p.contains("\\test\\") || p.contains("src/test/");
//        boolean looksContent = content != null && (
//                content.contains("@SpringBootTest")
//                        || content.contains("org.junit.jupiter")
//        );
//        return looksPath || looksContent;
//    }
//
//    public static String normalizeTestPath(String inputPath, String pkgPath) {
//        if (inputPath == null || inputPath.isBlank()) {
//            return "src/test/java/" + pkgPath + "/UnknownTest.java";
//        }
//        // اگر خودش در src/test بود همان را برگردان
//        if (inputPath.contains("src/test")) return inputPath;
//        // بقیه را به زیر مسیر تست هدایت کن
//        String fileName = inputPath.replace('\\','/');
//        int idx = fileName.lastIndexOf('/');
//        fileName = (idx >= 0) ? fileName.substring(idx + 1) : fileName;
//        if (!fileName.endsWith(".java")) fileName = fileName + ".java";
//        return "src/test/java/" + pkgPath + "/controller/" + fileName;
//    }
//
//    // ===== تشخیص نوع فایل =====
//    public static boolean isControllerFile(String path, String content, String normalizedCtrlSimple) {
//        boolean pathOk = path != null && path.replace('\\','/').contains("/controller/");
//        boolean annot  = content != null && content.contains("@RestController");
//        boolean nameOk = content != null && containsClassNamed(content, normalizedCtrlSimple);
//        return (pathOk && annot) || nameOk || (annot && content != null && content.contains("@RequestMapping"));
//    }
//
//    public static boolean isServiceFile(String path, String content, String svcSimple) {
//        if (content == null) return false;
//        final String normPath = path == null ? "" : path.replace('\\','/');
//
//        // باید حتماً interface با نام Service باشد و کلاس/impl نباشد
//        boolean inServiceFolder   = normPath.contains("/service/");
//        boolean hasInterfaceDecl  = containsInterfaceNamed(content, svcSimple);
//        boolean hasClassDecl      = containsClassNamed(content, svcSimple)       // اگر اشتباهاً کلاس هم‌نام ساخته شده باشد
//                || content.contains("@Service")
//                || containsClassNamed(content, svcSimple + "Impl");
//
//        // اگر مارکر منطقهٔ سرویس هست، فقط وقتی قبولش می‌کنیم که interface هم باشد
//        boolean regionLooksInterface = content.contains(serviceStart()) && content.contains("interface ");
//
//        return inServiceFolder && ( (hasInterfaceDecl && !hasClassDecl) || regionLooksInterface );
//    }
//
//    public static boolean isServiceImplFile(String path, String content, String implSimple) {
//        if (content == null) return false;
//        final String normPath = path == null ? "" : path.replace('\\','/');
//
//        boolean inServiceFolder  = normPath.contains("/service/");
//        boolean hasImplClassDecl = containsClassNamed(content, implSimple);
//        boolean annotatedService = content.contains("@Service");
//        boolean declaresClass    = Pattern.compile("\\bclass\\b").matcher(content).find();
//
//        // پیاده‌سازی معمولاً کلاس است، ممکن است @Service داشته باشد و/یا implements Service
//        boolean implementsService = Pattern.compile("\\bimplements\\s+\\w*Service\\b").matcher(content).find();
//
//        // برای جلوگیری از تداخل با interface:
//        boolean hasInterfaceDecl  = Pattern.compile("\\binterface\\b").matcher(content).find();
//
//        return inServiceFolder
//                && declaresClass
//                && !hasInterfaceDecl
//                && (hasImplClassDecl || annotatedService || implementsService);
//    }
//
//
//    // ===== اصلاح نام کلاس کنترلر (اگر بدون Controller آمده باشد) =====
//    public static String ensureControllerClassName(String content, String normalizedCtrlSimple) {
//        if (content == null || content.isBlank()) return content;
//        // اگر همین نام صحیح را دارد دست نمی‌زنیم
//        if (containsClassNamed(content, normalizedCtrlSimple)) return content;
//
//        // تلاش برای جایگزینی نام کلاس public class X { ... } به نام استاندارد
//        Pattern cls = Pattern.compile("(public\\s+class\\s+)([A-Za-z_][A-Za-z0-9_]*)(\\s*\\{)");
//        Matcher m = cls.matcher(content);
//        if (m.find()) {
//            return new StringBuilder(content)
//                    .replace(m.start(2), m.end(2), normalizedCtrlSimple)
//                    .toString();
//        }
//        return content;
//    }
//
//    // ===== استخراج متدها =====
//    public static String extractControllerMethodByNameOrMapping(String controllerSrc, String epName) {
//        if (controllerSrc == null || controllerSrc.isBlank()) return null;
//        String camel = camel(epName);
//
//        // 1) ابتدا بر اساس نام متد
//        String byName = extractMethodByName(controllerSrc, camel);
//        if (byName != null && !byName.isBlank()) return byName;
//
//        // 2) سپس بر اساس مپینگ شامل نام اندپوینت در مسیر
//        String byMapping = extractMethodByMapping(controllerSrc, epName);
//        if (byMapping != null && !byMapping.isBlank()) return byMapping;
//
//        // 3) اگر نشد، هیچ
//        return null;
//    }
//
//    public static String extractServiceMethodByName(String serviceSrc, String epName) {
//        if (serviceSrc == null || serviceSrc.isBlank()) return null;
//        String camel = camel(epName);
//        return extractMethodByName(serviceSrc, camel);
//    }
//
//    public static String ensureOnlyMethod(String srcOrMethod, String epName) {
//        // اگر کلاس کامل است، تلاش می‌کنیم فقط متد را برگردانیم
//        String method = extractServiceMethodByName(srcOrMethod, epName);
//        if (method != null && !method.isBlank()) return method;
//
//        method = extractControllerMethodByNameOrMapping(srcOrMethod, epName);
//        if (method != null && !method.isBlank()) return method;
//
//        // در بدترین حالت، خود ورودی را برگردان (ممکن است همین فقط بدنه متد باشد)
//        return srcOrMethod;
//    }
//
//    // ===== تگ‌گذاری =====
//    public static String wrapControllerTagged(String methodSrc) {
//        String body = methodSrc == null ? "" : methodSrc.trim();
//        // اگر خود متد کامل نیست، کمی تمیزکاری نکنیم تا بعد مرجر درست رفتار کند
//        return controllerStart() + "\n" + body + "\n" + controllerEnd();
//    }
//
//    public static String wrapServiceTagged(String methodSrc) {
//        String body = methodSrc == null ? "" : methodSrc.trim();
//        return serviceStart() + "\n" + body + "\n" + serviceEnd();
//    }
//
//    public static String normalizeAiArtifactType(String raw) {
//        if (raw == null) return "";
//        String r = raw.trim().toLowerCase(Locale.ROOT);
//        return switch (r) {
//            case "controller-method", "controller_method" -> "controller-method";
//            case "service-method", "service_method", "service-interface-method" -> "service-method";
//            case "service-impl-method", "service_impl_method", "service-implementation-method" -> "service-impl-method";
//            default -> r;
//        };
//    }
//
//    // ===== کمک‌متدهای regex =====
//    private static boolean containsClassNamed(String src, String simpleName) {
//        Pattern p = Pattern.compile("\\bclass\\s+" + Pattern.quote(simpleName) + "\\b");
//        return p.matcher(src).find();
//    }
//
//    private static boolean containsInterfaceNamed(String src, String simpleName) {
//        Pattern p = Pattern.compile("\\binterface\\s+" + Pattern.quote(simpleName) + "\\b");
//        return p.matcher(src).find();
//    }
//
//
//    private static String extractMethodByName(String src, String methodName) {
//        if (src == null || methodName == null || methodName.isBlank()) return null;
//
//        // امضا + بلاک‌های annotation در ابتدای خطوط را پوشش بده
//        // ^\s*(?:@...)* سپس هر چیزی تا رسیدن به methodName(
//        Pattern sig = Pattern.compile(
//                "(?m)(^[\\t ]*(?:@[^\r\\n]+\\s*)*[\\w\\s<>,\\[\\]\\.]*\\b"
//                        + Pattern.quote(methodName) + "\\s*\\()"
//        );
//        Matcher m = sig.matcher(src);
//        if (!m.find()) return null;
//
//        int start = m.start();          // ابتدای خط/annotationها
//        int openParen = m.end() - 1;    // روی '(' ایستاده‌ایم
//
//        // ) متناظر با '(' را بیاب
//        int depth = 0, closeParen = -1;
//        for (int i = openParen; i < src.length(); i++) {
//            char c = src.charAt(i);
//            if (c == '(') depth++;
//            else if (c == ')') {
//                depth--;
//                if (depth == 0) { closeParen = i; break; }
//            }
//        }
//        if (closeParen < 0) return null;
//
//        // بعد از ')' تا اولین { یا ; (با عبور از فاصله‌ها و throws ...)
//        int cursor = closeParen + 1;
//        while (cursor < src.length() && Character.isWhitespace(src.charAt(cursor))) cursor++;
//
//        if (src.regionMatches(true, cursor, "throws", 0, "throws".length())) {
//            cursor += "throws".length();
//            // رد شدن از عبارت throws تا رسیدن به { یا ;
//            while (cursor < src.length()) {
//                char c = src.charAt(cursor);
//                if (c == '{' || c == ';') break;
//                cursor++;
//            }
//        } else {
//            while (cursor < src.length()) {
//                char c = src.charAt(cursor);
//                if (c == '{' || c == ';') break;
//                cursor++;
//            }
//        }
//        if (cursor >= src.length()) return null;
//
//        if (src.charAt(cursor) == ';') {
//            // متد اینترفیس بدون بدنه
//            return src.substring(start, cursor + 1).trim();
//        }
//
//        // بدنه‌دار: آکولاد بسته‌ی متناظر را پیدا کن
//        int end = matchBrace(src, cursor);
//        if (end < 0) return null;
//
//        return src.substring(start, end + 1).trim();
//    }
//
//
//
//    private static String extractMethodByMapping(String src, String epName) {
//        // جست‌وجوی @GetMapping/@PostMapping/... که path شامل epName باشد
//        String ep = epName == null ? "" : epName.toLowerCase(Locale.ROOT);
//        Pattern map = Pattern.compile("@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)\\s*\\(.*?\\)", Pattern.DOTALL);
//        Matcher m = map.matcher(src);
//        while (m.find()) {
//            String anno = m.group();
//            if (anno.toLowerCase(Locale.ROOT).contains(ep)) {
//                // امضای متد بعد از annotation
//                int after = m.end();
//                // از بعد انوتیشن به دنبال امضای متد
//                Pattern sig = Pattern.compile("(public|protected|private)\\s+[\\w\\<\\>\\[\\],\\s]+\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(");
//                Matcher sm = sig.matcher(src.substring(after));
//                if (sm.find()) {
//                    int sigStart = after + backToLineStart(src.substring(after), sm.start());
//                    int braceOpen = src.indexOf('{', after + sm.end());
//                    if (braceOpen < 0) return null;
//                    int end = matchBrace(src, braceOpen);
//                    if (end < 0) return null;
//                    return src.substring(sigStart, end + 1).trim();
//                }
//            }
//        }
//        return null;
//    }
//
//    private static int backToLineStart(String s, int idx) {
//        int i = idx;
//        while (i > 0 && s.charAt(i - 1) != '\n' && s.charAt(i - 1) != '\r') i--;
//        return i;
//    }
//
//    private static int matchBrace(String s, int openIdx) {
//        int depth = 0;
//        for (int i = openIdx; i < s.length(); i++) {
//            char c = s.charAt(i);
//            if (c == '{') depth++;
//            else if (c == '}') {
//                depth--;
//                if (depth == 0) return i;
//            }
//        }
//        return -1;
//    }
//
//    private static String upperCamel(String s) {
//        if (s == null || s.isBlank()) return "Generated";
//        String[] parts = s.replaceAll("[^A-Za-z0-9]+", " ").trim().split("\\s+");
//        StringBuilder b = new StringBuilder();
//        for (String p : parts) {
//            if (p.isEmpty()) continue;
//            b.append(Character.toUpperCase(p.charAt(0)));
//            if (p.length() > 1) b.append(p.substring(1));
//        }
//        return b.length() == 0 ? "Generated" : b.toString();
//    }
//
//    private static String camel(String s) {
//        String uc = upperCamel(s);
//        return Character.toLowerCase(uc.charAt(0)) + uc.substring(1);
//    }
//}
//
