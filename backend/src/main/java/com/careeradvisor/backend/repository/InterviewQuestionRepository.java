package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.Difficulty;
import com.careeradvisor.backend.model.InterviewQuestion;
import com.careeradvisor.backend.model.ProblemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    List<InterviewQuestion> findByCategoryOrderByOrderIndexAsc(ProblemCategory category);

    List<InterviewQuestion> findByCategoryAndDifficultyOrderByOrderIndexAsc(ProblemCategory category, Difficulty difficulty);

    List<InterviewQuestion> findAllByOrderByOrderIndexAsc();
}
