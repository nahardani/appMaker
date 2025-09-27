package com.company.appmaker.ai;

public class PromptFactory {
    public static String buildControllerPrompt(String projectName, String packageBase,
                                               String controllerName, String basePath, String userNeed){
        return """
               You are a senior Spring Boot code generator.
               Task: produce Java code (Spring Boot) for REST controller/service/DTOs based on requirement.

               Constraints:
               - Package base: %s
               - Controller name: %s
               - Base path: %s
               - Use standard Spring MVC annotations.
               - Return compilable code. Include import statements.
               - If DTOs are needed, generate them.
               - Keep code idiomatic and minimal.
               - Output sections with clear markers:
                 ===PLAN===
                 <short plan>
                 ===FILES===
                 <path>:::<lang>:::<code>
                 <path>:::<lang>:::<code>
                 (Repeat for each file)
               
               Requirement (Persian allowed):
               %s
               """.formatted(packageBase, controllerName, (basePath==null?"":basePath), userNeed);
    }
}
