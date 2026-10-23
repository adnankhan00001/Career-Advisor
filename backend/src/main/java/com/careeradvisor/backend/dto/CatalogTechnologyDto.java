package com.careeradvisor.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogTechnologyDto {
    private Long id;
    private String name;
    private String slug;
    private String category;
    private String icon;
    private String description;
    private int competencyCount;
}
