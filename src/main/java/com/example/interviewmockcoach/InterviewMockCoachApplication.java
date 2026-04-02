package com.example.interviewmockcoach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class InterviewMockCoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewMockCoachApplication.class, args);
    }
}
