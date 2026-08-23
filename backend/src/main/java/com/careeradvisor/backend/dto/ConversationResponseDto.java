package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponseDto {
    private Long id;
    private String title;
    private String conversationType;
    private boolean archived;
    private long messageCount;
    private long unreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastMessageAt;

    @Builder.Default
    private List<ConversationParticipantDto> participants = new ArrayList<>();

    @Builder.Default
    private List<ChatMessageDto> messages = new ArrayList<>();
}
