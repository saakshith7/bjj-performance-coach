package com.bjjcoach.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="position_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionLog {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="session_id",nullable = false)
    private Session session;

    @Column(nullable = false)
    private String position;

    private String outcome;

    private String role;

    @PrePersist
    public void prePersist(){
        if(this.id==null){
            this.id = java.util.UUID.randomUUID().toString();
        }
    }
}
