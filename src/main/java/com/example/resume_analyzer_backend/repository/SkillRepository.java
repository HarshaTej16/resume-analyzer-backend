package com.example.resume_analyzer_backend.repository;

import com.example.resume_analyzer_backend.entity.Skill;
import com.example.resume_analyzer_backend.entity.Skill.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByResumeId(Long resumeId);
    List<Skill> findByResumeIdAndSkillCategory(Long resumeId, Category category);
    void deleteByResumeId(Long resumeId);
}