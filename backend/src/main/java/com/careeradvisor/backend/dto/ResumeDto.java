package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.ResumeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDto {
    private Long id;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private LocalDateTime uploadTimestamp;
    private ResumeStatus parsingStatus;
    private int skillsCount;
    private String extractedSummary;
    private String extractedEmail;
    private String extractedPhone;
}
