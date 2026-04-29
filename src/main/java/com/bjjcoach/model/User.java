package com.bjjcoach.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Builder.Default
    private String belt = "white";

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    private Integer age;

    @Builder.Default
    @Column(name = "training_days_per_week")
    private Integer trainingDaysPerWeek = 3;

    @Builder.Default
    @Column(name = "fitness_level")
    private String fitnessLevel = "beginner";

    @Builder.Default
    private String goal = "fitness";

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}

