package com.bjjcoach.repository;

import com.bjjcoach.entity.TechniqueLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TechniqueLogRepository extends JpaRepository<TechniqueLog,String> {
    List<TechniqueLog> findBySessionId(String sessionId);
    List<TechniqueLog> findBySession_User_Id(String userId);
}
