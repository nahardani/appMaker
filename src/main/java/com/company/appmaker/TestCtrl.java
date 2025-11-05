package com.company.appmaker;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class TestCtrl {

    @Qualifier("openaiClientTest")
    private final ChatClient openaiClientTest;

    @GetMapping("/test")
    String test() {
        return openaiClientTest.prompt("Say 'pong'").call().content();
    }


}
