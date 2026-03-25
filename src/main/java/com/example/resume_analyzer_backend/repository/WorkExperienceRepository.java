package com.example.resume_analyzer_backend.repository;

import com.example.resume_analyzer_backend.entity.WorkExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkExperienceRepository extends JpaRepository<WorkExperience, Long> {
    List<WorkExperience> findByResumeIdOrderByStartDateDesc(Long resumeId);
    void deleteByResumeId(Long resumeId);
}