package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.CallStatus;
import com.careeradvisor.backend.model.CallType;
import com.careeradvisor.backend.model.EndReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallSessionDto {

    private Long id;
    private Long conversationId;
    private Long callerId;
    private String callerName;
    private Long receiverId;
    private String receiverName;
    private CallType callType;
    private CallStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime answeredAt;
    private LocalDateTime endedAt;
    private Integer durationSeconds;
    private EndReason endReason;
    private LocalDateTime createdAt;
}
