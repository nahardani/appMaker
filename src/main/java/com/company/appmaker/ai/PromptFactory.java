package com.company.appmaker.ai;

import org.springframework.stereotype.Component;

@Component
public class PromptFactory {
    public String forSpringScaffold(String userStory, String basePackage, String feature, String basePath, int javaVersion) {
        return """
                You are a senior Spring Boot engineer. Produce ONLY Java %d + Spring Boot 3 code. 
                DO NOT produce C#, .NET, or any non-Java code.

                ### HARD CONSTRAINTS
                - Language: Java
                - Framework: spring-boot
                - JavaVersion: %d
                - BasePackage: %s
                - Feature: %s
                - REST base path: %s
                - Validation: jakarta.validation
                - Persistence: Spring Data JPA (jakarta.persistence)
                - Separate files per class/interface/DTO. 
                - Use EXACT output schema below (no markdown fences):

                <META>
                {
                  "language": "java",
                  "framework": "spring-boot",
                  "javaVersion": %d,
                  "module": "controller/service/repository/dto/entity"
                }
                </META>

                <FILE path="src/main/java/%s/%s/dto/OrderCreateDto.java" lang="java">
                ...Java code...
                </FILE>

                Include at least:
                - Controller: %s.%s.OrderController (mapping "%s")
                - Service: %s.%s.OrderService (interface) + OrderServiceImpl (impl)
                - Repository: %s.%s.OrderRepository (extends JpaRepository)
                - Entities: Order, OrderItem (relationship)
                - DTOs: OrderCreateDto, OrderItemDto
                - Optional: Mapper (MapStruct) if helpful

                Requirements:
                %s
                """.formatted(
                javaVersion, javaVersion, basePackage, feature, basePath,
                javaVersion,
                basePackage.replace('.', '/'), feature,
                basePackage, feature, basePath,
                basePackage, feature,
                basePackage, feature,
                userStory.trim()
        );
    }
}

