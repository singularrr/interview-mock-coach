package com.example.interviewmockcoach.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String mode = "mock";
    private final OpenAi openai = new OpenAi();

    @Data
    public static class OpenAi {
        private String baseUrl = "https://api.deepseek.com";
        private String apiKey = "";
        private String model = "deepseek-chat";
        private Double temperature = 0.3;
        private Integer timeoutSeconds = 15;
    }
}