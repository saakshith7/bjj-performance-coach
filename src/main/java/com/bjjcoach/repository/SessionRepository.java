package com.bjjcoach.repository;

import com.bjjcoach.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRepository extends JpaRepository<Session,String> {
    List<Session> findByUserIdOrderBySessionDateDesc(String userId);
}
