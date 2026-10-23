package com.careeradvisor.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkipQuestionRequest {

    @NotNull(message = "Session question ID is required")
    private Long sessionQuestionId;
}
