package com.sliit.smartcampus.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequestDTO {
    private String message;
    private List<String> history;
}
