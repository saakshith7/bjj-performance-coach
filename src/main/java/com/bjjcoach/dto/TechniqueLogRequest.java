package com.bjjcoach.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TechniqueLogRequest {
    @NotBlank
    private String technique;

    private String category;
    private Boolean success;
    private String notes;
}

