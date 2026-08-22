package com.careeradvisor.backend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMessageDto {
    private String role; // "system", "user", "assistant"
    private String content;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
