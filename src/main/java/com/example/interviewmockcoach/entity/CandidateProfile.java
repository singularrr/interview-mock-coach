package com.example.interviewmockcoach.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class CandidateProfile {

    @Column(nullable = false, length = 128)
    private String school;

    @Column(nullable = false, length = 128)
    private String major;

    @Column(nullable = false, length = 256)
    private String researchDirection;

    @Column(nullable = false, length = 4000)
    private String resumePoints;
}
