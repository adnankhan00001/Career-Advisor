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
public class RealTimeEventDto {
    private String type; // MESSAGE_SENT, MESSAGE_DELIVERED, MESSAGE_READ, TYPING_STARTED, TYPING_STOPPED, USER_ONLINE, USER_OFFLINE
    private Long conversationId;
    private Long messageId;
    private Long senderId;
    private String senderName;
    private String senderRole;
    private String content;
    private String status;
    private Integer sequenceNumber;
    private LocalDateTime timestamp;
}
