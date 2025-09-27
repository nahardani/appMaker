package com.company.appmaker.ai;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * کلاینت ساده Ollama: POST /api/generate  (stream=false)
 */
@Component
public class OllamaClient {

    private final RestTemplate http = new RestTemplate();
    private final String baseUrl;

    public OllamaClient() {
        // می‌توانی از application.properties هم بخوانی
        this.baseUrl = "http://localhost:11434";
    }

    public String generate(String model, String prompt) {
        String url = baseUrl + "/api/generate";
        Map<String, Object> payload = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "options", Map.of("temperature", 0.2) // deterministic‌تر
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> res = http.exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), Map.class);
        if (!res.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Ollama error: " + res.getStatusCode());
        }
        Object out = res.getBody() != null ? res.getBody().get("response") : null;
        return out != null ? out.toString() : "";
    }
}
