package ${basePackage}.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenAPI projectOpenAPI() {
    return new OpenAPI()
      .info(new Info()
        .title("${swaggerTitle:${artifactId} API}")
        .version("${swaggerVersion:v1}")
        .description("${swaggerDescription:Auto-generated API}"));
  }
}
