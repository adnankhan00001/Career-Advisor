package com.careeradvisor.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_participants", uniqueConstraints = {
    @UniqueConstraint(name = "uk_conv_participant", columnNames = {"conversation_id", "user_id"})
}, indexes = {
    @Index(name = "idx_conv_part_user", columnList = "user_id, conversation_id"),
    @Index(name = "idx_conv_part_read", columnList = "user_id, last_read_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "participant_role", nullable = false)
    private ParticipantRole role = ParticipantRole.MEMBER;

    @Builder.Default
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt = LocalDateTime.now();
}
