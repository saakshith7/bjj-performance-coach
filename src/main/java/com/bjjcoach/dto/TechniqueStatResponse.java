package com.bjjcoach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechniqueStatResponse {
    private String technique;
    private String category;
    private int totalAttempts;
    private int successes;
    private int failures;
    private Double successRate;
    private String severity;   // HIGH, MEDIUM, LOW, OK
}
