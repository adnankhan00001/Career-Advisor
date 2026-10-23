package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.EvidenceType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitEvidenceRequest {
    private Long stepId;

    @NotNull(message = "Evidence type is required")
    private EvidenceType evidenceType;

    private String url;

    private String notes;
}
