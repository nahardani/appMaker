package com.company.appmaker.controller;


import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ControllerFileMerger {

    private ControllerFileMerger(){}

    private static final Pattern PKG      = Pattern.compile("(?m)^\\s*package\\s+([\\w\\.]+)\\s*;\\s*$");
    private static final Pattern IMPORT   = Pattern.compile("(?m)^\\s*import\\s+[^;]+;\\s*$");
    private static final Pattern CLASSHDR = Pattern.compile("(?s)(@\\w+[\\s\\S]*?)?\\s*public\\s+class\\s+(\\w+)\\s*\\{");
    private static final Pattern METHOD_ANNO = Pattern.compile("(?m)^\\s*@(?:Get|Post|Put|Delete|Patch|Request)Mapping\\b[\\s\\S]*?$");

    /** ساده: از کلاسِ addition، متدهای Annotated را بیرون می‌کشد. */
    private static List<String> extractAnnotatedMethods(String source){
        // ابتدا باید بدنه‌ی کلاس را جدا کنیم تا { ... } داخلی را بررسی کنیم
        int classStart = findClassBodyStart(source);
        int classEnd   = findMatchingBrace(source, classStart);
        if (classStart < 0 || classEnd < 0) return List.of();
        String body = source.substring(classStart+1, classEnd); // میان دو آکولاد

        List<String> methods = new ArrayList<>();
        Matcher anno = METHOD_ANNO.matcher(body);
        while (anno.find()){
            int from = anno.start();
            int methodStart = body.indexOf('\n', from); // انتهای خط annotation
            if (methodStart < 0) methodStart = from;

            int sigPos = nextMethodSignaturePos(body, methodStart);
            if (sigPos < 0) continue;

            int open = body.indexOf('{', sigPos);
            if (open < 0) continue;

            int close = findMatchingBrace(body, open);
            if (close < 0) continue;

            String methodBlock = body.substring(from, close+1).trim();
            methods.add(methodBlock);
        }
        return methods;
    }

    private static int nextMethodSignaturePos(String s, int from){
        // به‌صورت ساده به دنبال "public|private|protected" و سپس '('
        for (int i=from; i<s.length(); i++){
            char c = s.charAt(i);
            if (c=='@') continue; // annotation های بعدی
            if (Character.isWhitespace(c)) continue;
            // یک خط/بلاک non-anno شروع شده:
            // در عمل کافی است تا '(' بعد از شناسه method را پیدا کنیم
            int paren = s.indexOf('(', i);
            if (paren > i) return i;
            return -1;
        }
        return -1;
    }

    private static int findClassBodyStart(String src){
        Matcher m = CLASSHDR.matcher(src);
        if (!m.find()) return -1;
        // موقعیت '{' بعد از header
        int brace = src.indexOf('{', m.end()-1);
        return brace;
    }

    private static int findMatchingBrace(CharSequence s, int openPos){
        if (openPos < 0 || openPos >= s.length()) return -1;
        int level = 0;
        for (int i=openPos; i<s.length(); i++){
            char c = s.charAt(i);
            if (c=='{') level++;
            else if (c=='}'){
                level--;
                if (level==0) return i;
            }
        }
        return -1;
    }

    private static Set<String> extractImports(String src){
        Matcher m = IMPORT.matcher(src);
        Set<String> set = new LinkedHashSet<>();
        while (m.find()){
            set.add(m.group().trim());
        }
        return set;
    }

    private static String extractPackage(String src){
        Matcher m = PKG.matcher(src);
        return m.find() ? m.group().trim() : null;
    }

    private static String classHeaderAndOpen(String base){
        // از base، تا '{' اول را برگردان
        int open = findClassBodyStart(base);
        if (open < 0) return base;
        return base.substring(0, open+1);
    }

    private static String classClosing(String base){
        int open = findClassBodyStart(base);
        if (open < 0) return "";
        int close = findMatchingBrace(base, open);
        if (close < 0) return "";
        return base.substring(close); // شامل '}' پایانی و شاید چیزهای بعدش
    }

    /** مرج چند فایل کنترلر به یک متن واحد. firstWins تعیین می‌کند پایه کدام است. */
    public static String mergeControllers(List<String> controllerSources){
        if (controllerSources == null || controllerSources.isEmpty()) return "";

        // پایه = اولین
        String base = controllerSources.get(0);

        // union package: از base نگه می‌داریم
        String pkgLine = extractPackage(base);
        if (pkgLine == null){
            // اگر هیچ‌کدام package ندارند، خالی می‌گذاریم
            pkgLine = controllerSources.stream()
                    .map(ControllerFileMerger::extractPackage)
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);
        }

        // union imports
        LinkedHashSet<String> imports = new LinkedHashSet<>(extractImports(base));
        for (int i=1;i<controllerSources.size();i++){
            imports.addAll(extractImports(controllerSources.get(i)));
        }

        // متدها
        List<String> mergedMethods = new ArrayList<>(extractAnnotatedMethods(base));
        for (int i=1;i<controllerSources.size();i++){
            for (String m : extractAnnotatedMethods(controllerSources.get(i))){
                if (!isDuplicateMethod(mergedMethods, m)){
                    mergedMethods.add(m);
                }
            }
        }

        // بازسازی متن
        StringBuilder out = new StringBuilder();
        if (pkgLine != null) {
            out.append(pkgLine).append("\n\n");
        }
        for (String imp : imports){
            out.append(imp).append("\n");
        }
        if (!imports.isEmpty()) out.append("\n");

        // header کلاس از base
        out.append(classHeaderAndOpen(base)).append("\n\n");

        // بدنه: همه‌ی متدهای مرج‌شده
        for (String m : mergedMethods){
            out.append("    ").append(m).append("\n\n");
        }

        // بسته‌شدن کلاس
        out.append(classClosing(base));

        return out.toString().trim() + "\n";
    }

    private static boolean isDuplicateMethod(List<String> existing, String candidate){
        // ساده: نام متد + تعداد پارامترها را از signature پیدا کن
        String sig = methodSignatureKey(candidate);
        if (sig == null) return false;
        for (String s : existing){
            String key = methodSignatureKey(s);
            if (sig.equals(key)) return true;
        }
        return false;
    }

    private static String methodSignatureKey(String methodBlock){
        // نام متد بین 'type name(' — ساده: آخرین شناسه قبل از '('
        int paren = methodBlock.indexOf('(');
        if (paren < 0) return null;
        String left = methodBlock.substring(0, paren);
        // آخرین کلمه‌ی قبل از '('
        String[] toks = left.trim().split("\\s+");
        if (toks.length == 0) return null;
        String name = toks[toks.length-1].replaceAll("[^A-Za-z0-9_]", "");

        // تعداد پارامترها
        int open = methodBlock.indexOf('(');
        int close = methodBlock.indexOf(')', open+1);
        if (open<0 || close<0) return name;
        String params = methodBlock.substring(open+1, close).trim();
        int count = params.isEmpty() ? 0 : params.split(",").length;
        return name + "#" + count;
    }
}
