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
public class StartUserConversationRequest {
    @NotNull(message = "targetUserId is required")
    private Long targetUserId;

    private String initialMessage;
}
