package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.AssessmentSession;
import com.careeradvisor.backend.model.AssessmentSessionQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentSessionQuestionRepository extends JpaRepository<AssessmentSessionQuestion, Long> {

    List<AssessmentSessionQuestion> findBySessionOrderByPresentationOrderAsc(AssessmentSession session);

    @Query("SELECT sq FROM AssessmentSessionQuestion sq JOIN FETCH sq.question JOIN FETCH sq.technology WHERE sq.session.id = :sessionId ORDER BY sq.presentationOrder ASC")
    List<AssessmentSessionQuestion> findBySessionIdWithQuestionAndTech(@Param("sessionId") Long sessionId);

    Optional<AssessmentSessionQuestion> findBySessionIdAndPresentationOrder(Long sessionId, int presentationOrder);

    Optional<AssessmentSessionQuestion> findBySessionIdAndQuestionId(Long sessionId, Long questionId);

    List<AssessmentSessionQuestion> findBySessionIdAndTechnologyIdOrderByPresentationOrderAsc(Long sessionId, Long technologyId);

    int countBySessionId(Long sessionId);

    int countBySessionIdAndTechnologyId(Long sessionId, Long technologyId);
}
