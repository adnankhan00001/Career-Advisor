package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerDto {
    private String id;
    private String title;
    private String category;
    private String level;
    private String duration;
    private String description;
    private String overview;
    private List<String> requiredSkills;
    private List<String> technologies;
    private List<String> responsibilities;
    private String salaryRange;
    private String roadmapSlug;
}
