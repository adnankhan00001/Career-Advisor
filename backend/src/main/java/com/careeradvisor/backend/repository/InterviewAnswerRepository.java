package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.InterviewAnswer;
import com.careeradvisor.backend.model.InterviewQuestion;
import com.careeradvisor.backend.model.MockInterview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {

    List<InterviewAnswer> findByInterview(MockInterview interview);

    Optional<InterviewAnswer> findByInterviewAndQuestion(MockInterview interview, InterviewQuestion question);

    long countByInterviewAndIsCorrectTrue(MockInterview interview);
}
