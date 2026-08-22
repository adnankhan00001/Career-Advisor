package com.careeradvisor.backend.dto;

import com.careeradvisor.backend.model.ProblemCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryProgressStat {
    private ProblemCategory category;
    private String displayName;
    private int total;
    private int solved;
    private int percentage;
}
