package com.bjjcoach.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="technique_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechniqueLog {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id" , nullable = false)
    private Session session;

    @Column(nullable = false)
    private String technique;

    private String category;

    private Boolean success;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    public void prePersist(){
        if(this.id==null){
            this.id=java.util.UUID.randomUUID().toString();
        }
    }
}
