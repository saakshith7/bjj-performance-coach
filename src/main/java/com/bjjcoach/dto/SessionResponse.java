package com.bjjcoach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    private String id;
    private LocalDate sessionDate;
    private Integer durationMinutes;
    private String sessionType;
    private Integer energyLevel;
    private String notes;
    private Instant createdAt;
}
