package com.example.resume_analyzer_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JobMatchRequest {

    @NotBlank(message = "Job description is required")
    @Size(min = 50, message = "Job description must be at least 50 characters")
    private String jobDescription;

    private String jobTitle;
    private String company;
}