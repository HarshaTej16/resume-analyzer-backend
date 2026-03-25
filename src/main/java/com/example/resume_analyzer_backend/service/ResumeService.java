package com.example.resume_analyzer_backend.service;

import com.example.resume_analyzer_backend.dto.response.ResumeAnalysisResponse;
import com.example.resume_analyzer_backend.entity.*;
import com.example.resume_analyzer_backend.exception.ResourceNotFoundException;
import com.example.resume_analyzer_backend.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private final ResumeRepository         resumeRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final SkillRepository          skillRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final EducationRepository      educationRepository;
    private final UserRepository           userRepository;
    private final FileParsingService       fileParsingService;
    private final ScoringService           scoringService;
    private final ObjectMapper             objectMapper;

    // ── Upload ────────────────────────────────────────────────────
    @Transactional
    public Resume uploadResume(MultipartFile file, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String filePath = fileParsingService.saveFile(file, userId);
        String fileType = fileParsingService.getExtension(file.getOriginalFilename());

        Resume resume = Resume.builder()
                .user(user)
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .fileSize(file.getSize())
                .fileType(fileType)
                .status(Resume.Status.PENDING)
                .build();

        return resumeRepository.save(resume);
    }

    // ── Async analysis ────────────────────────────────────────────
    @Async
    @Transactional
    public void analyzeResume(Long resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));
        try {
            resume.setStatus(Resume.Status.PROCESSING);
            resumeRepository.save(resume);

            // 1. Extract raw text
            String text = fileParsingService.extractText(
                    resume.getFilePath(), resume.getFileType());
            resume.setRawText(text);

            // 2. Score all dimensions
            Map<String, BigDecimal> scores = scoringService.scoreAll(text);

            // 3. Extract and save skills
            List<Map<String, String>> skillMaps = scoringService.extractSkills(text);
            List<Skill> skills = skillMaps.stream().map(sm -> Skill.builder()
                    .resume(resume)
                    .skillName(sm.get("name"))
                    .skillCategory(Skill.Category.valueOf(sm.get("category")))
                    .proficiency(Skill.Proficiency.valueOf(sm.get("proficiency")))
                    .build()
            ).toList();
            skillRepository.saveAll(skills);

            // 4. Extract and save work experience
            List<Map<String, Object>> expMaps = scoringService.extractExperience(text);
            List<WorkExperience> experiences = expMaps.stream().map(em -> {
                WorkExperience we = new WorkExperience();
                we.setResume(resume);
                we.setCompanyName((String) em.getOrDefault("company", "Unknown"));
                we.setJobTitle((String) em.getOrDefault("title", "Unknown"));
                we.setDescription((String) em.getOrDefault("description", ""));
                we.setIsCurrent(Boolean.TRUE.equals(em.get("isCurrent")));
                Object dur = em.get("durationMonths");
                we.setDurationMonths(dur instanceof Integer ? (Integer) dur : 0);
                return we;
            }).toList();
            workExperienceRepository.saveAll(experiences);

            // 5. Extract and save education
            List<Map<String, Object>> eduMaps = scoringService.extractEducation(text);
            List<Education> educations = eduMaps.stream().map(em -> {
                Education edu = new Education();
                edu.setResume(resume);
                edu.setInstitution((String) em.getOrDefault("institution", "Unknown"));
                edu.setDegree((String) em.getOrDefault("degree", "Unknown"));
                edu.setFieldOfStudy((String) em.getOrDefault("fieldOfStudy", ""));
                Object sy = em.get("startYear");
                Object ey = em.get("endYear");
                edu.setStartYear(sy instanceof Integer ? (Integer) sy : null);
                edu.setEndYear(ey instanceof Integer ? (Integer) ey : null);
                edu.setIsCompleted(true);
                return edu;
            }).toList();
            educationRepository.saveAll(educations);

            // 6. Extract candidate info
            String     name     = scoringService.extractName(text);
            String     email    = scoringService.extractEmail(text);
            String     phone    = scoringService.extractPhone(text);
            BigDecimal years    = scoringService.extractYearsOfExperience(text);
            String     eduLevel = scoringService.detectEducationLevel(text);
            String     industry = scoringService.detectIndustry(text);
            AnalysisResult.ReadinessLevel level =
                    scoringService.determineLevel(scores.get("overall"));

            // 7. Generate summary, strengths, improvements
            String summary = scoringService.generateSummary(
                    name, years, industry, level.name());
            List<String> strengths =
                    scoringService.generateStrengths(scores, skillMaps);
            List<Map<String, Object>> improvements =
                    scoringService.generateImprovements(scores, text);

            // 8. Save analysis result
            AnalysisResult result = AnalysisResult.builder()
                    .resume(resume)
                    .overallScore(scores.get("overall"))
                    .skillsScore(scores.get("skills"))
                    .experienceScore(scores.get("experience"))
                    .educationScore(scores.get("education"))
                    .formattingScore(scores.get("formatting"))
                    .keywordsScore(scores.get("keywords"))
                    .atsCompatibilityScore(scores.get("atsCompatibility"))
                    .readinessLevel(level)
                    .candidateName(name)
                    .candidateEmail(email)
                    .candidatePhone(phone)
                    .yearsOfExperience(years)
                    .educationLevel(eduLevel)
                    .primaryIndustry(industry)
                    .summaryText(summary)
                    .strengthsJson(objectMapper.writeValueAsString(strengths))
                    .improvementsJson(objectMapper.writeValueAsString(improvements))
                    .build();

            analysisResultRepository.save(result);

            // 9. Mark complete
            resume.setStatus(Resume.Status.ANALYZED);
            resume.setAnalyzedAt(LocalDateTime.now());
            resumeRepository.save(resume);

            log.info("Resume {} analysed. Score: {}", resumeId, scores.get("overall"));

        } catch (Exception e) {
            log.error("Analysis failed for resume {}: {}", resumeId, e.getMessage(), e);
            resume.setStatus(Resume.Status.FAILED);
            resumeRepository.save(resume);
        }
    }

    // ── Get analysis (returns status if not done yet) ─────────────
    @Transactional(readOnly = true)
    public ResumeAnalysisResponse getAnalysis(Long resumeId, Long userId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));

        if (resume.getStatus() == Resume.Status.PENDING
                || resume.getStatus() == Resume.Status.PROCESSING) {
            return ResumeAnalysisResponse.builder()
                    .resumeId(resume.getId())
                    .fileName(resume.getFileName())
                    .status(resume.getStatus().name())
                    .uploadedAt(resume.getUploadedAt())
                    .build();
        }

        if (resume.getStatus() == Resume.Status.FAILED) {
            return ResumeAnalysisResponse.builder()
                    .resumeId(resume.getId())
                    .fileName(resume.getFileName())
                    .status("FAILED")
                    .uploadedAt(resume.getUploadedAt())
                    .build();
        }

        AnalysisResult ar = analysisResultRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));

        List<Skill>          skills = skillRepository.findByResumeId(resumeId);
        List<WorkExperience> exps   = workExperienceRepository
                .findByResumeIdOrderByStartDateDesc(resumeId);
        List<Education>      edus   = educationRepository
                .findByResumeIdOrderByEndYearDesc(resumeId);

        return mapToResponse(resume, ar, skills, exps, edus);
    }

    // ── Get all resumes for user ──────────────────────────────────
    @Transactional(readOnly = true)
    public List<ResumeAnalysisResponse> getUserResumes(Long userId) {
        return resumeRepository.findByUserIdOrderByUploadedAtDesc(userId)
                .stream()
                .map(r -> {
                    ResumeAnalysisResponse.ResumeAnalysisResponseBuilder b =
                            ResumeAnalysisResponse.builder()
                                    .resumeId(r.getId())
                                    .fileName(r.getFileName())
                                    .status(r.getStatus().name())
                                    .uploadedAt(r.getUploadedAt())
                                    .analyzedAt(r.getAnalyzedAt());
                    if (r.getAnalysisResult() != null) {
                        b.overallScore(r.getAnalysisResult().getOverallScore())
                         .readinessLevel(r.getAnalysisResult().getReadinessLevel().name())
                         .primaryIndustry(r.getAnalysisResult().getPrimaryIndustry());
                    }
                    return b.build();
                }).toList();
    }

    // ── Delete ────────────────────────────────────────────────────
    @Transactional
    public void deleteResume(Long resumeId, Long userId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));
        fileParsingService.deleteFile(resume.getFilePath());
        resumeRepository.delete(resume);
    }

    // ── Map to response DTO ───────────────────────────────────────
    private ResumeAnalysisResponse mapToResponse(
            Resume resume, AnalysisResult ar,
            List<Skill> skills,
            List<WorkExperience> exps,
            List<Education> edus) {

        // Skills
        List<ResumeAnalysisResponse.SkillDto> skillDtos = skills.stream()
                .map(s -> ResumeAnalysisResponse.SkillDto.builder()
                        .name(s.getSkillName())
                        .category(s.getSkillCategory().name())
                        .proficiency(s.getProficiency().name())
                        .build())
                .toList();

        // Experience
        List<ResumeAnalysisResponse.ExperienceDto> expDtos = exps.stream()
                .map(e -> ResumeAnalysisResponse.ExperienceDto.builder()
                        .company(e.getCompanyName())
                        .title(e.getJobTitle())
                        .location(e.getLocation())
                        .startDate(e.getStartDate() != null
                                ? e.getStartDate().toString() : null)
                        .endDate(e.getEndDate() != null
                                ? e.getEndDate().toString() : "Present")
                        .isCurrent(Boolean.TRUE.equals(e.getIsCurrent()))
                        .durationMonths(e.getDurationMonths() != null
                                ? e.getDurationMonths() : 0)
                        .highlights(List.of(
                                e.getDescription() != null
                                        ? e.getDescription() : ""))
                        .build())
                .toList();

        // Education
        List<ResumeAnalysisResponse.EducationDto> eduDtos = edus.stream()
                .map(e -> ResumeAnalysisResponse.EducationDto.builder()
                        .institution(e.getInstitution())
                        .degree(e.getDegree())
                        .fieldOfStudy(e.getFieldOfStudy())
                        .startYear(e.getStartYear())
                        .endYear(e.getEndYear())
                        .gpa(e.getGpa())
                        .build())
                .toList();

        // Improvements
        List<ResumeAnalysisResponse.ImprovementDto> improvDtos = new ArrayList<>();
        try {
            if (ar.getImprovementsJson() != null) {
                List<Map<String, Object>> raw = objectMapper.readValue(
                        ar.getImprovementsJson(), new TypeReference<>() {});
                improvDtos = raw.stream()
                        .map(m -> ResumeAnalysisResponse.ImprovementDto.builder()
                                .category((String) m.get("category"))
                                .priority((String) m.get("priority"))
                                .suggestion((String) m.get("suggestion"))
                                .impactScore(m.get("impactScore") instanceof Number n
                                        ? n.doubleValue() : 0.0)
                                .build())
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Could not parse improvements: {}", e.getMessage());
        }

        // Strengths
        List<String> strengths = new ArrayList<>();
        try {
            if (ar.getStrengthsJson() != null) {
                strengths = objectMapper.readValue(
                        ar.getStrengthsJson(), new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("Could not parse strengths: {}", e.getMessage());
        }

        return ResumeAnalysisResponse.builder()
                .resumeId(resume.getId())
                .fileName(resume.getFileName())
                .status(resume.getStatus().name())
                .uploadedAt(resume.getUploadedAt())
                .analyzedAt(resume.getAnalyzedAt())
                .overallScore(ar.getOverallScore())
                .readinessLevel(ar.getReadinessLevel().name())
                .skillsScore(ar.getSkillsScore())
                .experienceScore(ar.getExperienceScore())
                .educationScore(ar.getEducationScore())
                .formattingScore(ar.getFormattingScore())
                .keywordsScore(ar.getKeywordsScore())
                .atsCompatibilityScore(ar.getAtsCompatibilityScore())
                .candidateName(ar.getCandidateName())
                .candidateEmail(ar.getCandidateEmail())
                .candidatePhone(ar.getCandidatePhone())
                .candidateLocation(ar.getCandidateLocation())
                .candidateLinkedIn(ar.getCandidateLinkedIn())
                .candidateGitHub(ar.getCandidateGitHub())
                .yearsOfExperience(ar.getYearsOfExperience())
                .educationLevel(ar.getEducationLevel())
                .primaryIndustry(ar.getPrimaryIndustry())
                .summaryText(ar.getSummaryText())
                .skills(skillDtos)
                .experience(expDtos)
                .education(eduDtos)
                .improvements(improvDtos)
                .strengths(strengths)
                .build();
    }
}