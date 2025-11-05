package com.company.appmaker.ai.util;


public final class ServiceSkeletonFactory {

    private ServiceSkeletonFactory() {}


    public static String create(String basePackage, String serviceName) {
        return
                "package " + basePackage + ".service;\n\n" +
                        "public interface " + serviceName + " {\n" +
                        "    // <AI-SERVICE-REGION>\n" +
                        "    // </AI-SERVICE-REGION>\n" +
                        "}\n";
    }

    public static String build(String basePackage, String featureName) {
        String pkg = (basePackage == null || basePackage.isBlank()) ? "com.example.app" : basePackage.trim();
        String svc = normalizeServiceName(featureName);

        return """
                package %s.service;

                public interface %s {

                    // ============================================
                    // <AI-SERVICE-REGION>
                    //  Place AI-generated service interface methods here.
                    // </AI-SERVICE-REGION>
                    // ============================================

                }
                """.formatted(pkg, svc);
    }

    private static String normalizeServiceName(String name) {
        if (name == null || name.isBlank()) return "GeneratedService";
        String n = name.trim();
        return n.endsWith("Service") ? n : (n + "Service");
    }
}
