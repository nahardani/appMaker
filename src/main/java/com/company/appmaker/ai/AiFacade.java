// com.company.appmaker.ai.AiFacade  (کلاس فعلی‌ات را به این شکل به‌روزرسانی کن)
package com.company.appmaker.ai;

import com.company.appmaker.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiFacade {

    @Qualifier("openaiClient")
    private final ChatClient openaiClient;

    @Qualifier("hfClient")
    private final ChatClient hfClient;

    @Qualifier("ollamaClient")
    private final ChatClient ollamaClient;

    private final AiProperties props;

    public String generate(String provider, String model, String prompt) {
        String prov = (provider == null || provider.isBlank()) ? "openai" : provider.toLowerCase();
        log.debug("AI generate | provider={}, model={} (fallbacks possible)", prov, model);


        if ("huggingface".equalsIgnoreCase(prov)) {
            var m = (model != null && !model.isBlank())
                    ? model
                    : props.getHuggingface().getModel();

            var call = hfClient.prompt().user(prompt);
            if (m != null && !m.isBlank()) {
                call = call.options(OpenAiChatOptions.builder().model(m).build());
            }
            return call.call().content();
        }


        if ("ollama".equals(prov)) {
            var call = ollamaClient.prompt().user(prompt);
            if (model != null && !model.isBlank()) {
                call = call.options(new OllamaOptions.Builder().model(model).build());
            } else if (props.getOllama().getChat().getModel() != null) {
                call = call.options(new OllamaOptions.Builder().model(props.getOllama().getChat().getModel()).build());
            }
            return call.call().content();
        }

        // default: openai
        var call = openaiClient.prompt().user(prompt);
        if (model != null && !model.isBlank()) {
            call = call.options(OpenAiChatOptions.builder().model(model).build());
        } else if (props.getOpenai().getChat().getModel() != null) {
            call = call.options(OpenAiChatOptions.builder().model(props.getOpenai().getChat().getModel()).build());
        }
        return call.call().content();
    }
}
