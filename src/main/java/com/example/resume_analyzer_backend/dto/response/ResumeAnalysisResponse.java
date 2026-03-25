package com.example.resume_analyzer_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResumeAnalysisResponse {

    // ── Resume Info ───────────────────────────────────────────────
    private Long resumeId;
    private String fileName;
    private String status;
    private LocalDateTime uploadedAt;
    private LocalDateTime analyzedAt;

    // ── Scores ────────────────────────────────────────────────────
    private BigDecimal overallScore;
    private String readinessLevel;
    private BigDecimal skillsScore;
    private BigDecimal experienceScore;
    private BigDecimal educationScore;
    private BigDecimal formattingScore;
    private BigDecimal keywordsScore;
    private BigDecimal atsCompatibilityScore;

    // ── Candidate Info ────────────────────────────────────────────
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String candidateLocation;
    private String candidateLinkedIn;
    private String candidateGitHub;
    private BigDecimal yearsOfExperience;
    private String educationLevel;
    private String primaryIndustry;
    private String summaryText;

    // ── Parsed Data ───────────────────────────────────────────────
    private List<SkillDto>       skills;
    private List<ExperienceDto>  experience;
    private List<EducationDto>   education;
    private List<String>         certifications;
    private List<String>         strengths;
    private List<ImprovementDto> improvements;

    // ── Nested DTOs ───────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SkillDto {
        private String name;
        private String category;
        private String proficiency;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ExperienceDto {
        private String company;
        private String title;
        private String location;
        private String startDate;
        private String endDate;
        private boolean isCurrent;
        private int durationMonths;
        private List<String> highlights;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class EducationDto {
        private String institution;
        private String degree;
        private String fieldOfStudy;
        private Integer startYear;
        private Integer endYear;
        private Double gpa;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ImprovementDto {
        private String category;
        private String priority;
        private String suggestion;
        private Double impactScore;
    }
}