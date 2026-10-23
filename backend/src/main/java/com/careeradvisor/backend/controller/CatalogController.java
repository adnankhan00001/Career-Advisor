package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.CatalogCareerDto;
import com.careeradvisor.backend.dto.CatalogCompetencyDto;
import com.careeradvisor.backend.dto.CatalogTechnologyDto;
import com.careeradvisor.backend.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/careers")
    public ResponseEntity<List<CatalogCareerDto>> getAllCareers() {
        return ResponseEntity.ok(catalogService.getAllCareers());
    }

    @GetMapping("/careers/{slug}")
    public ResponseEntity<CatalogCareerDto> getCareerBySlug(@PathVariable String slug) {
        return catalogService.getCareerBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/technologies")
    public ResponseEntity<List<CatalogTechnologyDto>> getAllTechnologies() {
        return ResponseEntity.ok(catalogService.getAllTechnologies());
    }

    @GetMapping("/technologies/{slug}")
    public ResponseEntity<CatalogTechnologyDto> getTechnologyBySlug(@PathVariable String slug) {
        return catalogService.getTechnologyBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/technologies/{slug}/competencies")
    public ResponseEntity<List<CatalogCompetencyDto>> getCompetenciesForTechnology(@PathVariable String slug) {
        if (catalogService.getTechnologyBySlug(slug).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(catalogService.getCompetenciesForTechnology(slug));
    }
}
