package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.AnswerStatus;
import com.careeradvisor.backend.model.AssessmentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentAnswerRepository extends JpaRepository<AssessmentAnswer, Long> {

    List<AssessmentAnswer> findBySessionId(Long sessionId);

    Optional<AssessmentAnswer> findBySessionQuestionId(Long sessionQuestionId);

    @Query("SELECT a FROM AssessmentAnswer a WHERE a.session.id = :sessionId AND a.sessionQuestion.presentationOrder = :order")
    Optional<AssessmentAnswer> findBySessionIdAndPresentationOrder(@Param("sessionId") Long sessionId, @Param("order") int order);

    int countBySessionIdAndStatus(Long sessionId, AnswerStatus status);
}
