package com.example.resume_analyzer_backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobMatchResponse {

    private Double matchScore;
    private Double skillsMatch;
    private Double experienceMatch;
    private Double educationMatch;
    private String fitLevel;          // POOR | FAIR | GOOD | EXCELLENT
    private String recommendation;

    private List<String> matchingSkills;
    private List<String> missingSkills;

    private String jobTitle;
    private String company;
}