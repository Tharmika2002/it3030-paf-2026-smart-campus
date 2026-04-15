package com.sliit.smartcampus.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class AIService {

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.model:gpt-4o-mini}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient;

    public AIService(@Value("${ai.api.url:https://api.openai.com/v1/chat/completions}") String apiUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String callAI(String prompt) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY")) {
            log.warn("AI API Key not configured. Returning empty response to trigger fallback safely.");
            return "";
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "You are an AI assistant. Return strictly what is asked, without conversational filler or markdown wrappers."),
                    Map.of("role", "user", "content", prompt)
            ));

            log.info("Sending prompt to AI API. Length: {}", prompt.length());

            String response = webClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(2)))
                    .block();

            log.info("Received response from AI API.");

            JsonNode rootNode = objectMapper.readTree(response);
            return rootNode.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("Error calling AI API. Returning empty response to trigger fallback safely.", e);
            return "";
        }
    }

    @Async
    public CompletableFuture<String> generateSummaryAsync(String prompt) {
        log.info("Starting async summary generation...");
        try {
            String result = callAI(prompt);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Async summary generation failed", e);
            return CompletableFuture.completedFuture(null);
        }
    }
}
