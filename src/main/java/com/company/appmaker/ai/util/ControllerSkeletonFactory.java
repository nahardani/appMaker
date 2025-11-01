package com.company.appmaker.ai.util;


public final class ControllerSkeletonFactory {

    private ControllerSkeletonFactory() {}

    /**
     * @param basePackage   e.g. "com.behsazan.customer"
     * @param controllerName e.g. "Accounting" (بدون پسوند Controller)
     * @param basePath      e.g. "/api/customer"
     * @param serviceName   e.g. "AccountingService"
     */


    public static String create(String basePackage, String basePath, String controllerName, String serviceName) {
        return
                "package " + basePackage + ".controller;\n\n" +
                        "import org.springframework.http.*;\n" +
                        "import org.springframework.web.bind.annotation.*;\n" +
                        "import lombok.RequiredArgsConstructor;\n" +
                        "import jakarta.validation.Valid;\n\n" +
                        "@RestController\n" +
                        "@RequestMapping(\"" + basePath + "\")\n" +
                        "@RequiredArgsConstructor\n" +
                        "public class " + controllerName + " {\n\n" +
                        "    private final " + serviceName + " service;\n\n" +
                        "    // <AI-ENDPOINTS-START>\n" +
                        "    // <AI-ENDPOINTS-END>\n" +
                        "}\n";
    }

    public static String build(String basePackage, String controllerName, String basePath, String serviceName) {
        String pkg = (basePackage == null || basePackage.isBlank()) ? "com.example.app" : basePackage.trim();
        String ctrl = normalizeControllerName(controllerName);
        String svc  = (serviceName == null || serviceName.isBlank()) ? (controllerName + "Service") : serviceName;

        return """
                package %s.controller;

                import org.springframework.http.*;
                import org.springframework.web.bind.annotation.*;
                import lombok.RequiredArgsConstructor;

                @RestController
                @RequestMapping("%s")
                @RequiredArgsConstructor
                public class %s {

                    private final %s service;

                    // ============================================
                    // <AI-ENDPOINTS-START>
                    //  Place AI-generated controller methods here.
                    // <AI-ENDPOINTS-END>
                    // ============================================

                }
                """.formatted(pkg, safeBasePath(basePath), ctrl, svc);
    }

    private static String normalizeControllerName(String name) {
        if (name == null || name.isBlank()) return "GeneratedController";
        String n = name.trim();
        return n.endsWith("Controller") ? n : (n + "Controller");
    }

    private static String safeBasePath(String p) {
        if (p == null || p.isBlank()) return "/api";
        return p.startsWith("/") ? p : ("/" + p);
    }
}
