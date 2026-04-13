package com.sliit.smartcampus.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sliit.smartcampus.ai.dto.ChatRequestDTO;
import com.sliit.smartcampus.ai.service.AIService;
import com.sliit.smartcampus.resource.dto.ApiResponse;
import com.sliit.smartcampus.resource.dto.ResourceResponseDTO;
import com.sliit.smartcampus.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AIService aiService;
    private final ResourceService resourceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    public ResponseEntity<ApiResponse<String>> chat(@RequestBody ChatRequestDTO request) {
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .status(400)
                    .message("Request message cannot be empty")
                    .timestamp(java.time.LocalDateTime.now())
                    .build());
        }
        
        log.info("Starting AI Chat. User query length: {}", request.getMessage().length());
        
        try {
            List<ResourceResponseDTO> resources = resourceService.getAllResources();
            String contextJson = "[]";
            if (!resources.isEmpty()) {
                contextJson = objectMapper.writeValueAsString(resources);
            }
            
            String prompt = "You are a smart campus assistant serving students. Be concise and friendly. " +
                            "Use the following available campus resources as your knowledge base to answer the user query accurately without making up unlisted information:\n" +
                            contextJson + "\n\n" +
                            "User asks: " + request.getMessage();
                            
            String answer = aiService.callAI(prompt);
            log.info("AI RAW RESPONSE (Chat): {}", answer);
            
            if (answer == null || answer.trim().isEmpty()) {
                log.warn("Invalid or empty AI Chat response received. Triggering fallback.");
                return getFallbackResponse();
            }
            
            log.info("AI Chat completed successfully.");
            return ResponseEntity.ok(ApiResponse.success("Success", answer.trim()));
        } catch (Exception e) {
            log.error("AI Chat failed", e);
            return getFallbackResponse();
        }
    }
    
    private ResponseEntity<ApiResponse<String>> getFallbackResponse() {
        log.info("Fallback triggered for AI Chat");
        return ResponseEntity.ok(ApiResponse.success("Fallback", "Hi there! I'm currently offline, but our portal makes finding resources easy. Need a study room? Check out the Resources tab."));
    }
}
