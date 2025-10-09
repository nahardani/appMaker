package com.company.appmaker.ai;


import com.company.appmaker.ai.provider.HuggingFaceProvider;
import com.company.appmaker.ai.provider.OllamaProvider;
import com.company.appmaker.ai.provider.OpenAiProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiFacade {
    private final OpenAiProvider openAiProvider;
    private final OllamaProvider ollamaProvider;
    private final HuggingFaceProvider huggingFaceProvider;

    @Value("${ai.provider:openai}")
    private String defaultProvider;

    public String generate(String provider, String model, String prompt) {
        String target = (provider == null || provider.isBlank()) ? defaultProvider : provider.toLowerCase();
        return switch (target) {
            case "ollama" -> ollamaProvider.generate(model, prompt);
            case "huggingface" -> huggingFaceProvider.generate(model, prompt);
            default -> openAiProvider.generate(model, prompt);
        };
    }
}
