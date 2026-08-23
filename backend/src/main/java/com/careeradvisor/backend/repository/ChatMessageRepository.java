package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.ChatMessage;
import com.careeradvisor.backend.model.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationOrderByCreatedAtAsc(Conversation conversation);

    @Query("SELECT m FROM ChatMessage m WHERE m.conversation = :conversation ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentMessagesByConversation(@Param("conversation") Conversation conversation, Pageable pageable);

    long countByConversation(Conversation conversation);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.conversation = :conversation")
    void deleteByConversation(@Param("conversation") Conversation conversation);
}
