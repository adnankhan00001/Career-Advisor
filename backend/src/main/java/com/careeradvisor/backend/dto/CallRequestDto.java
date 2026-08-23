package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.CallType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallRequestDto {

    @NotNull(message = "conversationId is required")
    private Long conversationId;

    @NotNull(message = "callType is required")
    private CallType callType;
}
