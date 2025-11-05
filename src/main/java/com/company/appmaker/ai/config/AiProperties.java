package com.company.appmaker.ai.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.ai")
public class AiProperties {
    private OpenAi openai = new OpenAi();
    private Hf huggingface = new Hf();
    private Ollama ollama = new Ollama();

    @Data
    public static class OpenAi {
        private String baseUrl;
        private String apiKey;
        private Chat chat = new Chat();

        @Data
        public static class Chat {
            private String model;
        }
    }

    @Data
    public static class Hf {
        private String baseUrl; // باید /v1 داشته باشد
        private String apiKey;
        private String model;   // مدل HF (مثلاً openai/gpt-oss-120b:fireworks-ai)
    }

    @Data
    public static class Ollama {
        private String baseUrl;
        private Chat chat = new Chat();

        @Data
        public static class Chat {
            private String model;
        }
    }
}
