package com.company.appmaker.ai.provider;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.Map;

@Slf4j
@Component
public class OpenAiProvider implements AiProvider {

    private final WebClient client;

    public OpenAiProvider(@Value("${ai.openai.base-url}") String baseUrl,
                          @Value("${ai.openai.model}") String defaultModel,
                          @Value("${OPENAI_API_KEY:}") String apiKey) {
        this.defaultModel = defaultModel;
        this.client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private final String defaultModel;

    @Override
    public String generate(String model, String prompt) {
        String usedModel = (model == null || model.isBlank()) ? defaultModel : model;
        try {
            Map<String, Object> body = Map.of(
                    "model", usedModel,
                    "messages", new Object[]{Map.of("role", "user", "content", prompt)}
            );
            var res = client.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (res == null) return "[OpenAI] Empty response";
            var choices = (java.util.List<Map<String, Object>>) res.get("choices");
            return (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");
        } catch (Exception e) {
            log.error("OpenAI error: {}", e.getMessage());
            return "[OpenAI Error] " + e.getMessage();
        }
    }
}

