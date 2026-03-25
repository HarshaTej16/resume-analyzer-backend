package com.example.resume_analyzer_backend.controller;

import com.example.resume_analyzer_backend.dto.request.JobMatchRequest;
import com.example.resume_analyzer_backend.dto.response.ApiResponse;
import com.example.resume_analyzer_backend.dto.response.JobMatchResponse;
import com.example.resume_analyzer_backend.dto.response.ResumeAnalysisResponse;
import com.example.resume_analyzer_backend.entity.Resume;
import com.example.resume_analyzer_backend.entity.User;
import com.example.resume_analyzer_backend.service.JobMatchService;
import com.example.resume_analyzer_backend.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
@Tag(name = "Resume", description = "Upload, analyse and manage resumes")
public class ResumeController {

    private final ResumeService  resumeService;
    private final JobMatchService jobMatchService;

    // ── Upload ────────────────────────────────────────────────────
    @PostMapping("/upload")
    @Operation(summary = "Upload a resume file (PDF / DOCX / TXT)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {

        Resume resume = resumeService.uploadResume(file, currentUser.getId());

        // Trigger async analysis (non-blocking)
        resumeService.analyzeResume(resume.getId());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                ApiResponse.success("Resume uploaded. Analysis in progress.", Map.of(
                        "resumeId", resume.getId(),
                        "fileName", resume.getFileName(),
                        "status",   resume.getStatus().name()
                ))
        );
    }

    // ── Get all resumes for current user ──────────────────────────
    @GetMapping
    @Operation(summary = "Get all resumes for the authenticated user")
    public ResponseEntity<ApiResponse<List<ResumeAnalysisResponse>>> getMyResumes(
            @AuthenticationPrincipal User currentUser) {

        List<ResumeAnalysisResponse> resumes =
                resumeService.getUserResumes(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(resumes));
    }

    // ── Get analysis for one resume ───────────────────────────────
    @GetMapping("/{id}/analysis")
    @Operation(summary = "Get the full analysis result for a resume")
    public ResponseEntity<ApiResponse<ResumeAnalysisResponse>> getAnalysis(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        ResumeAnalysisResponse analysis =
                resumeService.getAnalysis(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(analysis));
    }

    // ── Job match ─────────────────────────────────────────────────
    @PostMapping("/{id}/job-match")
    @Operation(summary = "Match a resume against a job description")
    public ResponseEntity<ApiResponse<JobMatchResponse>> jobMatch(
            @PathVariable Long id,
            @Valid @RequestBody JobMatchRequest request,
            @AuthenticationPrincipal User currentUser) {

        JobMatchResponse result =
                jobMatchService.match(id, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── Delete ────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a resume and its analysis")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        resumeService.deleteResume(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Resume deleted successfully", null));
    }
}