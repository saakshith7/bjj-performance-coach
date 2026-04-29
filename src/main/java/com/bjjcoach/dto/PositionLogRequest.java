package com.bjjcoach.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PositionLogRequest {

    @NotBlank
    private String position;

    private String outcome;
    private String role;
}
