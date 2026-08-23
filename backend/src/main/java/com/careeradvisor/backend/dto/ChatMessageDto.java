package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private Long conversationId;
    private String senderType; // "USER", "AI", "ADMIN", "SYSTEM"
    private String senderName;
    private String content;
    private Integer sequenceNumber;
    private String status;
    private LocalDateTime createdAt;
}
