package com.example.interviewmockcoach.dto.common;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfileDto {

    @NotBlank(message = "学校不能为空")
    private String school;

    @NotBlank(message = "专业不能为空")
    private String major;

    @NotBlank(message = "研究方向不能为空")
    private String researchDirection;

    @NotBlank(message = "个人简历要点不能为空")
    private String resumePoints;
}
