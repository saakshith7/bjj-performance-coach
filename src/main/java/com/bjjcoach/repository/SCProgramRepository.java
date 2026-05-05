package com.bjjcoach.repository;

import com.bjjcoach.entity.SCProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SCProgramRepository extends JpaRepository<SCProgram,String> {
    @Query("SELECT s FROM SCProgram s WHERE s.user.id = :userId ORDER BY s.generatedAt DESC")
    List<SCProgram> findByUserIdOrderByGeneratedAtDesc(@Param("userId") String userId);

    @Query("SELECT s FROM SCProgram s WHERE s.user.id =  :userId ORDER BY s.generatedAt DESC LIMIT 1")
    Optional<SCProgram> findLatestByUserId(@Param("userId") String userId);
}
