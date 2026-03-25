package com.example.resume_analyzer_backend.repository;

import com.example.resume_analyzer_backend.entity.AnalysisResult;
import com.example.resume_analyzer_backend.entity.AnalysisResult.ReadinessLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    Optional<AnalysisResult> findByResumeId(Long resumeId);

    @Query("SELECT a FROM AnalysisResult a ORDER BY a.overallScore DESC")
    List<AnalysisResult> findTopCandidates();

    List<AnalysisResult> findByReadinessLevel(ReadinessLevel level);

    @Query("SELECT AVG(a.overallScore) FROM AnalysisResult a")
    Double findAverageScore();
}