package com.sliit.smartcampus.resource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationResponseDTO {
    private String recommendation;
    private List<ResourceResponseDTO> suggestions;
}
