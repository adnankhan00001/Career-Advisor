package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.*;
import com.careeradvisor.backend.exception.ResourceNotFoundException;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.InterviewAnswerRepository;
import com.careeradvisor.backend.repository.InterviewQuestionRepository;
import com.careeradvisor.backend.repository.MockInterviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InterviewService {

    private final MockInterviewRepository interviewRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;

    public InterviewService(MockInterviewRepository interviewRepository,
                            InterviewQuestionRepository questionRepository,
                            InterviewAnswerRepository answerRepository) {
        this.interviewRepository = interviewRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    @Transactional
    public MockInterviewSessionDto startInterview(StartInterviewRequest request, User user) {
        ProblemCategory category = request.getCategory() != null ? request.getCategory() : ProblemCategory.JAVA;
        Difficulty difficulty = request.getDifficulty() != null ? request.getDifficulty() : Difficulty.MEDIUM;
        int durationMinutes = request.getDurationMinutes() != null && request.getDurationMinutes() >= 5
                ? Math.min(request.getDurationMinutes(), 60) : 15;
        int questionCount = request.getQuestionCount() != null && request.getQuestionCount() >= 3
                ? Math.min(request.getQuestionCount(), 10) : 5;

        // Select questions for session
        List<InterviewQuestion> candidates = questionRepository.findByCategoryAndDifficultyOrderByOrderIndexAsc(category, difficulty);
        if (candidates.size() < questionCount) {
            List<InterviewQuestion> byCat = questionRepository.findByCategoryOrderByOrderIndexAsc(category);
            for (InterviewQuestion q : byCat) {
                if (!candidates.contains(q)) {
                    candidates.add(q);
                }
            }
        }
        if (candidates.isEmpty()) {
            candidates = questionRepository.findAllByOrderByOrderIndexAsc();
        }

        List<InterviewQuestion> selectedQuestions = candidates.stream()
                .limit(questionCount)
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();
        int durationSeconds = durationMinutes * 60;
        LocalDateTime deadline = now.plusSeconds(durationSeconds);

        MockInterview interview = MockInterview.builder()
                .user(user)
                .category(category)
                .difficulty(difficulty)
                .status(InterviewStatus.IN_PROGRESS)
                .startedAt(now)
                .deadline(deadline)
                .durationSeconds(durationSeconds)
                .totalQuestions(selectedQuestions.size())
                .score(0)
                .correctCount(0)
                .build();

        MockInterview savedInterview = interviewRepository.save(interview);

        // Pre-create answer placeholders to bind question order
        List<InterviewAnswer> answers = new ArrayList<>();
        for (InterviewQuestion q : selectedQuestions) {
            InterviewAnswer ans = InterviewAnswer.builder()
                    .interview(savedInterview)
                    .question(q)
                    .selectedAnswer(null)
                    .isCorrect(null)
                    .build();
            answers.add(ans);
        }
        answerRepository.saveAll(answers);

        return mapToSessionDto(savedInterview, answers);
    }

    public MockInterviewSessionDto getActiveSession(Long interviewId, User user) {
        MockInterview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Mock interview not found with id: " + interviewId));

        validateOwnership(interview, user);

        // Check if deadline exceeded while still IN_PROGRESS
        LocalDateTime now = LocalDateTime.now();
        if (interview.getStatus() == InterviewStatus.IN_PROGRESS && now.isAfter(interview.getDeadline())) {
            return autoExpireOrSubmit(interview);
        }

        List<InterviewAnswer> answers = answerRepository.findByInterview(interview);
        return mapToSessionDto(interview, answers);
    }

    @Transactional
    public MockInterviewSessionDto saveAnswer(Long interviewId, SaveAnswerRequest request, User user) {
        MockInterview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Mock interview not found with id: " + interviewId));

        validateOwnership(interview, user);

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot update answers for an interview that is " + interview.getStatus());
        }

        // 30 seconds grace period for network latency
        if (LocalDateTime.now().isAfter(interview.getDeadline().plusSeconds(30))) {
            interview.setStatus(InterviewStatus.EXPIRED);
            interviewRepository.save(interview);
            throw new IllegalStateException("Interview session has expired.");
        }

        InterviewQuestion question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + request.getQuestionId()));

        InterviewAnswer answer = answerRepository.findByInterviewAndQuestion(interview, question)
                .orElse(InterviewAnswer.builder()
                        .interview(interview)
                        .question(question)
                        .build());

        answer.setSelectedAnswer(request.getAnswer());
        answer.setAnsweredAt(LocalDateTime.now());
        answerRepository.save(answer);

        List<InterviewAnswer> answers = answerRepository.findByInterview(interview);
        return mapToSessionDto(interview, answers);
    }

    @Transactional
    public MockInterviewResultDto submitInterview(Long interviewId, User user) {
        MockInterview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Mock interview not found with id: " + interviewId));

        validateOwnership(interview, user);

        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            return getInterviewResult(interviewId, user);
        }

        List<InterviewAnswer> answers = answerRepository.findByInterview(interview);
        int correctCount = 0;
        int total = answers.size();

        Map<String, int[]> topicStats = new LinkedHashMap<>();

        for (InterviewAnswer ans : answers) {
            InterviewQuestion q = ans.getQuestion();
            String userChoice = ans.getSelectedAnswer() != null ? ans.getSelectedAnswer().trim() : "";
            String correctChoice = q.getCorrectAnswer() != null ? q.getCorrectAnswer().trim() : "";

            boolean correct = isAnswerMatching(userChoice, correctChoice);
            ans.setIsCorrect(correct);
            answerRepository.save(ans);

            if (correct) {
                correctCount++;
            }

            topicStats.putIfAbsent(q.getTopic(), new int[]{0, 0});
            topicStats.get(q.getTopic())[0]++;
            if (correct) {
                topicStats.get(q.getTopic())[1]++;
            }
        }

        int score = total > 0 ? Math.round(((float) correctCount / total) * 100) : 0;
        LocalDateTime now = LocalDateTime.now();

        List<String> strongAreas = new ArrayList<>();
        List<String> weakAreas = new ArrayList<>();
        Map<String, Integer> categoryBreakdown = new LinkedHashMap<>();

        for (Map.Entry<String, int[]> entry : topicStats.entrySet()) {
            int tTotal = entry.getValue()[0];
            int tCorrect = entry.getValue()[1];
            int pct = tTotal > 0 ? Math.round(((float) tCorrect / tTotal) * 100) : 0;
            categoryBreakdown.put(entry.getKey(), pct);

            if (pct >= 70) {
                strongAreas.add(entry.getKey() + " (" + pct + "%)");
            } else {
                weakAreas.add(entry.getKey() + " (" + pct + "%)");
            }
        }

        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setCompletedAt(now);
        interview.setScore(score);
        interview.setCorrectCount(correctCount);
        interview.setStrongAreas(String.join(", ", strongAreas));
        interview.setWeakAreas(String.join(", ", weakAreas));

        MockInterview saved = interviewRepository.save(interview);

        return mapToResultDto(saved, answers, strongAreas, weakAreas, categoryBreakdown);
    }

    public MockInterviewResultDto getInterviewResult(Long interviewId, User user) {
        MockInterview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Mock interview not found with id: " + interviewId));

        validateOwnership(interview, user);

        List<InterviewAnswer> answers = answerRepository.findByInterview(interview);

        List<String> strong = interview.getStrongAreas() != null && !interview.getStrongAreas().isEmpty()
                ? Arrays.asList(interview.getStrongAreas().split(", ")) : Collections.emptyList();
        List<String> weak = interview.getWeakAreas() != null && !interview.getWeakAreas().isEmpty()
                ? Arrays.asList(interview.getWeakAreas().split(", ")) : Collections.emptyList();

        Map<String, Integer> breakdown = new LinkedHashMap<>();
        for (InterviewAnswer ans : answers) {
            String topic = ans.getQuestion().getTopic();
            breakdown.put(topic, Boolean.TRUE.equals(ans.getIsCorrect()) ? 100 : 0);
        }

        return mapToResultDto(interview, answers, strong, weak, breakdown);
    }

    public List<MockInterviewHistoryItemDto> getInterviewHistory(User user) {
        List<MockInterview> interviews = interviewRepository.findByUserOrderByStartedAtDesc(user);
        return interviews.stream()
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());
    }

    public InterviewSummaryDto getInterviewSummary(User user) {
        List<MockInterview> all = interviewRepository.findByUserOrderByStartedAtDesc(user);
        List<MockInterview> completed = all.stream()
                .filter(i -> i.getStatus() == InterviewStatus.COMPLETED)
                .collect(Collectors.toList());

        int total = all.size();
        int completedCount = completed.size();
        int avgScore = completedCount > 0
                ? (int) Math.round(completed.stream().mapToInt(MockInterview::getScore).average().orElse(0)) : 0;
        int bestScore = completed.stream().mapToInt(MockInterview::getScore).max().orElse(0);
        int latestScore = completed.isEmpty() ? 0 : completed.get(0).getScore();

        String strongest = "Core Java";
        String weakest = "DBMS & SQL";

        if (!completed.isEmpty()) {
            Map<ProblemCategory, Double> catAvgs = completed.stream()
                    .collect(Collectors.groupingBy(MockInterview::getCategory,
                            Collectors.averagingInt(MockInterview::getScore)));

            strongest = catAvgs.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey().name())
                    .orElse("Core Java");

            weakest = catAvgs.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(e -> e.getKey().name())
                    .orElse("DBMS");
        }

        List<MockInterviewHistoryItemDto> recent = all.stream()
                .limit(5)
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());

        return InterviewSummaryDto.builder()
                .totalInterviews(total)
                .completedInterviews(completedCount)
                .averageScore(avgScore)
                .bestScore(bestScore)
                .latestScore(latestScore)
                .strongestCategory(strongest)
                .weakestCategory(weakest)
                .recentInterviews(recent)
                .build();
    }

    private void validateOwnership(MockInterview interview, User user) {
        if (user == null || !interview.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not own this mock interview session.");
        }
    }

    private boolean isAnswerMatching(String userChoice, String correctChoice) {
        if (userChoice.isEmpty() || correctChoice.isEmpty()) return false;
        if (userChoice.equalsIgnoreCase(correctChoice)) return true;

        // Compare leading option letter (e.g. "A" vs "A) ...")
        char userLetter = Character.toUpperCase(userChoice.charAt(0));
        char correctLetter = Character.toUpperCase(correctChoice.charAt(0));
        return userLetter == correctLetter;
    }

    private MockInterviewSessionDto autoExpireOrSubmit(MockInterview interview) {
        interview.setStatus(InterviewStatus.EXPIRED);
        interviewRepository.save(interview);
        List<InterviewAnswer> answers = answerRepository.findByInterview(interview);
        return mapToSessionDto(interview, answers);
    }

    private MockInterviewSessionDto mapToSessionDto(MockInterview interview, List<InterviewAnswer> answers) {
        LocalDateTime now = LocalDateTime.now();
        long remaining = Math.max(0, Duration.between(now, interview.getDeadline()).getSeconds());

        Map<Long, String> selectedMap = new HashMap<>();
        for (InterviewAnswer ans : answers) {
            if (ans.getSelectedAnswer() != null) {
                selectedMap.put(ans.getQuestion().getId(), ans.getSelectedAnswer());
            }
        }

        List<InterviewQuestionDto> questionDtos = answers.stream()
                .map(ans -> {
                    InterviewQuestion q = ans.getQuestion();
                    return InterviewQuestionDto.builder()
                            .id(q.getId())
                            .question(q.getQuestion())
                            .category(q.getCategory())
                            .topic(q.getTopic())
                            .difficulty(q.getDifficulty())
                            .questionType(q.getQuestionType())
                            .options(q.getOptions())
                            .expectedConcepts(q.getExpectedConcepts())
                            .orderIndex(q.getOrderIndex())
                            .selectedAnswer(selectedMap.get(q.getId()))
                            .build();
                })
                .collect(Collectors.toList());

        return MockInterviewSessionDto.builder()
                .id(interview.getId())
                .category(interview.getCategory())
                .difficulty(interview.getDifficulty())
                .status(interview.getStatus())
                .startedAt(interview.getStartedAt())
                .deadline(interview.getDeadline())
                .remainingSeconds(remaining)
                .durationSeconds(interview.getDurationSeconds())
                .totalQuestions(interview.getTotalQuestions())
                .answeredCount(selectedMap.size())
                .questions(questionDtos)
                .build();
    }

    private MockInterviewResultDto mapToResultDto(MockInterview interview,
                                                 List<InterviewAnswer> answers,
                                                 List<String> strong,
                                                 List<String> weak,
                                                 Map<String, Integer> breakdown) {
        int durationUsed = interview.getCompletedAt() != null
                ? (int) Duration.between(interview.getStartedAt(), interview.getCompletedAt()).getSeconds()
                : interview.getDurationSeconds();

        List<InterviewQuestionReviewDto> reviews = answers.stream()
                .map(ans -> {
                    InterviewQuestion q = ans.getQuestion();
                    return InterviewQuestionReviewDto.builder()
                            .id(q.getId())
                            .question(q.getQuestion())
                            .category(q.getCategory())
                            .topic(q.getTopic())
                            .difficulty(q.getDifficulty())
                            .questionType(q.getQuestionType())
                            .options(q.getOptions())
                            .userAnswer(ans.getSelectedAnswer())
                            .correctAnswer(q.getCorrectAnswer())
                            .isCorrect(ans.getIsCorrect())
                            .explanation(q.getExplanation())
                            .expectedConcepts(q.getExpectedConcepts())
                            .build();
                })
                .collect(Collectors.toList());

        String recommendation = interview.getScore() >= 80
                ? "Excellent performance! Continue reinforcing edge cases in " + interview.getCategory() + "."
                : "Focus revision on " + (!weak.isEmpty() ? weak.get(0) : interview.getCategory()) + " to boost technical accuracy.";

        return MockInterviewResultDto.builder()
                .id(interview.getId())
                .category(interview.getCategory())
                .difficulty(interview.getDifficulty())
                .status(interview.getStatus())
                .startedAt(interview.getStartedAt())
                .completedAt(interview.getCompletedAt())
                .timeTakenSeconds(durationUsed)
                .durationSeconds(interview.getDurationSeconds())
                .score(interview.getScore())
                .totalQuestions(interview.getTotalQuestions())
                .correctCount(interview.getCorrectCount())
                .strongAreas(strong)
                .weakAreas(weak)
                .categoryBreakdown(breakdown)
                .questionReviews(reviews)
                .recommendation(recommendation)
                .build();
    }

    private MockInterviewHistoryItemDto mapToHistoryDto(MockInterview interview) {
        return MockInterviewHistoryItemDto.builder()
                .id(interview.getId())
                .category(interview.getCategory())
                .difficulty(interview.getDifficulty())
                .status(interview.getStatus())
                .score(interview.getScore())
                .totalQuestions(interview.getTotalQuestions())
                .correctCount(interview.getCorrectCount())
                .durationSeconds(interview.getDurationSeconds())
                .startedAt(interview.getStartedAt())
                .completedAt(interview.getCompletedAt())
                .build();
    }
}
