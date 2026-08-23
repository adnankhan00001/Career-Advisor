package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conversations_owner", columnList = "user_id"),
    @Index(name = "idx_conversations_updated_at", columnList = "updated_at"),
    @Index(name = "idx_conversations_last_msg", columnList = "last_message_at"),
    @Index(name = "idx_conversations_owner_archived", columnList = "user_id, archived")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "conversation_type", nullable = false)
    private ConversationType type = ConversationType.USER_TO_AI;

    @Builder.Default
    @Column(nullable = false, length = 120)
    private String title = "New AI Conversation";

    @Builder.Default
    @Column(nullable = false)
    private boolean archived = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt = LocalDateTime.now();
}
