package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.AnswerDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuizService {

    public String evaluateLevel(List<AnswerDto> answers) {
        int score = 0;

        for (AnswerDto ans : answers) {
            String selected = ans.getSelected();

            if ("O(log n)".equals(selected) ||
                    "Queue".equals(selected) ||
                    "Tree".equals(selected)) {
                score++;
            }
        }

        return assignLevel(score);
    }

    private String assignLevel(int score) {
        if (score <= 2) return "LEVEL_1";
        else return "LEVEL_2"; // future
    }
}