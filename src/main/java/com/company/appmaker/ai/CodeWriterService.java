package com.company.appmaker.ai;


import com.company.appmaker.ai.dto.CodeFile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class CodeWriterService {

    public int writeAll(String projectRoot, List<CodeFile> files) {
        int count = 0;
        for (CodeFile f : files) {
            count += writeOne(projectRoot, f) ? 1 : 0;
        }
        return count;
    }

    private boolean writeOne(String projectRoot, CodeFile f) {
        try {
            Path p = Path.of(projectRoot).resolve(f.path()).normalize();
            // حداقل محافظت ساده: اجازه خروج از ریشه پروژه نده
            Path root = Path.of(projectRoot).toAbsolutePath().normalize();
            if (!p.toAbsolutePath().normalize().startsWith(root)) {
                throw new SecurityException("Path escapes project root: " + p);
            }
            Files.createDirectories(p.getParent());
            Files.writeString(p, f.content());
            return true;
        } catch (IOException | SecurityException e) {
            // اینجا می‌تونی لاگ اضافه کنی
            return false;
        }
    }
}
