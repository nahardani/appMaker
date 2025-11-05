package com.company.appmaker.boot;

import com.company.appmaker.enums.PromptScope;
import com.company.appmaker.enums.PromptStatus;
import com.company.appmaker.enums.PromptTarget;
import com.company.appmaker.repo.PromptTemplateRepo;
import com.company.appmaker.service.PromptTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PromptSeederCategory1 {

    private final PromptTemplateRepo repo;

    @PostConstruct
    void init() {
        // اگر چیزی هست، دیگه دوباره نریز
        if (repo.count() > 0) {
            return;
        }

        String javaVersion = "21";
        String category = "spring-boot-21";

        List<PromptTemplate> all = new ArrayList<>();

        // 1) هدر/کانسترِینت مشترک
        all.add(t(
                "HEADER_CONSTRAINTS J21",
                PromptTarget.HEADER,
                javaVersion,
                category,
                bodyHeaderConstraintsJ21()
        ));

        // 2) کنترلر کامل
        all.add(t(
                "CONTROLLER J21",
                PromptTarget.CONTROLLER,
                javaVersion,
                category,
                bodyControllerJ21()
        ));

        // 3) متد کنترلر (برای مرج داخل مارکر)
        all.add(t(
                "CONTROLLER_METHOD",
                PromptTarget.CONTROLLER_METHOD,
                javaVersion,
                category,
                bodyControllerMethod()
        ));

        // 4) سرویس – اینترفیس
        all.add(t(
                "SERVICE_INTERFACE",
                PromptTarget.SERVICE,
                javaVersion,
                category,
                bodyServiceInterface()
        ));

        // 5) سرویس – ایمپل
        all.add(t(
                "SERVICE_IMPL",
                PromptTarget.SERVICE_IMPL,
                javaVersion,
                category,
                bodyServiceImpl()
        ));

        // 6) DTO
        all.add(t(
                "DTO J21",
                PromptTarget.DTO,
                javaVersion,
                category,
                bodyDtoJ21()
        ));

        // 7) ریپازیتوری
        all.add(t(
                "REPOSITORY J21",
                PromptTarget.REPOSITORY,
                javaVersion,
                category,
                bodyRepositoryJ21()
        ));

        // 8) انتیتی
        all.add(t(
                "ENTITY J21",
                PromptTarget.ENTITY,
                javaVersion,
                category,
                bodyEntityJ21()
        ));

        // 9) تست
        all.add(t(
                "TEST J21",
                PromptTarget.TEST,
                javaVersion,
                category,
                bodyTestJ21()
        ));

        repo.saveAll(all);
    }

    private PromptTemplate t(String name,
                             PromptTarget target,
                             String javaVersion,
                             String category,
                             String body) {
        PromptTemplate p = new PromptTemplate();
        p.setName(name);
        p.setCategory(category);
        p.setTarget(target);
        p.setScope(PromptScope.GLOBAL);
        p.setJavaVersion(javaVersion);
        p.setTags("spring,backend,boot3,java21");
        p.setBody(body);
        p.setStatus(PromptStatus.ACTIVE);
        p.setVersion(1L);
        return p;
    }

    // ===================== BODIES =====================

    // 1) HEADER_CONSTRAINTS J21
    private String bodyHeaderConstraintsJ21() {
        return ""
                + "You are an expert Spring Boot (Java 21) code generator. Follow ALL rules:\n"
                + "- Output MUST be a single JSON object with the EXACT schema:\n"
                + "  { \"files\": [ { \"path\": \"...\", \"language\": \"java\", \"content\": \"...\" } ], \"meta\": { \"language\": \"java\" } }\n"
                + "- Do NOT include any markdown fences, explanations, backticks, XML, or commentary—ONLY the JSON.\n"
                + "- Use package base: {{basePackage}} . All files must live under this package or its subpackages. All Java sources must start with the correct 'package ...;' line.\n"
                + "- Use Spring Boot 3.x annotations. Use jakarta.* validation/imports.\n"
                + "- Respect REST base path: {{basePath}} .\n"
                + "- Reuse or declare DTOs/services/repositories as needed. Keep names deterministic based on {{feature}}.\n"
                + "- Naming: controller={{feature|UpperCamel}}Controller.\n"
                + "- Java version is fixed: 21.\n";
    }

    // 2) CONTROLLER J21
    private String bodyControllerJ21() {
        return ""
                + "You are a senior Spring Boot 3.x engineer (Java 21). Generate ONLY Java code.\n"
                + "\n<META>{\"layer\":\"controller\",\"boot\":\"3.x\",\"java\":\"21\"}</META>\n\n"
                + "Create a REST controller under package {{basePackage}}.controller named {{feature|UpperCamel}}Controller.\n"
                + "Use jakarta.validation for @Valid, and ResponseEntity. Call {{feature|UpperCamel}}Service.\n"
                + "Controller MUST contain the region markers for AI methods:\n"
                + "    // <AI-ENDPOINTS-START>\n"
                + "    // <AI-ENDPOINTS-END>\n"
                + "\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/controller/{{feature|UpperCamel}}Controller.java\" lang=\"java\">\n"
                + "package {{basePackage}}.controller;\n"
                + "\n"
                + "import org.springframework.http.*;\n"
                + "import org.springframework.web.bind.annotation.*;\n"
                + "import lombok.RequiredArgsConstructor;\n"
                + "import jakarta.validation.Valid;\n"
                + "import {{basePackage}}.service.{{feature|UpperCamel}}Service;\n"
                + "\n"
                + "@RestController\n"
                + "@RequestMapping(\"{{basePath}}\")\n"
                + "@RequiredArgsConstructor\n"
                + "public class {{feature|UpperCamel}}Controller {\n"
                + "\n"
                + "    private final {{feature|UpperCamel}}Service service;\n"
                + "\n"
                + "    @PostMapping\n"
                + "    public ResponseEntity<Long> create(@Valid @RequestBody {{basePackage}}.dto.{{feature|UpperCamel}}CreateDto dto){\n"
                + "        Long id = service.create(dto);\n"
                + "        return ResponseEntity.status(HttpStatus.CREATED).body(id);\n"
                + "    }\n"
                + "\n"
                + "    @GetMapping(\"/{id}\")\n"
                + "    public ResponseEntity<{{basePackage}}.dto.{{feature|UpperCamel}}Dto> get(@PathVariable Long id){\n"
                + "        return ResponseEntity.ok(service.getById(id));\n"
                + "    }\n"
                + "\n"
                + "    // <AI-ENDPOINTS-START>\n"
                + "    // AI-generated endpoints will be merged here\n"
                + "    // <AI-ENDPOINTS-END>\n"
                + "}\n"
                + "</FILE>\n";
    }

    // 3) CONTROLLER_METHOD
    private String bodyControllerMethod() {
        return ""
                + "You are generating a Spring Boot REST controller method (Java 21) to be merged into a fixed skeleton.\n"
                + "Output ONE Java file under: src/main/java/{{basePackage|dotToSlash}}/controller/{{feature|UpperCamel}}Controller.java\n"
                + "The file MUST contain the full controller class with the new endpoint ONLY inside the region markers:\n"
                + "    // <AI-ENDPOINTS-START>\n"
                + "        // your single method goes here\n"
                + "    // <AI-ENDPOINTS-END>\n"
                + "Do NOT put any other controller methods outside the markers.\n"
                + "Use @GetMapping, @PostMapping, etc. based on user intent.\n"
                + "Delegate to {{feature|UpperCamel}}Service.\n";
    }

    // 4) SERVICE_INTERFACE
    private String bodyServiceInterface() {
        return ""
                + "Generate ONLY the service interface required by controller method.\n"
                + "Output EXACTLY ONE Java file at:\n"
                + "    src/main/java/{{basePackage|dotToSlash}}/service/{{feature|UpperCamel}}Service.java\n"
                + "This file must declare:\n"
                + "package {{basePackage}}.service;\n"
                + "\n"
                + "public interface {{feature|UpperCamel}}Service {\n"
                + "    // <AI-SERVICE-REGION>\n"
                + "        // Add ONLY the method signatures needed by endpoint\n"
                + "    // </AI-SERVICE-REGION>\n"
                + "}\n";
    }

    // 5) SERVICE_IMPL
    private String bodyServiceImpl() {
        return ""
                + "Generate ONLY the service implementation class for {{feature|UpperCamel}}Service (Java 21, Boot 3.x).\n"
                + "Output EXACTLY ONE Java file at:\n"
                + "    src/main/java/{{basePackage|dotToSlash}}/service/{{feature|UpperCamel}}ServiceImpl.java\n"
                + "The class MUST:\n"
                + "- be in package {{basePackage}}.service;\n"
                + "- be annotated with @Service and @RequiredArgsConstructor;\n"
                + "- implement {{feature|UpperCamel}}Service;\n"
                + "- define ONLY the methods required by endpoint between these markers:\n"
                + "      // <AI-SERVICE-REGION>\n"
                + "          // concrete method bodies implementing the interface\n"
                + "      // </AI-SERVICE-REGION>\n";
    }

    // 6) DTO J21
    private String bodyDtoJ21() {
        return ""
                + "Generate DTOs for Boot 3.x (Java 21) with jakarta.validation.\n"
                + "\n<META>{\"layer\":\"dto\",\"boot\":\"3.x\",\"java\":\"21\"}</META>\n\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/dto/{{feature|UpperCamel}}CreateDto.java\" lang=\"java\">\n"
                + "package {{basePackage}}.dto;\n"
                + "import jakarta.validation.constraints.*;\n"
                + "public class {{feature|UpperCamel}}CreateDto {\n"
                + "    @NotBlank\n"
                + "    private String name;\n"
                + "    public String getName() { return name; }\n"
                + "    public void setName(String name) { this.name = name; }\n"
                + "}\n"
                + "</FILE>\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/dto/{{feature|UpperCamel}}Dto.java\" lang=\"java\">\n"
                + "package {{basePackage}}.dto;\n"
                + "import java.time.LocalDateTime;\n"
                + "public class {{feature|UpperCamel}}Dto {\n"
                + "    private Long id;\n"
                + "    private String name;\n"
                + "    private LocalDateTime createdAt;\n"
                + "    public Long getId() { return id; }\n"
                + "    public void setId(Long id) { this.id = id; }\n"
                + "    public String getName() { return name; }\n"
                + "    public void setName(String name) { this.name = name; }\n"
                + "    public LocalDateTime getCreatedAt() { return createdAt; }\n"
                + "    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }\n"
                + "}\n"
                + "</FILE>\n";
    }

    // 7) REPOSITORY J21
    private String bodyRepositoryJ21() {
        return ""
                + "Spring Data JPA repository (Boot 3.x, Java 21) for {{feature|UpperCamel}}.\n"
                + "\n<META>{\"layer\":\"repository\",\"boot\":\"3.x\",\"java\":\"21\"}</META>\n\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/repository/{{feature|UpperCamel}}Repository.java\" lang=\"java\">\n"
                + "package {{basePackage}}.repository;\n"
                + "import org.springframework.stereotype.Repository;\n"
                + "import org.springframework.data.jpa.repository.JpaRepository;\n"
                + "import {{basePackage}}.entity.{{feature|UpperCamel}};\n"
                + "@Repository\n"
                + "public interface {{feature|UpperCamel}}Repository extends JpaRepository<{{feature|UpperCamel}}, Long> {}\n"
                + "</FILE>\n";
    }

    // 8) ENTITY J21
    private String bodyEntityJ21() {
        return ""
                + "JPA entity for Boot 3.x using jakarta.persistence (Java 21).\n"
                + "\n<META>{\"layer\":\"entity\",\"boot\":\"3.x\",\"java\":\"21\"}</META>\n\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/entity/{{feature|UpperCamel}}.java\" lang=\"java\">\n"
                + "package {{basePackage}}.entity;\n"
                + "import jakarta.persistence.*;\n"
                + "import java.time.LocalDateTime;\n"
                + "\n"
                + "@Entity\n"
                + "@Table(name = \"{{feature|snake_case}}\")\n"
                + "public class {{feature|UpperCamel}} {\n"
                + "    @Id\n"
                + "    @GeneratedValue(strategy = GenerationType.IDENTITY)\n"
                + "    private Long id;\n"
                + "    @Column(nullable = false)\n"
                + "    private String name;\n"
                + "    private LocalDateTime createdAt;\n"
                + "    public Long getId() { return id; }\n"
                + "    public void setId(Long id) { this.id = id; }\n"
                + "    public String getName() { return name; }\n"
                + "    public void setName(String name) { this.name = name; }\n"
                + "    public LocalDateTime getCreatedAt() { return createdAt; }\n"
                + "    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }\n"
                + "}\n"
                + "</FILE>\n";
    }

    // 9) TEST J21
    private String bodyTestJ21() {
        return ""
                + "JUnit 5 + SpringBootTest (Boot 3.x, Java 21) for {{feature}} controller.\n"
                + "\n<META>{\"layer\":\"test\",\"boot\":\"3.x\",\"java\":\"21\"}</META>\n\n"
                + "<FILE path=\"src/test/java/{{basePackage|dotpath}}/controller/{{feature|UpperCamel}}ControllerTest.java\" lang=\"java\">\n"
                + "package {{basePackage}}.controller;\n"
                + "import org.junit.jupiter.api.*;\n"
                + "import org.springframework.boot.test.context.SpringBootTest;\n"
                + "import org.springframework.beans.factory.annotation.Autowired;\n"
                + "import org.springframework.boot.test.web.client.TestRestTemplate;\n"
                + "import org.springframework.http.*;\n"
                + "\n"
                + "@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)\n"
                + "public class {{feature|UpperCamel}}ControllerTest {\n"
                + "  @Autowired private TestRestTemplate rest;\n"
                + "  @Test void create(){\n"
                + "    var dto = new {{basePackage}}.dto.{{feature|UpperCamel}}CreateDto();\n"
                + "    dto.setName(\"x\");\n"
                + "    var r = rest.postForEntity(\"{{basePath}}\", dto, Long.class);\n"
                + "    Assertions.assertEquals(HttpStatus.CREATED, r.getStatusCode());\n"
                + "  }\n"
                + "}\n"
                + "</FILE>\n";
    }
}
