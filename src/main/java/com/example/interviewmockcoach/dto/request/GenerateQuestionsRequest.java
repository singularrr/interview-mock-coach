package com.example.interviewmockcoach.dto.request;

import com.example.interviewmockcoach.dto.common.CandidateProfileDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQuestionsRequest {

    @NotNull(message = "candidateProfile不能为空")
    @Valid
    private CandidateProfileDto candidateProfile;

    @Min(value = 5, message = "问题数量不能少于5")
    @Max(value = 12, message = "问题数量不能大于12")
    private Integer questionCount = 8;
}
