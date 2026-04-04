package com.example.interviewmockcoach;

import com.example.interviewmockcoach.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class InterviewMockCoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewMockCoachApplication.class, args);
    }

    @Bean
    ApplicationRunner startupModeLogger(AiProperties aiProperties,
                                        @Value("${rag.enabled:false}") boolean ragEnabled) {
        return args -> {
            String mode = aiProperties == null ? "unknown" : String.valueOf(aiProperties.getMode());
            boolean apiKeyConfigured = aiProperties != null
                    && aiProperties.getOpenai() != null
                    && aiProperties.getOpenai().getApiKey() != null
                    && !aiProperties.getOpenai().getApiKey().isBlank();
            String model = aiProperties != null && aiProperties.getOpenai() != null
                    ? aiProperties.getOpenai().getModel()
                    : "unknown";

            log.info("AI mode: {} | RAG enabled: {} | Model: {} | API key configured: {}",
                    mode,
                    ragEnabled,
                    model,
                    apiKeyConfigured);
        };
    }
}