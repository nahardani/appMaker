package com.company.appmaker.ai.util;


import com.company.appmaker.config.MarkerMerger;
import com.company.appmaker.model.Project;
import com.company.appmaker.model.coctroller.ControllerDef;
import com.company.appmaker.model.coctroller.EndpointDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.company.appmaker.ai.util.CodeRepairPipeline.safe;

public final class Utils {

    private Utils() {
    }

    public static List<Project.GeneratedFile> collectCommittedAiFiles(Project p) {
        if (p == null || p.getControllers() == null || p.getControllers().isEmpty()) {
            return List.of();
        }

        final String basePackage = resolveBasePackage(p);
        final String basePath    = resolveBasePath(p);
        final String pkgPath     = basePackage.replace('.', '/');

        List<Project.GeneratedFile> out = new ArrayList<>();

        // برای هر کنترلر پروژه
        for (var ctrl : p.getControllers()) {
            if (ctrl == null) {
                continue;
            }

            // مثال: AccountController
            String ctrlSimple = normalizeControllerName(ctrl.getName());
            // مثال: Account
            String feature    = stripControllerSuffix(ctrlSimple);
            // مثال: AccountService , AccountServiceImpl
            String svcSimple  = feature + "Service";
            String implSimple = svcSimple + "Impl";

            // مسیرهای نهایی که می‌خوایم توی ZIP داشته باشیم
            String controllerPath  = "src/main/java/" + pkgPath + "/controller/" + ctrlSimple + ".java";
            String servicePath     = "src/main/java/" + pkgPath + "/service/"    + svcSimple  + ".java";
            String serviceImplPath = "src/main/java/" + pkgPath + "/service/"    + implSimple + ".java";

            // 1) این سه تا لیست، فقط تکه‌کدهایی رو می‌گیرن که AI به‌عنوان «متد» تولید کرده
            List<String> controllerMethods    = new ArrayList<>();
            List<String> serviceSignatures    = new ArrayList<>();
            List<String> serviceImplMethods   = new ArrayList<>();

            // 2) از روی اندپوینت‌ها، آرتیفکت‌های AI رو جمع کن
            if (ctrl.getEndpoints() != null) {
                for (var ep : ctrl.getEndpoints()) {
                    if (ep == null) continue;

                    // 2-1) آرتیفکت‌های «متنی» AI (همون‌هایی که type دارن)
                    if (ep.getAiArtifacts() != null) {
                        for (var a : ep.getAiArtifacts()) {
                            if (a == null) continue;
                            String content = safe(a.getContent());
                            if (content.isBlank()) continue;

                            String t = safe(a.getType()).trim(); // مثلاً controller-method
                            switch (t) {
                                case "controller-method" -> controllerMethods.add(trimMethod(content));
                                case "service-method", "service-interface" -> serviceSignatures.add(trimSignature(content));
                                case "service-impl-method" -> serviceImplMethods.add(trimMethod(content));
                                // بقیهٔ typeها فعلاً نادیده گرفته می‌شن
                            }
                        }
                    }
                }
            }

            // 3) اسکلت‌های ثابت رو بساز (تو الان این متدها رو توی همین Utils یا فکتوری‌هات داری)
            String controllerSkeleton = controllerSkeleton(
                    basePackage,
                    (ctrl.getBasePath() != null && !ctrl.getBasePath().isBlank()) ? ctrl.getBasePath().trim() : basePath,
                    ctrlSimple,
                    svcSimple
            );
            String serviceSkeleton  = serviceSkeleton(basePackage, svcSimple);
            String serviceImplSkel  = serviceImplSkeleton(basePackage, svcSimple, implSimple);

            // 4) حالا متدهای AI رو بذار وسط مارکرها
            String mergedController = mergeIntoRegion(
                    controllerSkeleton,
                    "// <AI-ENDPOINTS-START>",
                    "// <AI-ENDPOINTS-END>",
                    controllerMethods
            );
            String mergedService = mergeIntoRegion(
                    serviceSkeleton,
                    "// <AI-SERVICE-REGION>",
                    "// </AI-SERVICE-REGION>",
                    serviceSignatures
            );
            String mergedServiceImpl = mergeIntoRegion(
                    serviceImplSkel,
                    "// <AI-SERVICE-REGION>",
                    "// </AI-SERVICE-REGION>",
                    serviceImplMethods
            );

            // 5) این سه تا رو «همیشه» اضافه کن
            out.add(new Project.GeneratedFile(controllerPath,  mergedController));
            out.add(new Project.GeneratedFile(servicePath,     mergedService));
            out.add(new Project.GeneratedFile(serviceImplPath, mergedServiceImpl));

            // 6) حالا سراغ فایل‌های دیگهٔ این کنترلر برو (DTO, repository, client, ...)
            if (ctrl.getEndpoints() != null) {
                for (var ep : ctrl.getEndpoints()) {
                    if (ep == null || ep.getAiFiles() == null) continue;

                    for (var f : ep.getAiFiles()) {
                        if (f == null) continue;
                        var path    = f.getPath();
                        var content = f.getContent();
                        if (path == null || content == null) continue;
                        if (!path.endsWith(".java")) continue;

                        // 6-1) اگه AI یه فایل همنام با سه‌تای بالا داده، نادیده بگیر
                        boolean isController = path.endsWith("/controller/" + ctrlSimple + ".java");
                        boolean isService    = path.endsWith("/service/" + svcSimple + ".java");
                        boolean isImpl       = path.endsWith("/service/" + implSimple + ".java");
                        if (isController || isService || isImpl) {
                            // ما همین بالا خودمون ساختیمش
                            continue;
                        }

                        // 6-2) مسیرهای غیرعادی رو نرمال کن (همون متدی که قبلاً داشتی)
                        path = normalizeServicePaths(path, pkgPath, ctrlSimple, svcSimple, implSimple);

                        // 6-3) بقیه رو اضافه کن
                        out.add(new Project.GeneratedFile(path, content));
                    }
                }
            }
        }

        return out;
    }


    // ====== هِلپرها ======

    private static String normalizeArtifactType(String raw) {
        if (raw == null) return "";
        String t = raw.trim().toLowerCase();
        return switch (t) {
            case "controller", "controller-method", "controller_method" -> "controller-method";
            case "service", "service-method", "service_method", "service-interface", "service_interface" -> "service-method";
            case "service-impl", "service_impl", "service-impl-method", "service_impl_method" -> "service-impl-method";
            default -> t;
        };
    }

    private static boolean isSameJavaFile(String p1, String p2) {
        if (p1 == null || p2 == null) return false;
        String a = p1.replace('\\', '/');
        String b = p2.replace('\\', '/');
        return Objects.equals(a, b);
    }

//    private static String trimMethod(String s) {
//        return s == null ? "" : s.trim();
//    }

    private static String trimSignature(String s) {
        return s == null ? "" : s.trim().replaceAll(";+\\s*$", ";");
    }

    // ====== متدهایی که قبلاً داشتی، اینجا فقط اسم می‌گذارم؛ بدنه‌اش را از کلاس فعلی خودت بردار ======
//    public static String resolveBasePackage(Project p) {
//        // از کلاس فعلی Utils خودت بذار
//        return "com.example.demo";
//    }

//    private static String resolveBasePath(Project p) {
//        // از کلاس فعلی Utils خودت بذار
//        return "/api";
//    }

    public static String normalizeControllerName(String name) {
        if (name == null || name.isBlank()) return "DemoController";
        return name.endsWith("Controller") ? name.trim() : name.trim() + "Controller";
    }

    public static String stripControllerSuffix(String ctrlSimple) {
        if (ctrlSimple == null) return "Demo";
        return ctrlSimple.replaceAll("Controller$", "");
    }

    private static String controllerSkeleton(String basePackage, String basePath, String ctrlSimple, String svcSimple) {
        // اینو از ControllerSkeletonFactory خودت فراخوانی کن
        return ControllerSkeletonFactory.create(basePackage, basePath, ctrlSimple, svcSimple);
    }

    private static String serviceSkeleton(String basePackage, String svcSimple) {
        return ServiceSkeletonFactory.create(basePackage, svcSimple);
    }

    private static String serviceImplSkeleton(String basePackage, String svcSimple, String implSimple) {
        return ServiceImplSkeletonFactory.create(basePackage, svcSimple, implSimple);
    }

    private static String normalizeServicePaths(String path, String pkgPath, String ctrlSimple, String svcSimple, String implSimple) {
        // اگر مثلاً مدل داد: AccountService.java بدون فولدر
        if (path.endsWith(svcSimple + ".java")) {
            return "src/main/java/" + pkgPath + "/service/" + svcSimple + ".java";
        }
        if (path.endsWith(implSimple + ".java")) {
            return "src/main/java/" + pkgPath + "/service/" + implSimple + ".java";
        }
        if (path.endsWith(ctrlSimple + ".java")) {
            return "src/main/java/" + pkgPath + "/controller/" + ctrlSimple + ".java";
        }
        return path;
    }
    public static String resolveBasePackage(Project p){
        var ms = p.getMs();
        if (ms!=null && ms.getBasePackage()!=null && !ms.getBasePackage().isBlank()) return ms.getBasePackage().trim();
        String group = (p.getCompanyName()==null || p.getCompanyName().isBlank()) ? "com.example" : p.getCompanyName().trim();
        String artifact = (p.getProjectName()==null || p.getProjectName().isBlank()) ? "app" : p.getProjectName().trim().toLowerCase().replaceAll("[^a-z0-9]+","-");
        return group + "." + artifact.replace('-', '.');
    }
    private static String resolveBasePath(Project p){
        var ms = p.getMs();
        if (ms!=null && ms.getBasePath()!=null && !ms.getBasePath().isBlank()) return ms.getBasePath().trim();
        String artifact = (p.getProjectName()==null || p.getProjectName().isBlank()) ? "app" : p.getProjectName().trim().toLowerCase().replaceAll("[^a-z0-9]+","-");
        return "/api/" + artifact.replace("-","");
    }
//    public static String normalizeControllerName(String name){
//        if (name==null || name.isBlank()) return "GeneratedController";
//        String n = name.trim();
//        return n.endsWith("Controller") ? n : (n + "Controller");
//    }
//    public static String stripControllerSuffix(String ctrlSimple){
//        return ctrlSimple.endsWith("Controller") ? ctrlSimple.substring(0, ctrlSimple.length()-"Controller".length()) : ctrlSimple;
//    }
    private static String safe(String s){ return s==null ? "" : s; }
    public static String trimMethod(String code) {
        if (code == null) return "";
        String t = code.trim();

        // پاک‌سازی مارک‌داون احتمالی
        t = t.replaceAll("^```[a-zA-Z]*\\s*", "");
        t = t.replaceAll("\\s*```\\s*$", "");

        // اگر به اشتباه فقط بدنه آمده، همون رو برگردون
        // (فرض: ورودی «متد کامل» شامل امضا + بدنه است)
        // اینجا فقط تمیزکاری سبک انجام می‌دهیم.
        return t;
    }
//    public static String trimSignature(String code) {
//        if (code == null) return "";
//        String t = code.trim();
//
//        // پاک‌سازی مارک‌داون احتمالی
//        t = t.replaceAll("^```[a-zA-Z]*\\s*", "");
//        t = t.replaceAll("\\s*```\\s*$", "");
//
//        // اگر بدنه داشت، تا قبل از '{' بُرش بده
//        int brace = t.indexOf('{');
//        if (brace >= 0) {
//            t = t.substring(0, brace).trim();
//        }
//
//        // تلاش قوی‌تر: امضا (به‌همراه annotation ها و throws) را بگیر
//        // اگر regex ماتچ شد، از آن استفاده کن؛ وگرنه همان t.
//        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
//                "(?s)\\A\\s*((?:@\\w[\\w.()\\s,]*\\n\\s*)*)" +            // annotations (اختیاری)
//                        "([\\w\\<\\>\\[\\]\\s,?&]+\\s+\\w+\\s*\\([^)]*\\)\\s*(?:throws\\s+[^;{]+)?)\\s*;?\\s*\\z"
//        );
//        java.util.regex.Matcher m = p.matcher(t);
//        if (m.find()) {
//            String anns = m.group(1) == null ? "" : m.group(1);
//            String sig  = m.group(2) == null ? "" : m.group(2).trim();
//            t = (anns + sig).trim();
//        }
//
//        // مطمئن شو با ; تمام می‌شود (امضای اینترفیس)
//        if (!t.endsWith(";")) t = t + ";";
//        return t;
//    }

    private static String mergeIntoRegion(String skeleton,
                                          String startMarker,
                                          String endMarker,
                                          List<String> parts) {
        return MarkerMerger.merge(skeleton, startMarker, endMarker, parts);
    }

}
