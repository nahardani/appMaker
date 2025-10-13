package com.company.appmaker.boot;


import com.company.appmaker.enums.PromptScope;
import com.company.appmaker.enums.PromptStatus;
import com.company.appmaker.enums.PromptTarget;
import com.company.appmaker.repo.PromptTemplateRepo;
import com.company.appmaker.service.PromptTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct; // اگر روی Boot 2.x هستی از javax.annotation.PostConstruct

@Component @RequiredArgsConstructor
public class PromptSeederCategory1 {
    private final PromptTemplateRepo repo;

    @PostConstruct
    void init(){
        if (repo.count()>0) return;
        var all = new java.util.ArrayList<PromptTemplate>();

        // باند 8-11 و 17-21؛ بدنه‌ها همان‌هایی که دادم
        String minA="8",  maxA="11";
        String minB="17", maxB="21";

        all.add(t("Controller (J8-11)", PromptTarget.CONTROLLER, minA, maxA, bodyController_8_11()));
        all.add(t("Service (J8-11)",    PromptTarget.SERVICE,    minA, maxA, bodyService_8_11()));
        all.add(t("Repository (J8-11)", PromptTarget.REPOSITORY, minA, maxA, bodyRepository_8_11()));
        all.add(t("Entity (J8-11)",     PromptTarget.ENTITY,     minA, maxA, bodyEntity_8_11()));
        all.add(t("DTO (J8-11)",        PromptTarget.DTO,        minA, maxA, bodyDto_8_11()));
        all.add(t("Test (J8-11)",       PromptTarget.TEST,       minA, maxA, bodyTest_8_11()));

        all.add(t("Controller (J17-21)", PromptTarget.CONTROLLER, minB, maxB, bodyController_17_21()));
        all.add(t("Service (J17-21)",    PromptTarget.SERVICE,    minB, maxB, bodyService_17_21()));
        all.add(t("Repository (J17-21)", PromptTarget.REPOSITORY, minB, maxB, bodyRepository_17_21())); // یا آرگومان‌ها را منظم کن
        all.add(t("Entity (J17-21)",     PromptTarget.ENTITY,     minB, maxB, bodyEntity_17_21()));
        all.add(t("DTO (J17-21)",        PromptTarget.DTO,        minB, maxB, bodyDto_17_21()));
        all.add(t("Test (J17-21)",       PromptTarget.TEST,       minB, maxB, bodyTest_17_21()));

        repo.saveAll(all);
    }

    private PromptTemplate t(String name, PromptTarget target, String min, String max, String body){
        var p = new PromptTemplate();
        p.setName(name);
        p.setCategory("1");
        p.setTarget(target);
        p.setScope(PromptScope.GLOBAL);
        p.setJavaMin(min); p.setJavaMax(max);
        p.setTags("spring,backend");
        p.setBody(body);
        p.setStatus(PromptStatus.ACTIVE);
        p.setVersion(1L);
        return p;
    }

    // ========= BODIES: Java 8-11 (Boot 2.x, javax) =========

    private String bodyController_8_11() {
        return ""
                + "You are a senior Spring Boot 2.x engineer (Java {{javaVersion}}). Generate ONLY Java code.\n"
                + "\n<META>\n"
                + "{\"language\":\"java\",\"framework\":\"spring-boot\",\"boot\":\"2.x\",\"java\":\"8-11\",\"layer\":\"controller\"}\n"
                + "</META>\n\n"
                + "Create a REST controller under package {{basePackage}}.controller named {{feature|UpperCamel}}Controller.\n"
                + "Use javax.validation for @Valid, and ResponseEntity.\n"
                + "Use a service bean {{feature|UpperCamel}}Service.\n\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/controller/{{feature|UpperCamel}}Controller.java\" lang=\"java\">\n"
                + "import org.springframework.http.*;\n"
                + "import org.springframework.web.bind.annotation.*;\n"
                + "import org.springframework.beans.factory.annotation.Autowired;\n"
                + "import javax.validation.Valid;\n"
                + "\n"
                + "@RestController\n"
                + "@RequestMapping(\"{{basePath}}\")\n"
                + "public class {{feature|UpperCamel}}Controller {\n"
                + "    @Autowired private {{feature|UpperCamel}}Service service;\n"
                + "\n"
                + "    @PostMapping\n"
                + "    public ResponseEntity<Long> create(@Valid @RequestBody {{feature|UpperCamel}}CreateDto dto){\n"
                + "        Long id = service.create(dto);\n"
                + "        return ResponseEntity.status(HttpStatus.CREATED).body(id);\n"
                + "    }\n"
                + "\n"
                + "    @GetMapping(\"/{id}\")\n"
                + "    public ResponseEntity<{{feature|UpperCamel}}Dto> get(@PathVariable Long id){\n"
                + "        return ResponseEntity.ok(service.getById(id));\n"
                + "    }\n"
                + "}\n"
                + "</FILE>\n";
    }

    private String bodyService_8_11() {
        return ""
                + "You are a Spring Boot 2.x backend engineer (Java {{javaVersion}}). Generate ONLY Java code.\n"
                + "\n<META>{\"layer\":\"service\",\"boot\":\"2.x\"}</META>\n\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/service/{{feature|UpperCamel}}Service.java\" lang=\"java\">\n"
                + "import org.springframework.stereotype.Service;\n"
                + "import org.springframework.beans.factory.annotation.Autowired;\n"
                + "import javax.persistence.EntityNotFoundException;\n"
                + "\n"
                + "@Service\n"
                + "public class {{feature|UpperCamel}}Service {\n"
                + "    @Autowired private {{feature|UpperCamel}}Repository repository;\n"
                + "    @Autowired private {{feature|UpperCamel}}Mapper mapper;\n"
                + "\n"
                + "    public Long create({{feature|UpperCamel}}CreateDto dto){\n"
                + "        var entity = mapper.toEntity(dto);\n"
                + "        repository.save(entity);\n"
                + "        return entity.getId();\n"
                + "    }\n"
                + "    public {{feature|UpperCamel}}Dto getById(Long id){\n"
                + "        return repository.findById(id)\n"
                + "                .map(mapper::toDto)\n"
                + "                .orElseThrow(() -> new EntityNotFoundException(\"Not found\"));\n"
                + "    }\n"
                + "}\n"
                + "</FILE>\n";
    }

    private String bodyRepository_8_11() {
        return ""
                + "Create Spring Data JPA repository (Boot 2.x) for {{feature|UpperCamel}}.\n"
                + "\n<META>{\"layer\":\"repository\",\"boot\":\"2.x\"}</META>\n\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/repository/{{feature|UpperCamel}}Repository.java\" lang=\"java\">\n"
                + "import org.springframework.stereotype.Repository;\n"
                + "import org.springframework.data.jpa.repository.JpaRepository;\n"
                + "\n"
                + "@Repository\n"
                + "public interface {{feature|UpperCamel}}Repository extends JpaRepository<{{feature|UpperCamel}}, Long> {}\n"
                + "</FILE>\n";
    }

    private String bodyEntity_8_11() {
        return ""
                + "Generate a JPA entity for Boot 2.x using javax.persistence (Java {{javaVersion}}).\n"
                + "\n<META>{\"layer\":\"entity\",\"boot\":\"2.x\"}</META>\n\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/entity/{{feature|UpperCamel}}.java\" lang=\"java\">\n"
                + "import javax.persistence.*;\n"
                + "import java.time.LocalDateTime;\n"
                + "\n"
                + "@Entity\n"
                + "@Table(name = \"{{feature|snake_case}}\")\n"
                + "public class {{feature|UpperCamel}} {\n"
                + "    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)\n"
                + "    private Long id;\n"
                + "    @Column(nullable=false)\n"
                + "    private String name;\n"
                + "    private LocalDateTime createdAt;\n"
                + "    // getters/setters ...\n"
                + "}\n"
                + "</FILE>\n";
    }

    private String bodyDto_8_11() {
        return ""
                + "Generate DTOs for Boot 2.x (Java {{javaVersion}}) with javax.validation.\n"
                + "\n<META>{\"layer\":\"dto\",\"boot\":\"2.x\"}</META>\n\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/dto/{{feature|UpperCamel}}CreateDto.java\" lang=\"java\">\n"
                + "import javax.validation.constraints.*;\n"
                + "public class {{feature|UpperCamel}}CreateDto {\n"
                + "    @NotBlank private String name;\n"
                + "    // getters/setters\n"
                + "}\n"
                + "</FILE>\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/dto/{{feature|UpperCamel}}Dto.java\" lang=\"java\">\n"
                + "import java.time.LocalDateTime;\n"
                + "public class {{feature|UpperCamel}}Dto {\n"
                + "    private Long id; private String name; private LocalDateTime createdAt;\n"
                + "    // getters/setters\n"
                + "}\n"
                + "</FILE>\n";
    }

    private String bodyTest_8_11() {
        return ""
                + "Generate a JUnit 5 + SpringBootTest (Boot 2.x) for {{feature}} controller.\n"
                + "\n<META>{\"layer\":\"test\",\"boot\":\"2.x\"}</META>\n\n"
                + "<FILE path=\"src/test/java/{{basePackage|dotpath}}/controller/{{feature|UpperCamel}}ControllerTest.java\" lang=\"java\">\n"
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
                + "    var dto = new {{feature|UpperCamel}}CreateDto();\n"
                + "    dto.setName(\"x\");\n"
                + "    var r = rest.postForEntity(\"{{basePath}}\", dto, Long.class);\n"
                + "    Assertions.assertEquals(HttpStatus.CREATED, r.getStatusCode());\n"
                + "  }\n"
                + "}\n"
                + "</FILE>\n";
    }

    // ========= BODIES: Java 17-21 (Boot 3.x, jakarta) =========

    private String bodyController_17_21() {
        return ""
                + "You are a senior Spring Boot 3.x engineer (Java {{javaVersion}}). Generate ONLY Java code.\n"
                + "\n<META>{\"layer\":\"controller\",\"boot\":\"3.x\",\"java\":\"17-21\"}</META>\n\n"
                + "Create a REST controller under package {{basePackage}}.controller named {{feature|UpperCamel}}Controller.\n"
                + "Use jakarta.validation for @Valid, and ResponseEntity. Call {{feature|UpperCamel}}Service.\n\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/controller/{{feature|UpperCamel}}Controller.java\" lang=\"java\">\n"
                + "import org.springframework.http.*;\n"
                + "import org.springframework.web.bind.annotation.*;\n"
                + "import lombok.RequiredArgsConstructor;\n"
                + "import jakarta.validation.Valid;\n"
                + "\n"
                + "@RestController\n"
                + "@RequestMapping(\"{{basePath}}\")\n"
                + "@RequiredArgsConstructor\n"
                + "public class {{feature|UpperCamel}}Controller {\n"
                + "    private final {{feature|UpperCamel}}Service service;\n"
                + "    @PostMapping\n"
                + "    public ResponseEntity<Long> create(@Valid @RequestBody {{feature|UpperCamel}}CreateDto dto){\n"
                + "        Long id = service.create(dto);\n"
                + "        return ResponseEntity.status(HttpStatus.CREATED).body(id);\n"
                + "    }\n"
                + "    @GetMapping(\"/{id}\")\n"
                + "    public ResponseEntity<{{feature|UpperCamel}}Dto> get(@PathVariable Long id){\n"
                + "        return ResponseEntity.ok(service.getById(id));\n"
                + "    }\n"
                + "}\n"
                + "</FILE>\n";
    }

    private String bodyService_17_21() {
        return ""
                + "Generate a service layer (Boot 3.x, Java {{javaVersion}}).\n"
                + "\n<META>{\"layer\":\"service\",\"boot\":\"3.x\"}</META>\n\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/service/{{feature|UpperCamel}}Service.java\" lang=\"java\">\n"
                + "import org.springframework.stereotype.Service;\n"
                + "import lombok.RequiredArgsConstructor;\n"
                + "import jakarta.persistence.EntityNotFoundException;\n"
                + "\n"
                + "@Service\n"
                + "@RequiredArgsConstructor\n"
                + "public class {{feature|UpperCamel}}Service {\n"
                + "    private final {{feature|UpperCamel}}Repository repository;\n"
                + "    private final {{feature|UpperCamel}}Mapper mapper;\n"
                + "\n"
                + "    public Long create({{feature|UpperCamel}}CreateDto dto){\n"
                + "        var entity = mapper.toEntity(dto);\n"
                + "        repository.save(entity);\n"
                + "        return entity.getId();\n"
                + "    }\n"
                + "    public {{feature|UpperCamel}}Dto getById(Long id){\n"
                + "        return repository.findById(id)\n"
                + "                .map(mapper::toDto)\n"
                + "                .orElseThrow(() -> new EntityNotFoundException(\"Not found\"));\n"
                + "    }\n"
                + "}\n"
                + "</FILE>\n";
    }

    private String bodyRepository_17_21() {
        return ""
                + "Spring Data JPA repository (Boot 3.x) for {{feature|UpperCamel}}.\n"
                + "\n<META>{\"layer\":\"repository\",\"boot\":\"3.x\"}</META>\n\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/repository/{{feature|UpperCamel}}Repository.java\" lang=\"java\">\n"
                + "import org.springframework.stereotype.Repository;\n"
                + "import org.springframework.data.jpa.repository.JpaRepository;\n"
                + "@Repository\n"
                + "public interface {{feature|UpperCamel}}Repository extends JpaRepository<{{feature|UpperCamel}}, Long> {}\n"
                + "</FILE>\n";
    }

    private String bodyEntity_17_21() {
        return ""
                + "JPA entity for Boot 3.x using jakarta.persistence.\n"
                + "\n<META>{\"layer\":\"entity\",\"boot\":\"3.x\"}</META>\n\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/entity/{{feature|UpperCamel}}.java\" lang=\"java\">\n"
                + "import jakarta.persistence.*;\n"
                + "import java.time.LocalDateTime;\n"
                + "\n"
                + "@Entity\n"
                + "@Table(name = \"{{feature|snake_case}}\")\n"
                + "public class {{feature|UpperCamel}} {\n"
                + "    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)\n"
                + "    private Long id;\n"
                + "    @Column(nullable=false)\n"
                + "    private String name;\n"
                + "    private LocalDateTime createdAt;\n"
                + "    // getters/setters ...\n"
                + "}\n"
                + "</FILE>\n";
    }

    private String bodyDto_17_21() {
        return ""
                + "DTOs for Boot 3.x (Java {{javaVersion}}) with jakarta.validation.\n"
                + "\n<META>{\"layer\":\"dto\",\"boot\":\"3.x\"}</META>\n\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/dto/{{feature|UpperCamel}}CreateDto.java\" lang=\"java\">\n"
                + "import jakarta.validation.constraints.*;\n"
                + "public class {{feature|UpperCamel}}CreateDto {\n"
                + "    @NotBlank private String name;\n"
                + "    // getters/setters\n"
                + "}\n"
                + "</FILE>\n"
                + "<FILE path=\"src/main/java/{{basePackage|dotpath}}/dto/{{feature|UpperCamel}}Dto.java\" lang=\"java\">\n"
                + "import java.time.LocalDateTime;\n"
                + "public class {{feature|UpperCamel}}Dto {\n"
                + "    private Long id; private String name; private LocalDateTime createdAt;\n"
                + "    // getters/setters\n"
                + "}\n"
                + "</FILE>\n";
    }

    private String bodyTest_17_21() {
        return ""
                + "JUnit 5 + SpringBootTest (Boot 3.x) for {{feature}} controller.\n"
                + "\n<META>{\"layer\":\"test\",\"boot\":\"3.x\"}</META>\n\n"
                + "<FILE path=\"src/test/java/{{basePackage|dotpath}}/controller/{{feature|UpperCamel}}ControllerTest.java\" lang=\"java\">\n"
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
                + "    var dto = new {{feature|UpperCamel}}CreateDto();\n"
                + "    dto.setName(\"x\");\n"
                + "    var r = rest.postForEntity(\"{{basePath}}\", dto, Long.class);\n"
                + "    Assertions.assertEquals(HttpStatus.CREATED, r.getStatusCode());\n"
                + "  }\n"
                + "}\n"
                + "</FILE>\n";
    }
}
