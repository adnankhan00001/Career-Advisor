package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.ParticipantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationParticipantDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private ParticipantRole role;
    private boolean online;
    private LocalDateTime joinedAt;
    private LocalDateTime lastReadAt;
}
