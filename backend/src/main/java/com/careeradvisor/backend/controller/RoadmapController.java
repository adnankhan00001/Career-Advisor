package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.RoadmapSectionDto;
import com.careeradvisor.backend.service.RoadmapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roadmaps")
public class RoadmapController {

    private final RoadmapService roadmapService;

    public RoadmapController(RoadmapService roadmapService) {
        this.roadmapService = roadmapService;
    }

    @GetMapping
    public ResponseEntity<Map<String, List<RoadmapSectionDto>>> getAllRoadmaps() {
        return ResponseEntity.ok(roadmapService.getAllRoadmaps());
    }

    @GetMapping("/{careerTitle}")
    public ResponseEntity<List<RoadmapSectionDto>> getRoadmap(@PathVariable String careerTitle) {
        List<RoadmapSectionDto> sections = roadmapService.getRoadmapForCareer(careerTitle);
        if (sections.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sections);
    }
}
