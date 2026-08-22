package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSubmissionResultDto {
    private String status; // ACCEPTED, WRONG_ANSWER, COMPILATION_ERROR, RUNTIME_ERROR, NOT_EXECUTED
    private String output;
    private String expectedOutput;
    private String input;
    private Long executionTimeMs;
    private Long memoryKb;
    private int testCasesPassed;
    private int totalTestCases;
    private String message;
    private String sandboxInfo;
}
