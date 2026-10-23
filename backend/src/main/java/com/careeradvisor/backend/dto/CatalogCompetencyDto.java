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
public class CatalogCompetencyDto {
    private Long id;
    private String competencyKey;
    private String title;
    private String description;
    private String level;
    private Integer sequenceOrder;
    private String technologyName;
    private String technologySlug;

    @Builder.Default
    private List<String> prerequisiteKeys = new ArrayList<>();
}
