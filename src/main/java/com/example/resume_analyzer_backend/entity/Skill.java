package com.example.resume_analyzer_backend.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "skills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Resume resume;

    @Column(name = "skill_name", nullable = false, length = 200)
    private String skillName;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_category")
    @Builder.Default
    private Category skillCategory = Category.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(name = "proficiency")
    @Builder.Default
    private Proficiency proficiency = Proficiency.INTERMEDIATE;

    public enum Category {
        LANGUAGE, FRAMEWORK, TOOL, DATABASE, CLOUD, SOFT, OTHER
    }

    public enum Proficiency {
        BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    }
}