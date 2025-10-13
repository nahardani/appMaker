package com.company.appmaker.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PromptRenderer {
    private static final Pattern VAR = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.]+)\\s*}}");

    public String render(String template, Map<String,Object> vars) {
        if (template == null) return "";
        Matcher m = VAR.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            Object val = (vars==null)? null : vars.get(key);
            m.appendReplacement(sb, Matcher.quoteReplacement(val==null? "" : String.valueOf(val)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public boolean javaVersionOk(PromptTemplate t, String javaVersion) {
        try {
            int v = Integer.parseInt(javaVersion.replace("1.", ""));
            int min = (t.getJavaMin()==null || t.getJavaMin().isBlank()) ? 0 : Integer.parseInt(t.getJavaMin());
            int max = (t.getJavaMax()==null || t.getJavaMax().isBlank()) ? Integer.MAX_VALUE : Integer.parseInt(t.getJavaMax());
            return v >= min && v <= max;
        } catch (Exception e) { return true; }
    }
}
