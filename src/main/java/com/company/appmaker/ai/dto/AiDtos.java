package com.company.appmaker.ai.dto;

import java.util.ArrayList;
import java.util.List;

public class AiDtos {
    public static class GenerateReq {
        public String model;
        public String controllerName;
        public String basePath;
        public String prompt;
    }

    public static class FileItem {
        public String path;    // مثلا src/main/java/.../OrderController.java
        public String lang;    // java/yaml/json
        public String content;
    }

    public static class GenerateRes {
        public String plan;
        public List<FileItem> files = new ArrayList<>();
    }
}
