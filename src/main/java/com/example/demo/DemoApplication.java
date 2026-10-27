package com.example.demo;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @RestController
    static class DemoController {
        @GetMapping("/")
        public Map<String, String> home() {
            return Map.of(
                "application", "AWS CI/CD Demo",
                "message", "Deployed through Jenkins to Amazon EKS",
                "status", "running"
            );
        }

        @GetMapping("/health")
        public Map<String, String> health() {
            return Map.of("status", "UP");
        }
    }
}
