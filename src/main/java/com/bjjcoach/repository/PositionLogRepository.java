package com.bjjcoach.repository;

import com.bjjcoach.entity.PositionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionLogRepository extends JpaRepository<PositionLog,String > {
    List<PositionLog> findBySessionId(String sessionId);
    List<PositionLog> findBySession_User_Id(String userId);
}
