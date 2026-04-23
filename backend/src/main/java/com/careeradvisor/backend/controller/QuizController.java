package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.QuizRequest;
import com.careeradvisor.backend.dto.QuizResponse;
import com.careeradvisor.backend.service.QuizService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/submit")
    public QuizResponse submitQuiz(@RequestBody QuizRequest request) {

        String level = quizService.evaluateLevel(request.getAnswers());

        return new QuizResponse(level);
    }
}