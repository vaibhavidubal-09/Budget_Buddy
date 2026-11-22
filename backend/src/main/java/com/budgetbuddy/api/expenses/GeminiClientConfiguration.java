package com.budgetbuddy.api.expenses;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GeminiClientConfiguration {

    @Bean
    RestClient.Builder geminiRestClientBuilder(
            @Value("${budgetbuddy.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory);
    }
}
