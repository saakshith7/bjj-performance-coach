package com.bjjcoach.entity;

import com.bjjcoach.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @Column(name = "session_date",nullable = false)
    private LocalDate sessionDate;

    @Column(name = "duration_minutes",nullable = false)
    private Integer durationMinutes;

    @Column(name = "session_type")
    @Builder.Default
    private String sessionType = "gi";

    @Column(name = "energy_level")
    private Integer energyLevel;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at",updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist(){
        if(this.id==null){
            this.id=java.util.UUID.randomUUID().toString();
        }
        if(this.createdAt==null){
            this.createdAt=Instant.now();
        }
    }



}
