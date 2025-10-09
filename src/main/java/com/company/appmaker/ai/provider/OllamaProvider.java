package com.company.appmaker.ai.provider;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
public class OllamaProvider implements AiProvider {

    private final WebClient client;
    private final String defaultModel;

    public OllamaProvider(@Value("${ai.ollama.base-url}") String baseUrl,
                          @Value("${ai.ollama.model}") String defaultModel) {
        this.defaultModel = defaultModel;
        this.client = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public String generate(String model, String prompt) {
        String usedModel = (model == null || model.isBlank()) ? defaultModel : model;
        try {
            var res = client.post()
                    .uri("/api/generate")
                    .bodyValue(Map.of("model", usedModel, "prompt", prompt))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return (String) res.getOrDefault("response", "[Ollama] No response");
        } catch (Exception e) {
            log.error("Ollama error: {}", e.getMessage());
            return "[Ollama Error] " + e.getMessage();
        }
    }
}
