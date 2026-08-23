package com.careeradvisor.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartAdminConversationRequest {
    @NotBlank(message = "Subject is required")
    private String subject;

    private String initialMessage;
}
