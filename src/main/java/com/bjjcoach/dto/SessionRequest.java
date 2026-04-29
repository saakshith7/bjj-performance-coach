package com.bjjcoach.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SessionRequest {

    @NotNull
    private LocalDate sessionDate;

    private Integer durationMinutes;
    private String sessionType = "gi";

    private Integer energyLevel;
    private String notes;


}
