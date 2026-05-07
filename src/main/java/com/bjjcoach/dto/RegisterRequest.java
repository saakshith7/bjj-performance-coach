package com.bjjcoach.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank private String name;
    @Email @NotBlank private String email;
    @NotBlank @Size(min = 6) private String password;
    private String belt = "white";
    private java.math.BigDecimal weightKg;
    private Integer age;
    private Integer trainingDaysPerWeek = 3;
    private String fitnessLevel = "beginner";
    private String goal = "fitness";
}