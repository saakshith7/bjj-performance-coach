package com.bjjcoach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SCProgramResponse {
    private String id;
    private Instant generatedAt;
    private Integer durationWeeks;
    private String fitnessLevel;
    private List<String> targetWeaknesses;
    private List<DayProgramDTO> weeklySchedule;
    private String notes;
    private String comparisonWithPrevious;
}
