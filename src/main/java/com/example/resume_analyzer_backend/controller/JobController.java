package com.example.resume_analyzer_backend.controller;

import com.example.resume_analyzer_backend.dto.response.ApiResponse;
import com.example.resume_analyzer_backend.entity.JobDescription;
import com.example.resume_analyzer_backend.entity.User;
import com.example.resume_analyzer_backend.exception.ResourceNotFoundException;
import com.example.resume_analyzer_backend.repository.JobDescriptionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Tag(name = "Job Descriptions", description = "Save and retrieve job descriptions for matching")
public class JobController {

    private final JobDescriptionRepository jobDescriptionRepository;

    // ── Create ────────────────────────────────────────────────────
    @PostMapping
    @Operation(summary = "Save a job description")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createJob(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {

        String title       = body.getOrDefault("title", "Untitled Role");
        String company     = body.getOrDefault("company", "");
        String description = body.get("description");

        if (description == null || description.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Job description content is required"));
        }

        JobDescription jd = JobDescription.builder()
                .user(currentUser)
                .title(title)
                .company(company)
                .description(description)
                .build();

        jd = jobDescriptionRepository.save(jd);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Job description saved", Map.of(
                        "id",      jd.getId(),
                        "title",   jd.getTitle(),
                        "company", jd.getCompany()
                ))
        );
    }

    // ── List all for current user ─────────────────────────────────
    @GetMapping
    @Operation(summary = "Get all saved job descriptions")
    public ResponseEntity<ApiResponse<List<JobDescription>>> getMyJobs(
            @AuthenticationPrincipal User currentUser) {

        List<JobDescription> jobs =
                jobDescriptionRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    // ── Get one ───────────────────────────────────────────────────
    @GetMapping("/{id}")
    @Operation(summary = "Get a specific saved job description")
    public ResponseEntity<ApiResponse<JobDescription>> getJob(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        JobDescription jd = jobDescriptionRepository
                .findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job description", id));

        return ResponseEntity.ok(ApiResponse.success(jd));
    }

    // ── Delete ────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a saved job description")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        JobDescription jd = jobDescriptionRepository
                .findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Job description", id));

        jobDescriptionRepository.delete(jd);
        return ResponseEntity.ok(ApiResponse.success("Job description deleted", null));
    }
}