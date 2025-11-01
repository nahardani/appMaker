package com.company.appmaker.ai.util;


import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities to normalize names/paths and robustly merge AI-generated method bodies
 * into existing Java source files between well-known region markers.
 *
 * Markers expected:
 *   Controller: // <AI-ENDPOINTS-START> ... // <AI-ENDPOINTS-END>
 *   Service & Impl: // <AI-SERVICE-REGION> ... // </AI-SERVICE-REGION>
 */
public final class AiScaffoldUtils {

    private AiScaffoldUtils() {}

    // ---- Naming helpers -----------------------------------------------------

    public static String toUpperCamel(String raw) {
        if (raw == null || raw.isBlank()) return "";
        // split by non-alnum boundaries and camel-case the parts
        String[] parts = raw.replace('_',' ').replace('-',' ').trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
        }
        // fallback if nothing collected
        if (sb.length() == 0) {
            sb.append(Character.toUpperCase(raw.charAt(0)));
            if (raw.length() > 1) sb.append(raw.substring(1));
        }
        return sb.toString();
    }
    public static String ensureControllerName(String name) {
        String base = toUpperCamel(stripSuffixIgnoreCase(name, "Controller"));
        return base + "Controller";
    }
    public static String ensureServiceName(String feature) {
        String base = toUpperCamel(stripSuffixIgnoreCase(feature, "Service"));
        return base + "Service";
    }
    public static String ensureServiceImplName(String featureOrService) {
        // Accepts "Account" or "AccountService" and returns "AccountServiceImpl"
        String base = toUpperCamel(stripSuffixIgnoreCase(featureOrService, "Service"));
        return base + "ServiceImpl";
    }
    private static String stripSuffixIgnoreCase(String s, String suffix) {
        if (s == null) return "";
        if (s.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT))) {
            return s.substring(0, s.length() - suffix.length());
        }
        return s;
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
    private static final Set<String> SUPPORTED_ARTIFACT_TYPES = Set.of(
            "controller-method", "service-method", "service-impl-method"
    );
    public static boolean isSupportedArtifactType(String type) {
        return type != null && SUPPORTED_ARTIFACT_TYPES.contains(type.trim());
    }
    public static String mergeIntoRegion(String src, String startMarker, String endMarker, String payload) {
        if (src == null) src = "";
        if (payload == null) payload = "";
        String s = src;

        int startIdx = s.indexOf(startMarker);
        int endIdx = s.indexOf(endMarker);

        String prepared = normalizePayload(payload);

        if (startIdx >= 0 && endIdx > startIdx) {
            String before = s.substring(0, startIdx + startMarker.length());
            String after = s.substring(endIdx);
            // keep one blank line around
            return before + "\n" + prepared + "\n" + after;
        }

        // Markers not found → inject a region right before class closing brace.
        int closeIdx = findClassClosingBrace(s);
        String region = "\n    " + startMarker + "\n" + indent(prepared, 4) + "\n    " + endMarker + "\n";
        if (closeIdx > 0) {
            return s.substring(0, closeIdx) + region + s.substring(closeIdx);
        }
        // As a fallback, append markers at the end.
        return s + "\n" + startMarker + "\n" + prepared + "\n" + endMarker + "\n";
    }
    private static String normalizePayload(String payload) {
        // remove surrounding fences if pasted, ensure trailing newline once
        String p = payload.trim();
        // Remove accidental leading BOM or odd chars
        p = new String(p.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        return p;
    }
    private static int findClassClosingBrace(String src) {
        // naive: last '}' that looks like closing a top-level type
        // improve by scanning balance of braces ignoring strings/comments
        int last = src.lastIndexOf('}');
        return last;
    }
    private static String indent(String text, int spaces) {
        String pad = " ".repeat(spaces);
        String[] lines = text.split("\\R", -1);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(pad).append(line).append("\n");
        }
        return sb.toString().replaceFirst("\\s+$", "");
    }
    public static Optional<String> readRegion(String src, String startMarker, String endMarker) {
        if (src == null) return Optional.empty();
        int s = src.indexOf(startMarker);
        int e = src.indexOf(endMarker);
        if (s >= 0 && e > s) {
            return Optional.of(src.substring(s + startMarker.length(), e).trim());
        }
        return Optional.empty();
    }
    public static String joinMethods(Collection<String> methods) {
        if (methods == null || methods.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String m : methods) {
            if (m == null || m.isBlank()) continue;
            if (!first) sb.append("\n\n");
            sb.append(m.trim());
            first = false;
        }
        return sb.toString();
    }
    public static String controllerStart() { return "// <AI-ENDPOINTS-START>"; }
    public static String controllerEnd()   { return "// <AI-ENDPOINTS-END>"; }
    public static String serviceRegionStart() { return "// <AI-SERVICE-REGION>"; }
    public static String serviceRegionEnd()   { return "// </AI-SERVICE-REGION>"; }


}

