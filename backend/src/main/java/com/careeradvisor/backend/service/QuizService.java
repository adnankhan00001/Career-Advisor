package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.AnswerDto;
import com.careeradvisor.backend.dto.QuizRequest;
import com.careeradvisor.backend.dto.QuizResponse;
import com.careeradvisor.backend.model.QuizAttempt;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.QuizAttemptRepository;
import com.careeradvisor.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class QuizService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final UserRepository userRepository;

    // Canonical Question Answer Keys & Categories
    private static final Map<Integer, QuestionMeta> QUESTION_BANK = new HashMap<>();

    static {
        QUESTION_BANK.put(1, new QuestionMeta("General CS", "O(log n)"));
        QUESTION_BANK.put(2, new QuestionMeta("General CS", "Queue"));
        QUESTION_BANK.put(3, new QuestionMeta("General CS", "Tree"));
        QUESTION_BANK.put(4, new QuestionMeta("Backend", "201 Created"));
        QUESTION_BANK.put(5, new QuestionMeta("Backend", "Atomicity, Consistency, Isolation, Durability"));
        QUESTION_BANK.put(6, new QuestionMeta("Frontend", "useEffect"));
        QUESTION_BANK.put(7, new QuestionMeta("Data & Analytics", "HAVING"));
        QUESTION_BANK.put(8, new QuestionMeta("AI & ML", "Overfitting"));
    }

    public QuizService(QuizAttemptRepository quizAttemptRepository, UserRepository userRepository) {
        this.quizAttemptRepository = quizAttemptRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public QuizResponse processQuizSubmission(QuizRequest request, User user) {
        Map<Integer, String> userAnswers = new HashMap<>();

        if (request.getAnswers() != null) {
            for (AnswerDto ans : request.getAnswers()) {
                userAnswers.put(ans.getQuestionId(), ans.getSelected());
            }
        }
        if (request.getAnswersMap() != null) {
            userAnswers.putAll(request.getAnswersMap());
        }

        int score = 0;
        int totalQuestions = QUESTION_BANK.size();
        Map<String, Integer> categoryScores = new HashMap<>();
        Map<String, Integer> categoryTotals = new HashMap<>();

        for (Map.Entry<Integer, QuestionMeta> entry : QUESTION_BANK.entrySet()) {
            Integer qId = entry.getKey();
            QuestionMeta meta = entry.getValue();

            categoryTotals.put(meta.category, categoryTotals.getOrDefault(meta.category, 0) + 1);

            String selected = userAnswers.get(qId);
            if (selected != null && selected.trim().equalsIgnoreCase(meta.correctAnswer.trim())) {
                score++;
                categoryScores.put(meta.category, categoryScores.getOrDefault(meta.category, 0) + 1);
            }
        }

        int percentage = Math.round(((float) score / totalQuestions) * 100);

        String level;
        if (percentage >= 75) {
            level = "Advanced";
        } else if (percentage >= 40) {
            level = "Intermediate";
        } else {
            level = "Beginner";
        }

        // Determine recommended career track
        String bestCategory = "General CS";
        float highestRatio = -1.0f;
        for (Map.Entry<String, Integer> catEntry : categoryTotals.entrySet()) {
            String cat = catEntry.getKey();
            int total = catEntry.getValue();
            int correct = categoryScores.getOrDefault(cat, 0);
            float ratio = (float) correct / total;
            if (ratio > highestRatio) {
                highestRatio = ratio;
                bestCategory = cat;
            }
        }

        String recommendedCareer;
        if ("Backend".equalsIgnoreCase(bestCategory)) {
            recommendedCareer = "Java Backend Developer";
        } else if ("Frontend".equalsIgnoreCase(bestCategory)) {
            recommendedCareer = "Frontend Developer";
        } else if ("Data & Analytics".equalsIgnoreCase(bestCategory)) {
            recommendedCareer = "Data Analyst";
        } else if ("AI & ML".equalsIgnoreCase(bestCategory)) {
            recommendedCareer = "AI/ML Engineer";
        } else {
            recommendedCareer = percentage >= 60 ? "Full Stack Developer" : "Frontend Developer";
        }

        // Save attempt and update user profile if authenticated
        if (user != null) {
            QuizAttempt attempt = QuizAttempt.builder()
                    .user(user)
                    .score(score)
                    .totalQuestions(totalQuestions)
                    .percentage(percentage)
                    .level(level)
                    .recommendedCareer(recommendedCareer)
                    .build();
            quizAttemptRepository.save(attempt);

            user.setUserLevel(level);
            user.setLatestQuizScore(score + "/" + totalQuestions + " (" + percentage + "%)");
            userRepository.save(user);
        }

        return QuizResponse.builder()
                .score(score)
                .totalQuestions(totalQuestions)
                .percentage(percentage)
                .level(level)
                .recommendedCareer(recommendedCareer)
                .categoryScores(categoryScores)
                .build();
    }

    public Optional<QuizAttempt> getLatestAttempt(User user) {
        return quizAttemptRepository.findFirstByUserOrderByCreatedAtDesc(user);
    }

    public String evaluateLevel(List<AnswerDto> answers) {
        QuizRequest req = new QuizRequest();
        req.setAnswers(answers);
        return processQuizSubmission(req, null).getLevel();
    }

    private static class QuestionMeta {
        String category;
        String correctAnswer;

        QuestionMeta(String category, String correctAnswer) {
            this.category = category;
            this.correctAnswer = correctAnswer;
        }
    }
}