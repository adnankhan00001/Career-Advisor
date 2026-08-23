package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.Conversation;
import com.careeradvisor.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByOwnerAndArchivedFalseOrderByUpdatedAtDesc(User owner);

    List<Conversation> findByOwnerOrderByUpdatedAtDesc(User owner);

    Optional<Conversation> findByIdAndOwner(Long id, User owner);

    long countByOwner(User owner);

    List<Conversation> findByTypeOrderByUpdatedAtDesc(com.careeradvisor.backend.model.ConversationType type);

    Optional<Conversation> findByIdAndType(Long id, com.careeradvisor.backend.model.ConversationType type);
}
