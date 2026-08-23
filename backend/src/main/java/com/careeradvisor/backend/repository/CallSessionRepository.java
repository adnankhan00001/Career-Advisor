package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.CallSession;
import com.careeradvisor.backend.model.CallStatus;
import com.careeradvisor.backend.model.Conversation;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CallSessionRepository extends JpaRepository<CallSession, Long> {

    List<CallSession> findByConversationOrderByCreatedAtDesc(Conversation conversation);

    @Query("SELECT c FROM CallSession c WHERE (c.caller.id = :userId OR c.receiver.id = :userId) AND c.status IN :statuses")
    List<CallSession> findActiveCallsForUser(@Param("userId") Long userId, @Param("statuses") List<CallStatus> statuses);

    @Query("SELECT c FROM CallSession c WHERE c.conversation.id = :conversationId AND c.status IN :statuses")
    List<CallSession> findActiveCallsInConversation(@Param("conversationId") Long conversationId, @Param("statuses") List<CallStatus> statuses);

    @Query("SELECT c FROM CallSession c WHERE c.caller = :user OR c.receiver = :user ORDER BY c.createdAt DESC")
    List<CallSession> findUserCallHistory(@Param("user") User user);

    List<CallSession> findByStatusAndStartedAtBefore(CallStatus status, LocalDateTime cutoff);

    Optional<CallSession> findByIdAndCallerOrIdAndReceiver(Long id1, User caller, Long id2, User receiver);
}
