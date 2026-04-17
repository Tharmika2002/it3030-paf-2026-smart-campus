package com.sliit.smartcampus.resource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationRequestDTO {
    @NotNull(message = "Resource ID is required")
    private UUID resourceId;

    @NotBlank(message = "Date is required")
    private String date;

    @NotBlank(message = "Time range is required")
    private String timeRange;
}
