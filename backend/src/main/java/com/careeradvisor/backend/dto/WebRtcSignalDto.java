package com.careeradvisor.backend.dto;

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
public class WebRtcSignalDto {

    private String type;
    private Long callId;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private Long targetUserId;
    private CallType callType;
    private String sdp;
    private Object candidate;
    private EndReason endReason;
    private Integer durationSeconds;
    private LocalDateTime timestamp;
}
