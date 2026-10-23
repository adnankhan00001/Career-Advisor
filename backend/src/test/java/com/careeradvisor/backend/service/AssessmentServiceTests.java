package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTests {

    @Mock
    private AssessmentSessionRepository sessionRepository;

    @Mock
    private AssessmentQuestionRepository questionRepository;

    @Mock
    private AssessmentSessionQuestionRepository sessionQuestionRepository;

    @Mock
    private AssessmentAnswerRepository answerRepository;

    @Mock
    private TechnologyAssessmentResultRepository resultRepository;

    @Mock
    private TechnologyRepository technologyRepository;

    @Mock
    private CatalogService catalogService;

    @InjectMocks
    private AssessmentService assessmentService;

    private User student1;
    private User student2;
    private Technology techJava;
    private Technology techMysql;
    private AssessmentQuestion questionJavaBeginner;
    private AssessmentQuestion questionJavaIntermediate;
    private AssessmentQuestion questionMysqlBeginner;

    @BeforeEach
    void setUp() {
        student1 = User.builder().id(101L).name("Alice Student").email("alice@student.dev").build();
        student2 = User.builder().id(102L).name("Bob Student").email("bob@student.dev").build();

        techJava = Technology.builder()
                .id(1L)
                .name("Java")
                .slug("java")
                .active(true)
                .build();

        techMysql = Technology.builder()
                .id(2L)
                .name("MySQL")
                .slug("mysql")
                .active(true)
                .build();

        questionJavaBeginner = AssessmentQuestion.builder()
                .id(10L)
                .questionKey("java-beg-01")
                .technology(techJava)
                .topic("Syntax")
                .difficulty(AssessmentDifficulty.BEGINNER)
                .questionType(AssessmentQuestionType.MULTIPLE_CHOICE)
                .questionText("What is a boolean default?")
                .optionsJson("[\"true\",\"false\",\"null\",\"0\"]")
                .correctOptionIndex(1)
                .explanation("Default is false")
                .scoringWeight(1)
                .active(true)
                .build();

        questionJavaIntermediate = AssessmentQuestion.builder()
                .id(11L)
                .questionKey("java-int-01")
                .technology(techJava)
                .topic("Collections")
                .difficulty(AssessmentDifficulty.INTERMEDIATE)
                .questionType(AssessmentQuestionType.MULTIPLE_CHOICE)
                .questionText("HashMap complexity?")
                .optionsJson("[\"O(1)\",\"O(n)\",\"O(log n)\",\"O(n^2)\"]")
                .correctOptionIndex(0)
                .explanation("Average complexity is O(1)")
                .scoringWeight(2)
                .active(true)
                .build();

        questionMysqlBeginner = AssessmentQuestion.builder()
                .id(20L)
                .questionKey("mysql-beg-01")
                .technology(techMysql)
                .topic("SQL Syntax")
                .difficulty(AssessmentDifficulty.BEGINNER)
                .questionType(AssessmentQuestionType.MULTIPLE_CHOICE)
                .questionText("Which keyword groups records?")
                .optionsJson("[\"ORDER BY\",\"GROUP BY\",\"SORT\",\"COLLECT\"]")
                .correctOptionIndex(1)
                .explanation("GROUP BY groups rows")
                .scoringWeight(1)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("startAssessment creates session with initial questions for selected technologies")
    void testStartAssessmentSuccess() {
        StartAssessmentRequest request = StartAssessmentRequest.builder()
                .technologySlugs(List.of("java", "mysql"))
                .timeLimitMinutes(20)
                .build();

        when(technologyRepository.findBySlug("java")).thenReturn(Optional.of(techJava));
        when(technologyRepository.findBySlug("mysql")).thenReturn(Optional.of(techMysql));
        when(sessionRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(student1, AssessmentStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());

        when(sessionRepository.save(any(AssessmentSession.class))).thenAnswer(invocation -> {
            AssessmentSession s = invocation.getArgument(0);
            if (s.getId() == null) s.setId(500L);
            return s;
        });

        when(questionRepository.findByTechnologyAndActiveTrue(techJava)).thenReturn(List.of(questionJavaBeginner));
        when(questionRepository.findByTechnologyAndActiveTrue(techMysql)).thenReturn(List.of(questionMysqlBeginner));

        when(sessionQuestionRepository.save(any(AssessmentSessionQuestion.class))).thenAnswer(invocation -> {
            AssessmentSessionQuestion sq = invocation.getArgument(0);
            sq.setId((long) (sq.getPresentationOrder() + 1000));
            return sq;
        });

        when(sessionQuestionRepository.countBySessionId(500L)).thenReturn(2);
        AssessmentSessionQuestion sq1 = AssessmentSessionQuestion.builder()
                .id(1001L)
                .session(AssessmentSession.builder().id(500L).build())
                .question(questionJavaBeginner)
                .technology(techJava)
                .topic("Syntax")
                .difficulty(AssessmentDifficulty.BEGINNER)
                .presentationOrder(1)
                .build();
        AssessmentSessionQuestion sq2 = AssessmentSessionQuestion.builder()
                .id(1002L)
                .session(AssessmentSession.builder().id(500L).build())
                .question(questionMysqlBeginner)
                .technology(techMysql)
                .topic("SQL Syntax")
                .difficulty(AssessmentDifficulty.BEGINNER)
                .presentationOrder(2)
                .build();
        when(sessionQuestionRepository.findBySessionIdWithQuestionAndTech(500L)).thenReturn(List.of(sq1, sq2));

        when(catalogService.mapToTechnologyDto(any(Technology.class))).thenAnswer(invocation -> {
            Technology t = invocation.getArgument(0);
            return CatalogTechnologyDto.builder().id(t.getId()).name(t.getName()).slug(t.getSlug()).build();
        });

        AssessmentSessionDto result = assessmentService.startAssessment(student1, request);

        assertNotNull(result);
        assertEquals(500L, result.getSessionId());
        assertEquals("IN_PROGRESS", result.getStatus());
        assertEquals(2, result.getTotalQuestionsCount());
        verify(sessionRepository, atLeastOnce()).save(any(AssessmentSession.class));
    }

    @Test
    @DisplayName("startAssessment rejects unknown technology slug")
    void testStartAssessmentUnknownTech() {
        StartAssessmentRequest request = StartAssessmentRequest.builder()
                .technologySlugs(List.of("unknown-slug"))
                .build();

        when(technologyRepository.findBySlug("unknown-slug")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                assessmentService.startAssessment(student1, request));
    }

    @Test
    @DisplayName("submitAnswer records correct answer and marks")
    void testSubmitAnswerCorrect() {
        AssessmentSession session = AssessmentSession.builder()
                .id(501L)
                .user(student1)
                .status(AssessmentStatus.IN_PROGRESS)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        AssessmentSessionQuestion sq = AssessmentSessionQuestion.builder()
                .id(1001L)
                .session(session)
                .question(questionJavaBeginner)
                .technology(techJava)
                .topic("Syntax")
                .difficulty(AssessmentDifficulty.BEGINNER)
                .presentationOrder(1)
                .scoringWeight(1)
                .build();

        when(sessionRepository.findById(501L)).thenReturn(Optional.of(session));
        when(sessionQuestionRepository.findById(1001L)).thenReturn(Optional.of(sq));
        when(answerRepository.findBySessionQuestionId(1001L)).thenReturn(Optional.empty());
        when(sessionQuestionRepository.countBySessionIdAndTechnologyId(501L, techJava.getId())).thenReturn(1);
        when(sessionQuestionRepository.findBySessionIdAndTechnologyIdOrderByPresentationOrderAsc(501L, techJava.getId()))
                .thenReturn(List.of(sq));
        when(questionRepository.findByTechnologyAndActiveTrue(techJava))
                .thenReturn(List.of(questionJavaBeginner, questionJavaIntermediate));
        when(sessionQuestionRepository.save(any(AssessmentSessionQuestion.class))).thenAnswer(i -> {
            AssessmentSessionQuestion item = i.getArgument(0);
            item.setId(2001L);
            return item;
        });

        SubmitAnswerRequest request = SubmitAnswerRequest.builder()
                .sessionQuestionId(1001L)
                .selectedOptionIndex(1) // Option 1 is correct for questionJavaBeginner
                .build();

        AssessmentQuestionDto dto = assessmentService.submitAnswer(501L, request, student1);

        assertNotNull(dto);
        assertEquals(1, dto.getSelectedOptionIndex());
        assertEquals("ANSWERED", dto.getAnswerStatus());
        verify(answerRepository, atLeastOnce()).save(any(AssessmentAnswer.class));
    }

    @Test
    @DisplayName("skipQuestion marks status as SKIPPED with 0 marks")
    void testSkipQuestion() {
        AssessmentSession session = AssessmentSession.builder()
                .id(502L)
                .user(student1)
                .status(AssessmentStatus.IN_PROGRESS)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        AssessmentSessionQuestion sq = AssessmentSessionQuestion.builder()
                .id(1002L)
                .session(session)
                .question(questionJavaBeginner)
                .technology(techJava)
                .topic("Syntax")
                .difficulty(AssessmentDifficulty.BEGINNER)
                .presentationOrder(1)
                .build();

        when(sessionRepository.findById(502L)).thenReturn(Optional.of(session));
        when(sessionQuestionRepository.findById(1002L)).thenReturn(Optional.of(sq));
        when(answerRepository.findBySessionQuestionId(1002L)).thenReturn(Optional.empty());

        AssessmentQuestionDto dto = assessmentService.skipQuestion(502L, 1002L, student1);

        assertNotNull(dto);
        assertEquals("SKIPPED", dto.getAnswerStatus());
        assertNull(dto.getSelectedOptionIndex());
    }

    @Test
    @DisplayName("submitAnswer rejects expired session")
    void testSubmitAnswerExpiredThrows() {
        AssessmentSession session = AssessmentSession.builder()
                .id(503L)
                .user(student1)
                .status(AssessmentStatus.IN_PROGRESS)
                .expiresAt(LocalDateTime.now().minusMinutes(5)) // Expired
                .build();

        when(sessionRepository.findById(503L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(AssessmentSession.class))).thenAnswer(i -> i.getArgument(0));

        SubmitAnswerRequest request = SubmitAnswerRequest.builder()
                .sessionQuestionId(1001L)
                .selectedOptionIndex(1)
                .build();

        assertThrows(IllegalStateException.class, () ->
                assessmentService.submitAnswer(503L, request, student1));
    }

    @Test
    @DisplayName("accessing session of another user throws SecurityException")
    void testOwnershipEnforcement() {
        AssessmentSession session = AssessmentSession.builder()
                .id(504L)
                .user(student1) // Belongs to student1
                .status(AssessmentStatus.IN_PROGRESS)
                .build();

        when(sessionRepository.findById(504L)).thenReturn(Optional.of(session));

        // student2 attempts to access session
        assertThrows(SecurityException.class, () ->
                assessmentService.getSessionState(504L, student2));
    }

    @Test
    @DisplayName("submitAssessment calculates technology and overall scores accurately")
    void testSubmitAssessmentScoring() {
        AssessmentSession session = AssessmentSession.builder()
                .id(505L)
                .user(student1)
                .status(AssessmentStatus.IN_PROGRESS)
                .startTime(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        AssessmentSessionQuestion sq1 = AssessmentSessionQuestion.builder()
                .id(1L)
                .session(session)
                .question(questionJavaBeginner)
                .technology(techJava)
                .topic("Syntax")
                .difficulty(AssessmentDifficulty.BEGINNER)
                .presentationOrder(1)
                .scoringWeight(1)
                .build();

        AssessmentAnswer ans1 = AssessmentAnswer.builder()
                .id(101L)
                .session(session)
                .sessionQuestion(sq1)
                .selectedOptionIndex(1)
                .status(AnswerStatus.ANSWERED)
                .isCorrect(true)
                .awardedMarks(1)
                .build();

        when(sessionRepository.findById(505L)).thenReturn(Optional.of(session));
        when(sessionQuestionRepository.findBySessionIdWithQuestionAndTech(505L)).thenReturn(List.of(sq1));
        when(answerRepository.findBySessionId(505L)).thenReturn(List.of(ans1));
        when(sessionRepository.save(any(AssessmentSession.class))).thenAnswer(i -> i.getArgument(0));

        when(resultRepository.save(any(TechnologyAssessmentResult.class))).thenAnswer(i -> {
            TechnologyAssessmentResult r = i.getArgument(0);
            r.setId(999L);
            return r;
        });

        when(resultRepository.findBySessionIdWithTechnology(505L)).thenAnswer(i -> {
            TechnologyAssessmentResult r = TechnologyAssessmentResult.builder()
                    .id(999L)
                    .session(session)
                    .technology(techJava)
                    .rawScore(100)
                    .marksEarned(1)
                    .marksAvailable(1)
                    .evaluatedQuestionsCount(1)
                    .correctCount(1)
                    .incorrectCount(0)
                    .estimatedProficiency("INSUFFICIENT_EVIDENCE")
                    .confidenceLevel("LOW")
                    .build();
            return List.of(r);
        });

        AssessmentReportDto report = assessmentService.submitAssessment(505L, student1);

        assertNotNull(report);
        assertEquals(505L, report.getSessionId());
        assertEquals(100, report.getOverallScore());
        assertEquals(1, report.getTechnologyResults().size());
        assertEquals("java", report.getTechnologyResults().get(0).getTechnologySlug());
        assertEquals(100, report.getTechnologyResults().get(0).getRawScore());
        assertEquals(1, report.getQuestionReviews().size());
        assertEquals("Default is false", report.getQuestionReviews().get(0).getExplanation());
    }

    @Test
    @DisplayName("Should handle duplicate assessment submission idempotently without re-evaluating")
    void testSubmitAssessment_duplicateSubmissionIdempotent() {
        AssessmentSession session = AssessmentSession.builder()
                .id(506L)
                .user(student1)
                .status(AssessmentStatus.SUBMITTED)
                .overallScore(85)
                .overallProficiency("Advanced")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(sessionRepository.findById(506L)).thenReturn(Optional.of(session));
        when(resultRepository.findBySessionIdWithTechnology(506L)).thenReturn(Collections.emptyList());
        when(sessionQuestionRepository.findBySessionIdWithQuestionAndTech(506L)).thenReturn(Collections.emptyList());
        when(answerRepository.findBySessionId(506L)).thenReturn(Collections.emptyList());

        AssessmentReportDto report = assessmentService.submitAssessment(506L, student1);

        assertNotNull(report);
        assertEquals(506L, report.getSessionId());
        assertEquals(85, report.getOverallScore());
        assertEquals("Advanced", report.getOverallProficiency());
        // Verify no re-saving of session or results
        verify(sessionRepository, never()).save(any(AssessmentSession.class));
        verify(resultRepository, never()).save(any(TechnologyAssessmentResult.class));
    }

    @Test
    @DisplayName("Should reject submission of CANCELLED assessment session")
    void testSubmitAssessment_rejectsCancelledSession() {
        AssessmentSession session = AssessmentSession.builder()
                .id(507L)
                .user(student1)
                .status(AssessmentStatus.CANCELLED)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(sessionRepository.findById(507L)).thenReturn(Optional.of(session));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                assessmentService.submitAssessment(507L, student1));
        assertTrue(ex.getMessage().contains("cancelled"));
    }
}
