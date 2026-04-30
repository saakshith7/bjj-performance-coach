package com.bjjcoach.repository;

import com.bjjcoach.entity.WeaknessReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WeaknessReportRepository extends JpaRepository<WeaknessReport,String> {

    @Query("SELECT w FROM WeaknessReport w WHERE w.user.id = :userId ORDER BY w.generatedAt DESC")
    List<WeaknessReport> findByUserIdOrderByGeneratedAtDesc(@Param("userId") String userId);

    @Query("SELECT w FROM WeaknessReport w WHERE w.user.id = :userId ORDER BY w.generatedAt DESC LIMIT 1")
    Optional<WeaknessReport> findTopByUserIdOrderByGeneratedAtDesc(@Param("userId") String userId);
}