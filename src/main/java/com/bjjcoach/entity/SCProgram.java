package com.bjjcoach.entity;

import com.bjjcoach.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name ="sc_program")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SCProgram {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn (name = "user_id",nullable = false)
    private User user;

    @Column(name = "generated_at",updatable = false)
    private Instant generatedAt;

    @Column(name = "duration_weeks")
    private Integer durationWeeks;

    @Column(name = "fitness_level")
    private String fitnessLevel;

    @Column(name = "target_weaknesses",columnDefinition = "JSON")
    private String targetWeaknesses;

    @Column(name = "weekly_schedule", columnDefinition = "JSON")
    private String weeklySchedule;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    public void prePersist(){
        if(this.id == null){
            this.id=java.util.UUID.randomUUID().toString();
        }
        if(this.generatedAt == null){
            this.generatedAt = Instant.now();
        }
    }
}
