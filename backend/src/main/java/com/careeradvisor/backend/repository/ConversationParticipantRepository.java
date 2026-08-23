package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.Conversation;
import com.careeradvisor.backend.model.ConversationParticipant;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    List<ConversationParticipant> findByConversation(Conversation conversation);

    List<ConversationParticipant> findByUser(User user);

    Optional<ConversationParticipant> findByConversationAndUser(Conversation conversation, User user);

    boolean existsByConversationAndUser(Conversation conversation, User user);

    Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);

    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    long countByConversation(Conversation conversation);

    void deleteByConversation(Conversation conversation);
}
