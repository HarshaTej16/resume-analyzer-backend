package com.example.resume_analyzer_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", unique = true, nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Resume resume;

    // BigDecimal fields — precision/scale is fine here
    @Column(name = "overall_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal overallScore = BigDecimal.ZERO;

    @Column(name = "skills_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal skillsScore = BigDecimal.ZERO;

    @Column(name = "experience_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal experienceScore = BigDecimal.ZERO;

    @Column(name = "education_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal educationScore = BigDecimal.ZERO;

    @Column(name = "formatting_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal formattingScore = BigDecimal.ZERO;

    @Column(name = "keywords_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal keywordsScore = BigDecimal.ZERO;

    @Column(name = "ats_compatibility_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal atsCompatibilityScore = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "readiness_level")
    @Builder.Default
    private ReadinessLevel readinessLevel = ReadinessLevel.ENTRY;

    @Column(name = "candidate_name")
    private String candidateName;

    @Column(name = "candidate_email")
    private String candidateEmail;

    @Column(name = "candidate_phone", length = 50)
    private String candidatePhone;

    @Column(name = "candidate_location")
    private String candidateLocation;

    @Column(name = "candidate_linkedin")
    private String candidateLinkedIn;

    @Column(name = "candidate_github")
    private String candidateGitHub;

    // BigDecimal — precision/scale is fine
    @Column(name = "years_of_experience", precision = 4, scale = 1)
    @Builder.Default
    private BigDecimal yearsOfExperience = BigDecimal.ZERO;

    @Column(name = "education_level", length = 100)
    private String educationLevel;

    @Column(name = "primary_industry", length = 100)
    private String primaryIndustry;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "certifications_json", columnDefinition = "TEXT")
    private String certificationsJson;

    @Column(name = "strengths_json", columnDefinition = "TEXT")
    private String strengthsJson;

    @Column(name = "improvements_json", columnDefinition = "TEXT")
    private String improvementsJson;

    @Column(name = "analyzed_at")
    @Builder.Default
    private LocalDateTime analyzedAt = LocalDateTime.now();

    public enum ReadinessLevel { ENTRY, JUNIOR, MID, SENIOR, EXPERT }
}