package com.company.appmaker.ai.preflight;

import java.util.*;
import java.util.stream.Collectors;

public class ExternalSpec {
    public String baseUrl;   // e.g., https://api.example.com
    public String path;      // e.g., /v1/facilities/{id}
    public String method;    // GET|POST|PUT|PATCH|DELETE
    public String authType;  // NONE|BASIC|BEARER|API_KEY
    public String authValue; // اختیاری؛ ترجیحاً از config خوانده می‌شود
    public Integer timeoutMs = 5000;
    public Integer retryMax = 2;
    public Long retryBackoffMs = 200L;

    public final Map<String,String> headers = new LinkedHashMap<>();

    public String headersJson() {
        if (headers.isEmpty()) return "{}";
        return "{"
                + headers.entrySet().stream()
                .map(e -> "\"" + escape(e.getKey()) + "\":\"" + escape(e.getValue()) + "\"")
                .collect(Collectors.joining(","))
                + "}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }
}
