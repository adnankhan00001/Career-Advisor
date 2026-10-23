package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogCareerDto {
    private Long id;
    private String title;
    private String slug;
    private String description;
    private String overview;
    private String category;
    private String level;
    private String duration;
    private String salaryRange;

    @Builder.Default
    private List<CatalogTechnologyDto> technologies = new ArrayList<>();
}
