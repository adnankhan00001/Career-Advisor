package com.careeradvisor.backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressActionDto {
    private String title;
    private String category;
    private String whyItMatters;
    private String actionUrl;
    private String estimatedEffort;
}
