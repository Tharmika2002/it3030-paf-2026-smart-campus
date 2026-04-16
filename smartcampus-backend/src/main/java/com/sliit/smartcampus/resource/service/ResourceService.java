package com.sliit.smartcampus.resource.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sliit.smartcampus.ai.service.AIService;
import com.sliit.smartcampus.resource.dto.*;
import com.sliit.smartcampus.resource.entity.Resource;
import com.sliit.smartcampus.resource.entity.ResourceStatus;
import com.sliit.smartcampus.resource.entity.ResourceType;
import com.sliit.smartcampus.resource.exception.DuplicateResourceException;
import com.sliit.smartcampus.resource.exception.ResourceNotFoundException;
import com.sliit.smartcampus.resource.mapper.ResourceMapper;
import com.sliit.smartcampus.resource.repository.ResourceRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper;
    private final AIService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager entityManager;

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ResourceResponseDTO createResource(ResourceRequestDTO dto) {
        resourceRepository.findByNameAndLocation(dto.getName(), dto.getLocation())
                .ifPresent(r -> {
                    throw new DuplicateResourceException("Resource already exists at this location");
                });

        Resource resource = resourceMapper.toEntity(dto);
        resource.setArchived(false);

        resource.setQrCode(java.util.UUID.randomUUID().toString());

        Resource saved = resourceRepository.save(resource);

        entityManager.flush();
        entityManager.refresh(saved);
        final Resource finalSaved = saved;

        log.info("Triggering async AI summary generation for resource: {}", saved.getId());
        String prompt = "Generate a short engaging description for a campus resource.\nType: " + saved.getType()
                + "\nCapacity: " + saved.getCapacity()
                + "\nLocation: " + saved.getLocation()
                + "\nAmenities: " + saved.getAmenities()
                + "\nTags: " + saved.getTags();

        aiService.generateSummaryAsync(prompt).thenAccept(summary -> {
            if (summary != null && !summary.isEmpty()) {
                finalSaved.setAiSummary(summary.trim());
                resourceRepository.save(finalSaved);
                log.info("AI summary saved for resource: {}", finalSaved.getId());
            } else {
                log.warn("AI summary generation returned empty for resource: {}", finalSaved.getId());
            }
        }).exceptionally(ex -> {
            log.error("Failed to generate AI summary for resource: {}", finalSaved.getId(), ex);
            return null;
        });

        return resourceMapper.toDTO(finalSaved);
    }

    @Transactional(readOnly = true)
    public List<ResourceResponseDTO> getAllResources() {
        return resourceRepository.findByArchivedFalse().stream()
                .map(resourceMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Resource getResourceEntityById(UUID id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
    }

    @Transactional(readOnly = true)
    public ResourceResponseDTO getResourceById(UUID id) {
        return resourceMapper.toDTO(getResourceEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<ResourceResponseDTO> getResourcesByType(ResourceType type) {
        return resourceRepository.findByTypeAndArchivedFalse(type).stream()
                .map(resourceMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResourceResponseDTO> getResourcesByStatus(ResourceStatus status) {
        return resourceRepository.findByStatusAndArchivedFalse(status).stream()
                .map(resourceMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResourceResponseDTO> searchByLocation(String location) {
        return resourceRepository.findByLocationContainingIgnoreCaseAndArchivedFalse(location).stream()
                .map(resourceMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ResourceResponseDTO> getAllResourcesPaged(Pageable pageable) {
        return resourceRepository.findAllByArchivedFalse(pageable)
                .map(resourceMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ResourceResponseDTO> filterResources(ResourceType type, ResourceStatus status, Pageable pageable) {
        return resourceRepository.findByTypeAndStatusAndArchivedFalse(type, status, pageable)
                .map(resourceMapper::toDTO);
    }

    @Transactional
    public ResourceResponseDTO updateResource(UUID id, ResourceRequestDTO dto) {
        Resource existingResource = getResourceEntityById(id);
        existingResource.setName(dto.getName());
        existingResource.setType(dto.getType());
        existingResource.setCapacity(dto.getCapacity());
        existingResource.setLocation(dto.getLocation());
        existingResource.setStatus(dto.getStatus());
        existingResource.setAmenities(dto.getAmenities());
        existingResource.setImageUrl(dto.getImageUrl());
        existingResource.setTags(dto.getTags());
        existingResource.setAiSummary(dto.getAiSummary());
        return resourceMapper.toDTO(resourceRepository.save(existingResource));
    }

    @Transactional
    public void deleteResource(UUID id) {
        Resource existingResource = getResourceEntityById(id);
        existingResource.setArchived(true);
        resourceRepository.save(existingResource);
    }

    @Transactional
    public ResourceResponseDTO updateStatus(UUID id, UpdateStatusDTO dto) {
        Resource existingResource = getResourceEntityById(id);
        existingResource.setStatus(dto.getStatus());

        if (dto.getStatus() == ResourceStatus.OUT_OF_SERVICE) {
            // TODO: placeholder logic for booking cancellation
        }

        return resourceMapper.toDTO(resourceRepository.save(existingResource));
    }

    @Transactional
    public ReviewResponseDTO addReview(UUID id, ReviewRequestDTO dto) {
        getResourceEntityById(id);
        return ReviewResponseDTO.builder()
                .id(UUID.randomUUID())
                .resourceId(id)
                .rating(dto.getRating())
                .comment(dto.getComment())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public AvailabilityDTO checkAvailability(UUID id, LocalDateTime from, LocalDateTime to) {
        getResourceEntityById(id);
        return AvailabilityDTO.builder()
                .from(from)
                .to(to)
                .available(true)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI — Search
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ResourceResponseDTO> aiSearch(String query) {
        log.info("Starting AI search for query: {}", query);

        List<Resource> resources = resourceRepository.findByArchivedFalse();
        if (resources.isEmpty()) {
            log.warn("No resources found in DB");
            return List.of();
        }

        try {
            // FIX 1: Added capacity to context so AI can match people/capacity queries
            List<Map<String, Object>> context = new ArrayList<>();
            for (int i = 0; i < resources.size(); i++) {
                Resource r = resources.get(i);
                Map<String, Object> item = new HashMap<>();
                item.put("index",    i);
                item.put("name",     r.getName()     != null ? r.getName()         : "");
                item.put("type",     r.getType()     != null ? r.getType().name()  : "");
                item.put("location", r.getLocation() != null ? r.getLocation()     : "");
                item.put("capacity", r.getCapacity() != null ? r.getCapacity()     : 0);
                item.put("amenities",r.getAmenities()!= null ? r.getAmenities()    : List.of());
                context.add(item);
            }

            String jsonResources = objectMapper.writeValueAsString(context);
            log.info("AI search prompt built with {} resources.", context.size());

            String prompt = "You are a campus resource search assistant.\n\n" +
                    "Available resources (each has an index, name, type, location, capacity, amenities):\n" +
                    jsonResources + "\n\n" +
                    "User query: '" + query + "'\n\n" +
                    "Task: Find ALL resources that satisfy EVERY condition mentioned in the query.\n" +
                    "Rules for matching:\n" +
                    "* Vague location words like 'somewhere', 'anywhere', 'a place', 'a room', 'any' mean NO location filter — ignore them\n" +
                    "* Only apply a location filter if the query mentions a specific place like 'Block A', 'F1304', 'Building 2' etc.\n" +
                    "* If query mentions amenities (e.g. projector, AC, whiteboard), the resource must have ALL of them in its amenities list — match case-insensitively\n" +
                    "* If query mentions capacity or number of people, the resource capacity must be >= that number\n" +
                    "* If query mentions a type (lab, lecture hall, meeting room), match the type field\n" +
                    "* Only include a resource if it satisfies ALL conditions in the query — do not include partial matches\n" +
                    "* If no resources match, return an empty array: {\"matchedIndexes\": []}\n\n" ;

            String aiResponseRaw = aiService.callAI(prompt);
            log.info("AI RAW RESPONSE (Search): {}", aiResponseRaw);

            if (aiResponseRaw == null || aiResponseRaw.trim().isEmpty()
                    || aiResponseRaw.length() < 5
                    || !aiResponseRaw.contains("{")) {
                log.warn("AI response missing JSON structure. Triggering fallback.");
                return fallbackSearch(query);
            }

            String cleaned = safeExtractJson(aiResponseRaw);
            if ("{}".equals(cleaned)) {
                log.warn("safeExtractJson could not extract valid JSON. Triggering fallback.");
                return fallbackSearch(query);
            }

            JsonNode node;
            try {
                node = objectMapper.readTree(cleaned);
            } catch (Exception ex) {
                log.error("Failed to parse cleaned AI JSON for search", ex);
                return fallbackSearch(query);
            }

            if (node == null || node.isMissingNode()
                    || !node.has("matchedIndexes")
                    || !node.get("matchedIndexes").isArray()) {
                log.warn("matchedIndexes missing or not an array. Triggering fallback.");
                return fallbackSearch(query);
            }

            List<Integer> indexes = new ArrayList<>();
            for (JsonNode n : node.get("matchedIndexes")) {
                if (n.isInt()) {
                    int idx = n.asInt();
                    if (idx >= 0 && idx < resources.size()) {
                        indexes.add(idx);
                    }
                }
            }
            log.info("Parsed AI match indexes: {}", indexes);

            // FIX 2: Empty array from AI = valid "no match" — do NOT fallback
            List<ResourceResponseDTO> result = indexes.stream()
                    .map(resources::get)
                    .map(resourceMapper::toDTO)
                    .collect(Collectors.toList());

            log.info("AI search completed with {} matches", result.size());
            return result;

        } catch (Exception e) {
            log.error("AI search failed unexpectedly", e);
            return fallbackSearch(query);
        }
    }

    private List<ResourceResponseDTO> fallbackSearch(String query) {
        log.info("Fallback search triggered for query: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        String lq = query.toLowerCase();
        return resourceRepository.findByArchivedFalse().stream()
                .filter(r ->
                        (r.getName()      != null && r.getName().toLowerCase().contains(lq)) ||
                                (r.getAmenities() != null && r.getAmenities().toString().toLowerCase().contains(lq))
                )
                .map(resourceMapper::toDTO)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI — Analytics (uses real resource data from DB)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AnalyticsDTO getResourceAnalytics(UUID id) {
        Resource resource = getResourceEntityById(id);

        String resourceName     = resource.getName()     != null ? resource.getName()         : "Unknown";
        String resourceType     = resource.getType()     != null ? resource.getType().name()  : "Unknown";
        String resourceLocation = resource.getLocation() != null ? resource.getLocation()     : "Unknown";
        String resourceStatus   = resource.getStatus()   != null ? resource.getStatus().name(): "Unknown";
        int    capacity         = resource.getCapacity() != null ? resource.getCapacity()     : 0;
        String amenities        = resource.getAmenities()!= null ? resource.getAmenities().toString() : "[]";
        String tags             = resource.getTags()     != null ? resource.getTags().toString()      : "[]";
        String aiSummary        = resource.getAiSummary()!= null ? resource.getAiSummary()            : "";

        long totalSameType   = resourceRepository.findByTypeAndArchivedFalse(resource.getType()).size();
        long activeSameType  = resourceRepository.findByTypeAndArchivedFalse(resource.getType())
                .stream()
                .filter(r -> r.getStatus() != null && r.getStatus().name().equals("ACTIVE"))
                .count();
        double utilizationRate = totalSameType > 0
                ? Math.round((activeSameType * 100.0 / totalSameType) * 10.0) / 10.0
                : 0.0;

        log.info("AI Analytics for resource: {} | type: {} | status: {} | utilization: {}%",
                resourceName, resourceType, resourceStatus, utilizationRate);

        try {
            String prompt = "You are a campus resource analytics AI.\n\n"
                    + "Analyse the following real campus resource data and provide insights:\n\n"
                    + "Resource Name: "     + resourceName     + "\n"
                    + "Type: "              + resourceType     + "\n"
                    + "Location: "          + resourceLocation + "\n"
                    + "Capacity: "          + capacity         + "\n"
                    + "Current Status: "    + resourceStatus   + "\n"
                    + "Amenities: "         + amenities        + "\n"
                    + "Tags: "              + tags             + "\n"
                    + "AI Summary: "        + aiSummary        + "\n"
                    + "Active resources of same type: " + activeSameType + " out of " + totalSameType + "\n"
                    + "Utilization rate of this resource type: " + utilizationRate + "%\n\n"
                    + "Based on this real data, return ONLY valid JSON:\n"
                    + "{\"insight\": \"<insight about this resource usage and demand>\", "
                    + "\"prediction\": \"<prediction about future demand for this resource>\"}\n\n"
                    + "Rules:\n* Base your response on the actual data above\n* No explanation\n* No markdown\n* Only JSON output";

            String aiResp = aiService.callAI(prompt);
            log.info("AI RAW RESPONSE (Analytics): {}", aiResp);

            if (aiResp == null || aiResp.trim().isEmpty()
                    || aiResp.length() < 5
                    || !aiResp.contains("{")) {
                log.warn("AI analytics response missing JSON. Triggering fallback.");
                return getFallbackAnalytics(resource, utilizationRate);
            }

            String cleanJson = safeExtractJson(aiResp);
            if ("{}".equals(cleanJson)) {
                log.warn("safeExtractJson returned empty object for analytics. Triggering fallback.");
                return getFallbackAnalytics(resource, utilizationRate);
            }

            JsonNode node;
            try {
                node = objectMapper.readTree(cleanJson);
            } catch (Exception ex) {
                log.error("Failed to parse AI analytics JSON", ex);
                return getFallbackAnalytics(resource, utilizationRate);
            }

            if (node == null || node.isMissingNode()
                    || !node.has("insight")    || node.get("insight").isNull()    || node.get("insight").isMissingNode()
                    || !node.has("prediction") || node.get("prediction").isNull() || node.get("prediction").isMissingNode()) {
                log.warn("AI analytics response missing required fields. Triggering fallback.");
                return getFallbackAnalytics(resource, utilizationRate);
            }

            String insight    = node.get("insight").asText("").trim();
            String prediction = node.get("prediction").asText("").trim();

            if (insight.isEmpty() || prediction.isEmpty()) {
                log.warn("AI analytics fields are blank. Triggering fallback.");
                return getFallbackAnalytics(resource, utilizationRate);
            }

            log.info("AI Analytics completed successfully for resource: {}", resourceName);
            return AnalyticsDTO.builder()
                    .totalBookings(0)
                    .utilizationRate(utilizationRate)
                    .aiInsight(insight)
                    .prediction(prediction)
                    .build();

        } catch (Exception e) {
            log.error("AI Analytics failed unexpectedly", e);
            return getFallbackAnalytics(resource, utilizationRate);
        }
    }

    private AnalyticsDTO getFallbackAnalytics(Resource resource, double utilizationRate) {
        log.info("Fallback triggered for resource analytics");
        String status = resource.getStatus() != null ? resource.getStatus().name() : "Unknown";
        return AnalyticsDTO.builder()
                .totalBookings(0)
                .utilizationRate(utilizationRate)
                .aiInsight("Resource '" + resource.getName() + "' is currently " + status
                        + " with a type utilization rate of " + utilizationRate + "%.")
                .prediction("Demand is expected to remain consistent based on current resource availability.")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AI — Recommendations
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AiRecommendationResponseDTO getAiRecommendations(AiRecommendationRequestDTO dto) {
        Resource sourceResource = getResourceEntityById(dto.getResourceId());

        List<Resource> available = resourceRepository
                .findByTypeAndArchivedFalse(sourceResource.getType())
                .stream()
                .filter(r -> !r.getId().equals(sourceResource.getId()))
                .collect(Collectors.toList());

        try {
            List<Map<String, Object>> alternativesList = available.stream()
                    .map(r -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id",       r.getId()       != null ? r.getId().toString()  : "");
                        m.put("name",     r.getName()     != null ? r.getName()            : "");
                        m.put("location", r.getLocation() != null ? r.getLocation()        : "");
                        return m;
                    })
                    .collect(Collectors.toList());

            String availableJson = objectMapper.writeValueAsString(alternativesList);

            String prompt = "Requested resource:\n"
                    + sourceResource.getName() + " at " + sourceResource.getLocation()
                    + "\n\nAvailable alternatives:\n" + availableJson
                    + "\n\nUser preference:\n- date: " + dto.getDate()
                    + "\n- timeRange: " + dto.getTimeRange()
                    + "\n\nBriefly explain why these are good alternatives in 1 sentence."
                    + " Give no markdown formatting or prefix.";

            String aiResponse = aiService.callAI(prompt);
            log.info("AI RAW RESPONSE (Recommendation): {}", aiResponse);

            if (aiResponse == null || aiResponse.trim().isEmpty()
                    || aiResponse.trim().startsWith("```")
                    || aiResponse.length() < 5) {
                log.warn("Invalid AI Recommendation output. Triggering fallback.");
                return getFallbackRecommendation(available);
            }

            log.info("AI Recommendation completed successfully");
            return AiRecommendationResponseDTO.builder()
                    .recommendation(aiResponse.trim())
                    .suggestions(available.stream()
                            .limit(3)
                            .map(resourceMapper::toDTO)
                            .collect(Collectors.toList()))
                    .build();

        } catch (Exception e) {
            log.error("AI Recommendation failed unexpectedly", e);
            return getFallbackRecommendation(available);
        }
    }

    private AiRecommendationResponseDTO getFallbackRecommendation(List<Resource> available) {
        log.info("Fallback triggered for recommendations");
        return AiRecommendationResponseDTO.builder()
                .recommendation("These alternatives are highly rated and match your base prerequisites.")
                .suggestions(available.stream()
                        .limit(3)
                        .map(resourceMapper::toDTO)
                        .collect(Collectors.toList()))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String safeExtractJson(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }
        try {
            String cleaned = response
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*",      "")
                    .trim();

            int start = cleaned.indexOf("{");
            int end   = cleaned.lastIndexOf("}");
            if (start != -1 && end != -1 && start <= end) {
                return cleaned.substring(start, end + 1);
            }
        } catch (Exception e) {
            log.error("Failed to extract JSON from AI response", e);
        }
        return "{}";
    }
}