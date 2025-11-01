package com.company.appmaker.ai.util;


import com.company.appmaker.ai.dto.CodeFile;
import java.util.*;
import java.util.regex.*;

public class CodeNormalizer {

    private static final Pattern PKG = Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;\\s*$");
    private static final Pattern CLS = Pattern.compile("(?m)^\\s*public\\s+(final\\s+|abstract\\s+)?class\\s+([A-Za-z0-9_]+)");

    public static List<CodeFile> normalize(List<CodeFile> in, String basePackage, String controllerClassName){
        if (in==null) return List.of();
        String basePath = basePackage.replace('.','/');

        List<CodeFile> out = new ArrayList<>();
        for (CodeFile f : in){
            if (f==null || f.path()==null || f.content()==null) continue;
            if (!f.path().endsWith(".java")) continue;

            String layer = detectLayer(f.path(), f.content());
            String pkg   = basePackage + (layer.isEmpty() ? "" : "."+layer);
            String path  = basePath + (layer.isEmpty()? "" : "/"+layer) + "/" + suggestedFileName(f, controllerClassName);

            String content = ensurePackage(f.content(), pkg);
            content = maybeRenameClass(content, f.path(), controllerClassName, layer);

            out.add(new CodeFile(path, content, f.lang()));


        }
        return out;
    }

    private static String detectLayer(String path, String content){
        String p = path.toLowerCase(Locale.ROOT);
        if (p.contains("/controller") || content.contains("@RestController")) return "controller";
        if (p.contains("/service")    || content.contains("@Service"))       return "service";
        if (p.contains("/repository") || content.contains("@Repository"))    return "repository";
        if (p.contains("/dto")        || content.contains("class ") && p.endsWith("dto.java")) return "dto";
        return ""; // default root
    }

    private static String ensurePackage(String content, String pkg){
        Matcher m = PKG.matcher(content);
        if (m.find()){
            return m.replaceFirst("package " + pkg + ";");
        }
        return "package " + pkg + ";\n\n" + content;
    }

    private static String suggestedFileName(CodeFile f, String controllerClassName){
        String name = f.path();
        int idx = name.lastIndexOf('/');
        String fn  = (idx>=0 ? name.substring(idx+1) : name);
        if (controllerClassName!=null && !controllerClassName.isBlank()
                && fn.toLowerCase(Locale.ROOT).contains("controller")){
            return controllerClassName.replaceAll("[^A-Za-z0-9_]","") + ".java";
        }
        return fn;
    }

    private static String maybeRenameClass(String content, String origPath, String controllerClassName, String layer){
        if (!"controller".equals(layer)) return content;
        if (controllerClassName==null || controllerClassName.isBlank()) return content;
        Matcher m = CLS.matcher(content);
        if (m.find()){
            String curr = m.group(2);
            if (!curr.equals(controllerClassName)){
                return content.replaceFirst("\\bpublic\\s+(final\\s+|abstract\\s+)?class\\s+"+Pattern.quote(curr),
                        "public class " + controllerClassName);
            }
        }
        return content;
    }
}
