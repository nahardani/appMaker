package com.company.appmaker.ai.preflight;

import java.util.*;
import java.util.stream.Collectors;

public class MethodSig {
    public String name;          // e.g., getFacilityInfo
    public String returnType;    // e.g., List<FacilityDto> یا FacilityDto
    public final List<Param> params = new ArrayList<>();

    public String paramsJson() {
        if (params.isEmpty()) return "[]";
        return "["
                + params.stream().map(Param::toJson).collect(Collectors.joining(","))
                + "]";
    }

    public static class Param {
        public String name;   // customerId
        public String type;   // String, Long, ...
        public String source; // query|path|body|header

        public String toJson() {
            return "{"
                    + "\"name\":\"" + esc(name) + "\","
                    + "\"type\":\"" + esc(type) + "\","
                    + "\"source\":\"" + esc(source) + "\""
                    + "}";
        }
        private static String esc(String s) {
            if (s == null) return "";
            return s.replace("\\","\\\\").replace("\"","\\\"");
        }
    }
}
