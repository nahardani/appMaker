// com.company.appmaker.ai.config.AiConfig
package com.company.appmaker.ai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;

@Configuration
@RequiredArgsConstructor
public class AiConfig {

    private final AiProperties props;

//    private static String ensureV1(String baseUrl){
//        if (baseUrl == null || baseUrl.isBlank())
//            throw new IllegalArgumentException("AI base-url is null/blank");
//        return baseUrl.endsWith("/v1") ? baseUrl
//               : (baseUrl.endsWith("/") ? baseUrl + "v1" : baseUrl + "/v1");
//    }

    @Bean("openaiClient")
    public ChatClient openaiClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }

    /** Hugging Face Router (OpenAI-compatible) */
    @Bean("hfClient")
    public ChatClient hfClient(AiProperties props) {
        var p = props.getHuggingface();
        var api = new OpenAiApi(p.getBaseUrl(), p.getApiKey());
        var model = OpenAiChatModel.builder().openAiApi(api).build();
        return ChatClient.builder(model).defaultAdvisors().build();
    }


    @Bean("ollamaClient")
    public ChatClient ollamaClient(org.springframework.ai.ollama.OllamaChatModel ollamaModel) {
        return ChatClient.builder(ollamaModel).build();
    }

    @Bean("openaiClientTest")
    public ChatClient openaiClientTest(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }
}
