package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.QuizRequest;
import com.careeradvisor.backend.dto.QuizResponse;
import com.careeradvisor.backend.model.QuizAttempt;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import com.careeradvisor.backend.security.CustomUserDetails;
import com.careeradvisor.backend.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class QuizController {

    private final QuizService quizService;
    private final UserRepository userRepository;

    public QuizController(QuizService quizService, UserRepository userRepository) {
        this.quizService = quizService;
        this.userRepository = userRepository;
    }

    @PostMapping({"/quiz/submit", "/api/quiz/submit"})
    public ResponseEntity<QuizResponse> submitQuiz(
            @RequestBody QuizRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = null;
        if (userDetails != null && userDetails.getId() != null) {
            user = userRepository.findById(userDetails.getId()).orElse(null);
        }

        QuizResponse response = quizService.processQuizSubmission(request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/quiz/latest")
    public ResponseEntity<QuizResponse> getLatestQuizResult(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.notFound().build();
        }

        User user = userRepository.findById(userDetails.getId()).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return quizService.getLatestAttempt(user)
                .map(attempt -> ResponseEntity.ok(QuizResponse.builder()
                        .score(attempt.getScore())
                        .totalQuestions(attempt.getTotalQuestions())
                        .percentage(attempt.getPercentage())
                        .level(attempt.getLevel())
                        .recommendedCareer(attempt.getRecommendedCareer())
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }
}