package com.company.appmaker.config;

import com.company.appmaker.ai.dto.CodeFragment;

import java.util.List;

public class MarkerMerger {

    private MarkerMerger() {
    }

    public static String merge(String skeleton,
                               String startMarker,
                               String endMarker,
                               List<String> parts) {
        if (skeleton == null || skeleton.isBlank()) return skeleton;
        int start = skeleton.indexOf(startMarker);
        int end   = skeleton.indexOf(endMarker);
        if (start < 0 || end < 0 || end < start) {
            // مارکر پیدا نشد، همون اسکلت رو برگردون
            return skeleton;
        }

        String before = skeleton.substring(0, start + startMarker.length());
        String after  = skeleton.substring(end);

        StringBuilder sb = new StringBuilder();
        sb.append(before).append("\n");
        if (parts != null) {
            for (String p : parts) {
                if (p == null || p.isBlank()) continue;
                sb.append(p.trim()).append("\n");
            }
        }
        sb.append(after);
        return sb.toString();
    }

    public static String mergeBlocks(String baseContent, List<CodeFragment> fragments, String startTag, String endTag) {
        StringBuilder out = new StringBuilder();
        String[] lines = baseContent.split("\n");
        boolean insideRegion = false;

        for (String line : lines) {
            if (line.trim().equals(startTag)) {
                insideRegion = true;
                out.append(line).append("\n");
                // درج متدهای جدید
                for (CodeFragment f : fragments) {
                    out.append(f.content()).append("\n\n");
                }
                continue;
            }
            if (line.trim().equals(endTag)) {
                insideRegion = false;
            }
            if (!insideRegion) out.append(line).append("\n");
        }

        // اگر region وجود نداشت، اضافه کن
        if (!baseContent.contains(startTag)) {
            out.append("\n").append(startTag).append("\n");
            for (CodeFragment f : fragments)
                out.append(f.content()).append("\n\n");
            out.append(endTag).append("\n");
        }
        return out.toString();
    }
}
