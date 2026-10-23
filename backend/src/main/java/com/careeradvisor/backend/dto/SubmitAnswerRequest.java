package com.careeradvisor.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {

    @NotNull(message = "Session question ID is required")
    private Long sessionQuestionId;

    @NotNull(message = "Selected option index is required")
    @Min(value = 0, message = "Selected option index must be non-negative")
    @Max(value = 5, message = "Selected option index is out of bounds")
    private Integer selectedOptionIndex;
}
