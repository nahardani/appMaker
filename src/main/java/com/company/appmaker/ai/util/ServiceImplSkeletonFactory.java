package com.company.appmaker.ai.util;


public final class ServiceImplSkeletonFactory {

    private ServiceImplSkeletonFactory() {}

    public static String create(String basePackage, String serviceName, String implName) {
        return
                "package " + basePackage + ".service;\n\n" +
                        "import org.springframework.stereotype.Service;\n" +
                        "import lombok.RequiredArgsConstructor;\n\n" +
                        "@Service\n" +
                        "@RequiredArgsConstructor\n" +
                        "public class " + implName + " implements " + serviceName + " {\n\n" +
                        "    // <AI-SERVICE-REGION>\n" +
                        "    // </AI-SERVICE-REGION>\n" +
                        "}\n";
    }

    /**
     * @param basePackage e.g. "com.behsazan.customer"
     * @param featureName e.g. "Accounting" (بدون پسوند Service)
     */
    public static String build(String basePackage, String featureName) {
        String pkg   = (basePackage == null || basePackage.isBlank()) ? "com.example.app" : basePackage.trim();
        String svc   = normalizeServiceName(featureName);
        String impl  = svc + "Impl";

        return """
                package %s.service;

                import org.springframework.stereotype.Service;
                import lombok.RequiredArgsConstructor;

                @Service
                @RequiredArgsConstructor
                public class %s implements %s {

                    // ============================================
                    // <AI-SERVICE-REGION>
                    //  Place AI-generated service implementation methods here.
                    // </AI-SERVICE-REGION>
                    // ============================================

                }
                """.formatted(pkg, impl, svc);
    }

    private static String normalizeServiceName(String name) {
        if (name == null || name.isBlank()) return "GeneratedService";
        String n = name.trim();
        return n.endsWith("Service") ? n : (n + "Service");
    }
}
