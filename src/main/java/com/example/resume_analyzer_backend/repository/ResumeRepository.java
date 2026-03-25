package com.example.resume_analyzer_backend.repository;

import com.example.resume_analyzer_backend.entity.Resume;
import com.example.resume_analyzer_backend.entity.Resume.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByUserIdOrderByUploadedAtDesc(Long userId);

    List<Resume> findByStatus(Status status);

    Optional<Resume> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT r FROM Resume r LEFT JOIN FETCH r.analysisResult WHERE r.id = :id")
    Optional<Resume> findByIdWithAnalysis(@Param("id") Long id);

    long countByUserId(Long userId);
}