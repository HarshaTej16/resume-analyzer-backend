package com.example.resume_analyzer_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "education")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Resume resume;

    @Column(name = "institution", length = 300)
    private String institution;

    @Column(name = "degree", length = 300)
    private String degree;

    @Column(name = "field_of_study", length = 300)
    private String fieldOfStudy;

    @Column(name = "start_year")
    private Integer startYear;

    @Column(name = "end_year")
    private Integer endYear;

    // Double fields must NOT have precision/scale — only BigDecimal supports that
    @Column(name = "gpa")
    private Double gpa;

    @Column(name = "is_completed")
    @Builder.Default
    private Boolean isCompleted = true;
}