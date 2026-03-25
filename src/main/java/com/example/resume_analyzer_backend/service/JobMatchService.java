package com.example.resume_analyzer_backend.service;

import com.example.resume_analyzer_backend.dto.request.JobMatchRequest;
import com.example.resume_analyzer_backend.dto.response.JobMatchResponse;
import com.example.resume_analyzer_backend.entity.AnalysisResult;
import com.example.resume_analyzer_backend.entity.Resume;
import com.example.resume_analyzer_backend.entity.Skill;
import com.example.resume_analyzer_backend.exception.ResourceNotFoundException;
import com.example.resume_analyzer_backend.repository.AnalysisResultRepository;
import com.example.resume_analyzer_backend.repository.ResumeRepository;
import com.example.resume_analyzer_backend.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobMatchService {

    private final ResumeRepository         resumeRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final SkillRepository          skillRepository;

    private static final List<String> KNOWN_SKILLS = List.of(
            "Java", "Python", "JavaScript", "TypeScript", "Kotlin", "Go",
            "C++", "C#", "Swift", "Ruby", "PHP", "Scala", "Dart",
            "Spring Boot", "Spring", "React", "Angular", "Vue", "Node.js",
            "Django", "FastAPI", "Flask", "Next.js", "Express", "Laravel",
            "Docker", "Kubernetes", "Jenkins", "Git", "Maven", "Gradle",
            "Terraform", "Ansible", "CI/CD",
            "MySQL", "PostgreSQL", "MongoDB", "Redis", "Elasticsearch",
            "Oracle", "SQL Server", "Cassandra", "DynamoDB",
            "AWS", "Azure", "GCP", "Firebase",
            "Linux", "Agile", "Scrum",
            "Machine Learning", "Deep Learning", "TensorFlow", "PyTorch",
            "Microservices", "REST API", "GraphQL", "gRPC",
            "Flutter", "Android", "iOS", "React Native",
            "Blockchain", "Solidity", "Web3"
    );

    @Transactional(readOnly = true)
    public JobMatchResponse match(Long resumeId, Long userId, JobMatchRequest request) {

        // Verify resume exists and belongs to user
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));

        // Check analysis is complete
        if (resume.getStatus() != Resume.Status.ANALYZED) {
            throw new IllegalArgumentException(
                    "Resume analysis is not complete yet. Please wait for analysis to finish.");
        }

        // Load analysis result — use Optional to avoid crash
        Optional<AnalysisResult> arOpt = analysisResultRepository.findByResumeId(resumeId);

        // Candidate skills from skill table
        List<Skill> candidateSkills = skillRepository.findByResumeId(resumeId);
        Set<String> candidateSkillNames = candidateSkills.stream()
                .map(s -> s.getSkillName().toLowerCase())
                .collect(Collectors.toSet());

        // Also extract skills from raw text if skill table is empty
        if (candidateSkillNames.isEmpty() && resume.getRawText() != null) {
            String rawLower = resume.getRawText().toLowerCase();
            KNOWN_SKILLS.forEach(sk -> {
                if (rawLower.contains(sk.toLowerCase()))
                    candidateSkillNames.add(sk.toLowerCase());
            });
        }

        // Skills required by job description
        String jd       = request.getJobDescription();
        String jdLower  = jd.toLowerCase();
        List<String> jdSkills = KNOWN_SKILLS.stream()
                .filter(sk -> jdLower.contains(sk.toLowerCase()))
                .collect(Collectors.toList());

        // Matching and missing
        List<String> matching = jdSkills.stream()
                .filter(sk -> candidateSkillNames.contains(sk.toLowerCase()))
                .collect(Collectors.toList());
        List<String> missing = jdSkills.stream()
                .filter(sk -> !candidateSkillNames.contains(sk.toLowerCase()))
                .collect(Collectors.toList());

        // Scores
        double skillsMatch = jdSkills.isEmpty() ? 70.0
                : Math.min(100.0, (matching.size() / (double) jdSkills.size()) * 100.0);

        double experienceMatch = arOpt.map(ar -> calculateExperienceMatch(ar, jd))
                .orElse(60.0);

        double educationMatch = arOpt.map(ar -> calculateEducationMatch(ar, jd))
                .orElse(70.0);

        // Weighted overall
        double matchScore = (skillsMatch * 0.50)
                          + (experienceMatch * 0.30)
                          + (educationMatch  * 0.20);

        String fitLevel       = determineFitLevel(matchScore);
        String recommendation = buildRecommendation(fitLevel, matching, missing);

        log.info("Job match for resume {}: score={} fit={}",
                resumeId, Math.round(matchScore), fitLevel);

        return JobMatchResponse.builder()
                .matchScore(round(matchScore))
                .skillsMatch(round(skillsMatch))
                .experienceMatch(round(experienceMatch))
                .educationMatch(round(educationMatch))
                .fitLevel(fitLevel)
                .recommendation(recommendation)
                .matchingSkills(matching)
                .missingSkills(missing)
                .jobTitle(request.getJobTitle())
                .company(request.getCompany())
                .build();
    }

    private double calculateExperienceMatch(AnalysisResult ar, String jd) {
        double candidateYears = ar.getYearsOfExperience() != null
                ? ar.getYearsOfExperience().doubleValue() : 0.0;

        double requiredYears = 0.0;
        Matcher m = Pattern.compile("(\\d+)\\+?\\s+years?",
                Pattern.CASE_INSENSITIVE).matcher(jd);
        if (m.find()) {
            try { requiredYears = Double.parseDouble(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }

        if (requiredYears == 0)              return 75.0;
        if (candidateYears >= requiredYears) return 100.0;
        if (candidateYears == 0)             return 30.0;
        return Math.min(100.0, (candidateYears / requiredYears) * 100.0);
    }

    private double calculateEducationMatch(AnalysisResult ar, String jd) {
        String jdLower  = jd.toLowerCase();
        String eduLevel = ar.getEducationLevel() != null
                ? ar.getEducationLevel().toLowerCase() : "";

        if (jdLower.contains("phd") || jdLower.contains("doctorate"))
            return eduLevel.contains("phd") ? 100.0 : 50.0;
        if (jdLower.contains("master") || jdLower.contains("msc"))
            return (eduLevel.contains("phd") || eduLevel.contains("master")) ? 100.0 : 65.0;
        if (jdLower.contains("bachelor") || jdLower.contains("degree"))
            return (eduLevel.contains("phd") || eduLevel.contains("master")
                    || eduLevel.contains("bachelor")) ? 100.0 : 60.0;
        return 80.0;
    }

    private String determineFitLevel(double score) {
        if (score >= 80) return "EXCELLENT";
        if (score >= 65) return "GOOD";
        if (score >= 45) return "FAIR";
        return "POOR";
    }

    private String buildRecommendation(String fitLevel,
                                        List<String> matching,
                                        List<String> missing) {
        return switch (fitLevel) {
            case "EXCELLENT" -> String.format(
                    "Strong match! The candidate meets key requirements with %d matching skills. Recommended for interview.",
                    matching.size());
            case "GOOD" -> String.format(
                    "Good fit with %d matching skills. Minor gaps in %s can be addressed quickly.",
                    matching.size(),
                    missing.isEmpty() ? "a few areas"
                            : String.join(", ", missing.subList(0, Math.min(2, missing.size()))));
            case "FAIR" -> String.format(
                    "Partial match with %d of the required skills. Key gaps: %s.",
                    matching.size(),
                    missing.isEmpty() ? "general areas"
                            : String.join(", ", missing.subList(0, Math.min(3, missing.size()))));
            default -> String.format(
                    "Low match. Candidate has only %d of the required skills. Significant upskilling needed.",
                    matching.size());
        };
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}