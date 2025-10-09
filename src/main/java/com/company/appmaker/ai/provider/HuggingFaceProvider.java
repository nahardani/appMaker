package com.company.appmaker.ai.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HuggingFaceProvider implements AiProvider {

    private final WebClient client;
    private final String baseUrl;
    private final String defaultModel;

    public HuggingFaceProvider(
            @Value("${ai.huggingface.base-url}") String baseUrl,
            @Value("${ai.huggingface.model}") String defaultModel,
            @Value("${ai.huggingface.api-key:}") String apiKey
    ) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.defaultModel = defaultModel;

        WebClient.Builder b = WebClient.builder()
                .baseUrl(this.baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (apiKey != null && !apiKey.isBlank()) {
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        this.client = b.build();
    }

    @Override
    public String generate(String model, String prompt) {
        String usedModel = (model == null || model.isBlank()) ? defaultModel : model;

        Map<String, Object> body = Map.of(
                "model", usedModel,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", prompt
                ))
                // در صورت نیاز می‌تونی این پارامترها رو هم اضافه کنی:
                // "temperature", 0.2, "max_tokens", 512, "top_p", 1
        );

        try {
            Map<?, ?> res = client.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (res == null) return "[HF Router] Empty response";

            List<?> choices = (List<?>) res.get("choices");
            if (choices == null || choices.isEmpty()) {
                Object err = res.get("error");
                return err != null ? "[HF Router Error] " + err : "[HF Router] No choices";
            }

            Object first = choices.get(0);
            if (first instanceof Map<?, ?> m) {
                Object msg = m.get("message");
                if (msg instanceof Map<?, ?> mm) {
                    Object content = mm.get("content");
                    if (content != null) return content.toString();
                }
            }
            return first.toString();
        } catch (Exception e) {
            log.error("HF Router error: {}", e.getMessage());
            return "[HF Router Error] " + e.getMessage();
        }
    }
}
