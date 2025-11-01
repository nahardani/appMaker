package com.company.appmaker.ai.config;

import io.netty.handler.logging.LogLevel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class HttpClientLoggingConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        var httpClient = HttpClient.create()
                .wiretap("reactor.netty.http.client.HttpClient");
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse());
    }

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            System.out.println("[HTTP] --> " + req.method() + " " + req.url());
            req.headers().forEach((k, v) -> {
                if (!k.equalsIgnoreCase("Authorization")) {
                    System.out.println("[HTTP]     " + k + ": " + String.join(",", v));
                } else {
                    System.out.println("[HTTP]     Authorization: Bearer *****");
                }
            });
            return reactor.core.publisher.Mono.just(req);
        });
    }

    private static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(res -> {
            System.out.println("[HTTP] <-- " + res.statusCode());
            res.headers().asHttpHeaders().forEach((k, v) -> System.out.println("[HTTP]     " + k + ": " + String.join(",", v)));
            return reactor.core.publisher.Mono.just(res);
        });
    }
}
