package com.example.resume_analyzer_backend.controller;

import com.example.resume_analyzer_backend.dto.response.ApiResponse;
import com.example.resume_analyzer_backend.entity.AnalysisResult;
import com.example.resume_analyzer_backend.repository.AnalysisResultRepository;
import com.example.resume_analyzer_backend.repository.ResumeRepository;
import com.example.resume_analyzer_backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only statistics and management endpoints")
public class AdminController {

    private final UserRepository           userRepository;
    private final ResumeRepository         resumeRepository;
    private final AnalysisResultRepository analysisResultRepository;

    // ── Platform stats ────────────────────────────────────────────
    @GetMapping("/stats")
    @Operation(summary = "Get overall platform statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {

        long totalUsers    = userRepository.count();
        long totalResumes  = resumeRepository.count();
        long analysed      = resumeRepository.findByStatus(
                com.example.resume_analyzer_backend.entity.Resume.Status.ANALYZED).size();
        Double avgScore    = analysisResultRepository.findAverageScore();

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "totalUsers",     totalUsers,
                "totalResumes",   totalResumes,
                "analysedCount",  analysed,
                "averageScore",   avgScore != null ? Math.round(avgScore * 100.0) / 100.0 : 0.0
        )));
    }

    // ── Top candidates ────────────────────────────────────────────
    @GetMapping("/top-candidates")
    @Operation(summary = "Get top scored candidates")
    public ResponseEntity<ApiResponse<List<AnalysisResult>>> getTopCandidates() {
        List<AnalysisResult> top = analysisResultRepository
                .findTopCandidates()
                .stream()
                .limit(20)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(top));
    }

    // ── Candidates by level ───────────────────────────────────────
    @GetMapping("/candidates/level/{level}")
    @Operation(summary = "Get candidates by readiness level")
    public ResponseEntity<ApiResponse<List<AnalysisResult>>> getByLevel(
            @PathVariable String level) {

        AnalysisResult.ReadinessLevel readinessLevel =
                AnalysisResult.ReadinessLevel.valueOf(level.toUpperCase());

        List<AnalysisResult> results =
                analysisResultRepository.findByReadinessLevel(readinessLevel);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}