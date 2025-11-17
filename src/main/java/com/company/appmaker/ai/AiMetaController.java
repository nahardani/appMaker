package com.company.appmaker.ai;

import com.company.appmaker.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/meta")
@RequiredArgsConstructor
public class AiMetaController {

    private final AiProperties props;

    @GetMapping("/models")
    public List<String> models(@RequestParam String provider) {
        String prov = (provider == null ? "openai" : provider.trim().toLowerCase());

        switch (prov) {
            case "ollama": {
                String m = (props.getOllama() != null
                        && props.getOllama().getChat() != null)
                        ? props.getOllama().getChat().getModel()
                        : null;
                return StringUtils.hasText(m)
                        ? List.of(m)
                        : List.of("llama3:8b", "codellama:7b", "phi3");
            }
            case "huggingface": {
                String m = (props.getHuggingface() != null)
                        ? props.getHuggingface().getModel()
                        : null;
                return StringUtils.hasText(m)
                        ? List.of(m)
                        : List.of(
                        "openai/gpt-oss-120b:fireworks-ai",
                        "meta-llama/Meta-Llama-3-8B-Instruct"
                );
            }
            case "openai":
            default: {
                String m = (props.getOpenai() != null
                        && props.getOpenai().getChat() != null)
                        ? props.getOpenai().getChat().getModel()
                        : null;
                return StringUtils.hasText(m)
                        ? List.of(m)
                        : List.of("gpt-4o-mini", "gpt-4.1-mini","gpt-5.1","gpt-120");
            }
        }
    }
}

