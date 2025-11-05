package com.company.appmaker.ai.util;

import com.company.appmaker.ai.dto.CodeFile;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public final class CodeRepairPipeline {

    private enum Kind { CONTROLLER, SERVICE_INTERFACE, SERVICE_IMPL, DTO, ENTITY, REPOSITORY, CLIENT, OTHER }
    private record Detected(Kind kind, String className) {}


    private CodeRepairPipeline() {}

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

    public record Result(List<CodeFile> files) {}

    // ---------- helpers ----------

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
        if (src.contains("extends JpaRepository") || src.contains("@Repository")) {
            String name = extractFirstInterfaceOrClassName(src);
            return new Detected(Kind.REPOSITORY, name);
        }

        // Client (feign/rest template facade)
        if (src.contains("@FeignClient") || src.contains("RestTemplate")) {
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
    public static String safe(String s){
        return (s == null ? "generated" : s.replaceAll("[^A-Za-z0-9]", ""));
    }

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

//    private static List<CodeFile> ensureServiceInterfaceAndImpl(List<CodeFile> files,
//                                                                String basePackage,
//                                                                String feature) {
//        String pkgDir = basePackage.replace('.', '/');
//        String svcName   = capitalize(safe(feature)) + "Service";
//        String implName  = svcName + "Impl";
//        String svcPath   = "src/main/java/" + pkgDir + "/service/" + svcName + ".java";
//        String implPath  = "src/main/java/" + pkgDir + "/service/" + implName + ".java";
//        String svcPkg    = basePackage + ".service";
//
//        CodeFile svcFile  = null;
//        CodeFile implFile = null;
//
//        // پیدا کردن فایل‌های فعلی
//        for (var f : files) {
//            if (!"java".equalsIgnoreCase(f.lang())) continue;
//            var det = detectKindAndName(f.content());
//            if (det.kind() == Kind.SERVICE_INTERFACE && det.className() != null && det.className().equals(svcName)) {
//                svcFile = f;
//            } else if (det.kind() == Kind.SERVICE_IMPL && det.className() != null && det.className().equals(implName)) {
//                implFile = f;
//            }
//        }
//
//        // استخراج امضاها از هر کدام که داریم
//        List<String> sigsFromImpl = (implFile != null) ? extractInterfaceSignaturesFromImpl(implFile.content()) : List.of();
//        List<String> sigsFromIface= (svcFile  != null) ? extractInterfaceSignaturesFromInterface(svcFile.content())  : List.of();
//
//        // منبع امضاها: اگر interface داریم از خودش؛ وگرنه از impl
//        List<String> interfaceSigs = !sigsFromIface.isEmpty() ? sigsFromIface : sigsFromImpl;
//
//        // اگر هیچ امضایی نداریم، بی‌خیال نمی‌شویم: یک متد نمونه می‌گذاریم تا کلاس‌ها خالی نباشند
//        if (interfaceSigs.isEmpty()) {
//            interfaceSigs = List.of("ResponseEntity<Void> ping();");
//        }
//
//        // ساخت/هماهنگ‌سازی Interface
//        if (svcFile == null) {
//            String content = buildServiceInterfaceSource(svcPkg, svcName, interfaceSigs);
//            svcFile = new CodeFile(svcPath, "java", content);
//            files.add(svcFile);
//        } else {
//            // هم‌سان‌سازی: امضاهای interface موجود + هر امضایی که از impl اضافه‌تر است
//            var merged = mergeInterfaceSignatures(svcFile.content(), interfaceSigs);
//            svcFile = new CodeFile(svcPath, "java", ensurePackage(merged, svcPkg));
//            // جایگزین در لیست
//            files = replacePath(files, svcPath, svcFile);
//        }
//
//        // ساخت/هماهنگ‌سازی Impl
//        if (implFile == null) {
//            String content = buildServiceImplSource(svcPkg, implName, svcName, interfaceSigs);
//            implFile = new CodeFile(implPath, "java", content);
//            files.add(implFile);
//        } else {
//            String normalized = ensurePackage(implFile.content(), svcPkg);
//            if (!normalized.contains("@Service")) {
//                normalized = addServiceAnnotationIfMissing(normalized);
//            }
//            normalized = ensureImplImplements(normalized, svcName);
//            normalized = ensureImplHasMethods(normalized, interfaceSigs);
//            implFile = new CodeFile(implPath, "java", normalized);
//            files = replacePath(files, implPath, implFile);
//        }
//
//        return files;
//    }

    // از interface فعلی امضاها را در می‌آورد (خطوطی که به ';' ختم می‌شوند)
    private static List<String> extractInterfaceSignaturesFromInterface(String code) {
        List<String> out = new ArrayList<>();
        if (code == null) return out;
        // فقط بدنه interface را ساده پیدا کنیم
        var m = Pattern.compile("interface\\s+\\w+\\s*\\{(?s)(.*?)\\}").matcher(code);
        if (m.find()) {
            String body = m.group(1);
            // خطوطی که به ';' ختم می‌شوند و class/enum/record نیستند
            for (String line : body.split("\\R")) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("@")) continue;
                if (t.contains("(") && t.endsWith(";")) {
                    out.add(cleanSignature(t));
                }
            }
        }
        return out;
    }

//    // از impl فعلی امضاها را استخراج می‌کند: بدنه متد را حذف و ';' می‌گذارد
//    private static List<String> extractInterfaceSignaturesFromImpl(String code) {
//        List<String> out = new ArrayList<>();
//        for (String m : splitMethods(code)) { // از util موجودت استفاده کن
//            String sig = toInterfaceSignature(m); // بدنه را حذف و امضا را درست کن
//            if (sig != null) out.add(sig);
//        }
//        return out;
//    }

    // امضاهای فعلی اینترفیس را با امضاهای پیشنهادی ادغام می‌کند.
// - امضاهای تکراری حذف می‌شوند (با نرمال‌سازی برای مقایسه).
// - ترتیب: اول امضاهای موجود، بعد امضاهای جدیدِ کمبود.
// - بدنه‌ی interface با محتوای ادغام‌شده جایگزین می‌شود.
    private static String mergeInterfaceSignatures(String ifaceCode, List<String> desiredSigs) {
        if (ifaceCode == null) return null;
        List<String> existing = extractInterfaceSignaturesFromInterface(ifaceCode); // همینی که قبلاً دادم
        LinkedHashMap<String, String> merged = new LinkedHashMap<>();

        // 1) امضاهای موجود
        for (String s : existing) {
            if (s == null || s.isBlank()) continue;
            String norm = normalizeSigForCompare(s);
            // مطمئن شو با ; تمام می‌شود
            String clean = s.trim();
            if (!clean.endsWith(";")) clean = clean + ";";
            merged.put(norm, clean);
        }

        // 2) امضاهای موردنیاز (اضافه‌شده)
        for (String s : desiredSigs) {
            if (s == null || s.isBlank()) continue;
            String clean = s.trim();
            // اگر کاربر/AI modifier گذاشته، برای interface نیازی به public نیست
            clean = clean.replaceFirst("^(?i)public\\s+", "");
            if (!clean.endsWith(";")) clean = clean + ";";
            String norm = normalizeSigForCompare(clean);
            merged.putIfAbsent(norm, clean);
        }

        // 3) ساخت بدنه‌ی جدید با این امضاها
        StringBuilder body = new StringBuilder();
        boolean first = true;
        for (String sig : merged.values()) {
            if (!first) body.append("\n\n");
            first = false;
            body.append("    ").append(sig);
        }

        // 4) جایگزینی بدنه داخل interface
        return replaceInterfaceBody(ifaceCode, body.toString());
    }

    // نرمال‌سازی برای مقایسه‌ی امضاها (annotationها و فاصله‌ها حذف/یکنواخت می‌شوند)
    private static String normalizeSigForCompare(String s) {
        String t = s;
        // حذف annotationهای ابتدای خط
        t = t.replaceAll("(?m)^\\s*@\\w[\\w.()\\s,]*\\s*", "");
        // برداشتن public/protected/private/default
        t = t.replaceAll("(?i)\\b(public|protected|private|default|abstract|static|final|synchronized)\\b\\s*", "");
        // حذف فاصله‌های اضافه
        t = t.replaceAll("\\s+", " ").trim();
        // حذف ; انتهایی برای مقایسه
        t = t.replaceAll(";\\s*$", "");
        return t;
    }

    // بدنه‌ی interface را با body جدید جایگزین می‌کند، بدون تغییر پکیج/ایمپورت/هدر
    private static String replaceInterfaceBody(String code, String newBodyIndented) {
        // capture: before '{' , body , '}' پایانی
        var m = Pattern.compile("(?s)^(.*?\\binterface\\s+\\w+\\s*\\{)(.*?)(\\}\\s*)$")
                .matcher(code);
        if (m.find()) {
            String head  = m.group(1);
            String tail  = m.group(3);
            // فاصله‌گذاری مرتب
            String mid = (newBodyIndented == null || newBodyIndented.isBlank())
                    ? ""
                    : "\n" + newBodyIndented + "\n";
            return head + mid + tail;
        }
        // اگر الگو پیدا نشد، همان کد را برگردان (fail-safe)
        return code;
    }


    private static String toInterfaceSignature(String methodBlock) {
        if (methodBlock == null) return null;
        // annotationها را نگه داریم
        String ann = "";
        var am = Pattern.compile("(?ms)^(\\s*(?:@\\w[\\w.()\\s,]*\\s*)*)").matcher(methodBlock);
        if (am.find()) ann = am.group(1);

        // امضا تا قبل از '{' یا ';'
        var sm = Pattern.compile("(?ms)(public|protected|private)?\\s*(static\\s+)?([^{;(]+\\))").matcher(methodBlock);
        if (!sm.find()) return null;

        String sigPart = sm.group(3).trim(); // ReturnType name(args)
        // حذف throws و … اگر بعد از پرانتز آمده
        sigPart = sigPart.replaceAll("\\)\\s*throws\\s+[^;{]+", ")");

        // public/private را در interface لازم نداریم
        // اطمینان از انتهای ';'
        return (ann + sigPart + ";").trim();
    }

    // ساخت سورس Interface
    private static String buildServiceInterfaceSource(String pkg, String svcName, List<String> signatures) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import org.springframework.http.ResponseEntity;\n");
        sb.append("public interface ").append(svcName).append(" {\n\n");
        for (String s : signatures) {
            sb.append("    ").append(s).append("\n\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    // ساخت سورس Impl (بدنه‌ها TODO)
    private static String buildServiceImplSource(String pkg, String implName, String ifaceName, List<String> signatures) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import org.springframework.stereotype.Service;\n");
        sb.append("import lombok.RequiredArgsConstructor;\n");
        sb.append("import org.springframework.http.ResponseEntity;\n\n");
        sb.append("@Service\n@RequiredArgsConstructor\n");
        sb.append("public class ").append(implName).append(" implements ").append(ifaceName).append(" {\n\n");
        for (String s : signatures) {
            sb.append("    @Override\n");
            sb.append("    public ").append(s.replaceAll(";\\s*$", " {\n"));
            sb.append("        // TODO: implement\n");
            sb.append("        return null;\n");
            sb.append("    }\n\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String ensureImplImplements(String code, String ifaceName) {
        var m = Pattern.compile("\\bclass\\s+\\w+\\s*(extends\\s+\\w+\\s*)?(implements\\s+[^\\{]+)?\\{").matcher(code);
        if (m.find()) {
            String before = code.substring(0, m.start());
            String decl   = code.substring(m.start(), m.end());
            String after  = code.substring(m.end());

            if (decl.contains("implements")) {
                if (!decl.matches("(?s).*\\bimplements\\b[^\\{]*\\b" + Pattern.quote(ifaceName) + "\\b.*")) {
                    decl = decl.replaceFirst("\\bimplements\\b", "implements " + ifaceName + ",");
                }
            } else {
                decl = decl.replaceFirst("\\{", " implements " + ifaceName + " {");
            }
            return before + decl + after;
        }
        return code;
    }

    private static String ensureImplHasMethods(String implCode, List<String> interfaceSigs) {
        // برای هر امضای interface، اگر در impl متد متناظر نیست، یک استاب اضافه کن
        String out = implCode;
        for (String sig : interfaceSigs) {
            String methodName = extractMethodName(sig);
            if (methodName == null) continue;
            if (!containsMethodByName(out, methodName)) {
                out = appendStub(out, sig);
            }
        }
        return out;
    }

    private static String extractMethodName(String signature) {
        var m = Pattern.compile("\\b([A-Za-z_][A-Za-z0-9_]*)\\s*\\(").matcher(signature);
        if (!m.find()) return null;
        return m.group(1);
    }

    private static boolean containsMethodByName(String code, String name) {
        return Pattern.compile("\\b" + Pattern.quote(name) + "\\s*\\(").matcher(code).find();
    }

    private static String appendStub(String implCode, String ifaceSig) {
        // افزودن در انتهای کلاس قبل از '}' پایانی
        int pos = implCode.lastIndexOf('}');
        if (pos < 0) pos = implCode.length();
        StringBuilder sb = new StringBuilder(implCode.substring(0, pos));
        sb.append("\n    @Override\n");
        sb.append("    public ").append(ifaceSig.replaceAll(";\\s*$", " {\n"));
        sb.append("        // TODO: implement\n");
        sb.append("        return null;\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static List<CodeFile> replacePath(List<CodeFile> files, String path, CodeFile replacement){
        List<CodeFile> out = new ArrayList<>(files.size());
        boolean done = false;
        for (var f : files){
            if (path.equals(f.path())) {
                out.add(replacement);
                done = true;
            } else out.add(f);
        }
        if (!done) out.add(replacement);
        return out;
    }

    private static String cleanSignature(String s){
        // حذف public/private/static و … در امضای interface
        String t = s.trim();
        t = t.replaceAll("^(public|protected|private)\\s+", "");
        t = t.replaceAll("\\s+default\\s+", " ");
        return t;
    }





}
