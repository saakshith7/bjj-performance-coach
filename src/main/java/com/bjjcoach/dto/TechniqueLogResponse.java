package com.bjjcoach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechniqueLogResponse {
    private String id;
    private String sessionId;
    private String technique;
    private String category;
    private Boolean success;
    private String notes;
}
