package com.example.resume_analyzer_backend.repository;

import com.example.resume_analyzer_backend.entity.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {
    List<JobDescription> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<JobDescription> findByIdAndUserId(Long id, Long userId);
}