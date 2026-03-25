package com.example.resume_analyzer_backend.repository;

import com.example.resume_analyzer_backend.entity.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EducationRepository extends JpaRepository<Education, Long> {
    List<Education> findByResumeIdOrderByEndYearDesc(Long resumeId);
    void deleteByResumeId(Long resumeId);
}