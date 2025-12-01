package com.ccc.okrtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing // Enables automatic audit fields
public class OkrTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(OkrTrackerApplication.class, args);
    }
}