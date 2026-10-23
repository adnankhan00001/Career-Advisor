package com.careeradvisor.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AssessmentModelTests {

    @Test
    @DisplayName("AssessmentQuestion builder sets default type, weight, active, and timestamps")
    void testAssessmentQuestionDefaults() {
        Technology java = Technology.builder().name("Java").slug("java").build();

        AssessmentQuestion question = AssessmentQuestion.builder()
                .questionKey("java-oop-01")
                .technology(java)
                .topic("OOP Fundamentals")
                .difficulty(AssessmentDifficulty.BEGINNER)
                .questionText("What is polymorphism?")
                .optionsJson("[\"A\",\"B\",\"C\",\"D\"]")
                .correctOptionIndex(0)
                .explanation("Polymorphism allows dynamic method dispatch.")
                .build();

        assertEquals(AssessmentQuestionType.MULTIPLE_CHOICE, question.getQuestionType());
        assertEquals(1, question.getScoringWeight());
        assertTrue(question.isActive());
        assertNotNull(question.getCreatedAt());
        assertNotNull(question.getUpdatedAt());
        assertEquals("java-oop-01", question.getQuestionKey());
    }

    @Test
    @DisplayName("AssessmentSession builder initializes status, defaults, and expiration")
    void testAssessmentSessionDefaults() {
        User user = User.builder().id(101L).name("Sam Student").build();
        Technology react = Technology.builder().name("React").slug("react").build();
        Set<Technology> techs = new HashSet<>();
        techs.add(react);

        AssessmentSession session = AssessmentSession.builder()
                .user(user)
                .selectedTechnologies(techs)
                .timeLimitMinutes(25)
                .build();

        session.onCreate();

        assertEquals(AssessmentStatus.IN_PROGRESS, session.getStatus());
        assertEquals(25, session.getTimeLimitMinutes());
        assertEquals("v2.0-adaptive", session.getAssessmentVersion());
        assertNotNull(session.getStartTime());
        assertNotNull(session.getExpiresAt());
        assertTrue(session.getExpiresAt().isAfter(session.getStartTime()));
        assertEquals(1, session.getSelectedTechnologies().size());
    }

    @Test
    @DisplayName("AssessmentSessionQuestion builder sets order and scoring weight")
    void testAssessmentSessionQuestionDefaults() {
        AssessmentSession session = AssessmentSession.builder().id(1L).build();
        AssessmentQuestion question = AssessmentQuestion.builder().id(10L).build();
        Technology java = Technology.builder().id(5L).build();

        AssessmentSessionQuestion sq = AssessmentSessionQuestion.builder()
                .session(session)
                .question(question)
                .technology(java)
                .topic("Java Core")
                .difficulty(AssessmentDifficulty.INTERMEDIATE)
                .presentationOrder(1)
                .scoringWeight(2)
                .build();

        sq.onCreate();

        assertEquals(1, sq.getPresentationOrder());
        assertEquals(2, sq.getScoringWeight());
        assertEquals(AssessmentDifficulty.INTERMEDIATE, sq.getDifficulty());
        assertNotNull(sq.getCreatedAt());
    }

    @Test
    @DisplayName("AssessmentAnswer builder sets default UNANSWERED status")
    void testAssessmentAnswerDefaults() {
        AssessmentAnswer answer = AssessmentAnswer.builder().build();
        answer.onCreate();

        assertEquals(AnswerStatus.UNANSWERED, answer.getStatus());
        assertEquals(0, answer.getAwardedMarks());
        assertNull(answer.getIsCorrect());
        assertNotNull(answer.getCreatedAt());
        assertNotNull(answer.getUpdatedAt());
    }

    @Test
    @DisplayName("TechnologyAssessmentResult builder sets scoring version and timestamps")
    void testTechnologyAssessmentResultDefaults() {
        TechnologyAssessmentResult result = TechnologyAssessmentResult.builder()
                .rawScore(85)
                .marksEarned(17)
                .marksAvailable(20)
                .evaluatedQuestionsCount(5)
                .correctCount(4)
                .incorrectCount(1)
                .estimatedProficiency("ADVANCED")
                .confidenceLevel("HIGH")
                .build();

        result.onCreate();

        assertEquals("v2.0-heuristic", result.getScoringVersion());
        assertEquals(85, result.getRawScore());
        assertEquals("ADVANCED", result.getEstimatedProficiency());
        assertEquals("HIGH", result.getConfidenceLevel());
        assertNotNull(result.getCreatedAt());
    }
}
