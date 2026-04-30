package com.bjjcoach.entity;

import com.bjjcoach.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "weakness_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeaknessReport {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @Column(name = "generated_at",updatable = false)
    private Instant generatedAt;

    @Column(name = "total_sessions_analyzed")
    private Integer totalSessionsAnalyzed;

    @Column(name = "weak_positions",columnDefinition = "JSON")
    private String weakPositions;

    @Column(name = "weak_techniques", columnDefinition = "JSON")
    private String weakTechniques;

    @Column(name = "role_analysis", columnDefinition = "JSON")
    private String roleAnalysis;

    @Column(name = "cardio_flag")
    private Boolean cardioFlag;

    @Column(name = "recommendations", columnDefinition = "JSON")
    private String recommendations;

    @PrePersist
    public void prePersist(){
        if(this.id==null){
            this.id=java.util.UUID.randomUUID().toString();
        }
        if(this.generatedAt == null){
            this.generatedAt = Instant.now();
        }
    }

}
