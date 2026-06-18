package com.bank.frauddetection.controller;

import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/ml")
public class MlMonitoringController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String mlServiceUrl;

    public MlMonitoringController(@Value("${app.ml-service.url:http://localhost:8000}") String mlServiceUrl) {
        this.mlServiceUrl = trimTrailingSlash(mlServiceUrl);
    }

    @GetMapping("/health")
    public Map<?, ?> health() {
        return restTemplate.getForObject(URI.create(mlServiceUrl + "/health"), Map.class);
    }

    @GetMapping("/logs")
    public Map<?, ?> logs() {
        return restTemplate.getForObject(URI.create(mlServiceUrl + "/logs?limit=100"), Map.class);
    }

    @GetMapping("/feature-importance")
    public Map<?, ?> featureImportance() {
        return restTemplate.getForObject(URI.create(mlServiceUrl + "/model/feature-importance"), Map.class);
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8000";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
