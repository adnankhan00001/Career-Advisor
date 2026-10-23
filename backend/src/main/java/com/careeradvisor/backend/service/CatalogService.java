package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.CatalogCareerDto;
import com.careeradvisor.backend.dto.CatalogCompetencyDto;
import com.careeradvisor.backend.dto.CatalogTechnologyDto;
import com.careeradvisor.backend.model.*;
import com.careeradvisor.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private final CareerTrackRepository careerTrackRepository;
    private final TechnologyRepository technologyRepository;
    private final CompetencyNodeRepository competencyNodeRepository;
    private final CompetencyPrerequisiteRepository prerequisiteRepository;

    public CatalogService(CareerTrackRepository careerTrackRepository,
                          TechnologyRepository technologyRepository,
                          CompetencyNodeRepository competencyNodeRepository,
                          CompetencyPrerequisiteRepository prerequisiteRepository) {
        this.careerTrackRepository = careerTrackRepository;
        this.technologyRepository = technologyRepository;
        this.competencyNodeRepository = competencyNodeRepository;
        this.prerequisiteRepository = prerequisiteRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogCareerDto> getAllCareers() {
        List<CareerTrack> tracks = careerTrackRepository.findAllActiveWithTechnologies();
        return tracks.stream()
                .map(this::mapToCareerDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<CatalogCareerDto> getCareerBySlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            return Optional.empty();
        }
        return careerTrackRepository.findBySlugWithTechnologies(slug.trim().toLowerCase())
                .map(this::mapToCareerDto);
    }

    @Transactional(readOnly = true)
    public List<CatalogTechnologyDto> getAllTechnologies() {
        List<Technology> technologies = technologyRepository.findByActiveTrueOrderByNameAsc();
        return technologies.stream()
                .map(this::mapToTechnologyDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<CatalogTechnologyDto> getTechnologyBySlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            return Optional.empty();
        }
        return technologyRepository.findBySlug(slug.trim().toLowerCase())
                .map(this::mapToTechnologyDto);
    }

    @Transactional(readOnly = true)
    public List<CatalogCompetencyDto> getCompetenciesForTechnology(String technologySlug) {
        if (technologySlug == null || technologySlug.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<CompetencyNode> nodes = competencyNodeRepository.findByTechnologySlug(technologySlug.trim().toLowerCase());
        return nodes.stream()
                .map(this::mapToCompetencyDto)
                .collect(Collectors.toList());
    }

    /**
     * Validates and establishes a directed prerequisite relationship between two competencies.
     * Prevents self-dependencies, duplicate records, and circular dependency chains.
     */
    @Transactional
    public CompetencyPrerequisite addPrerequisite(CompetencyNode competency, CompetencyNode prerequisite, String reason) {
        if (competency == null || prerequisite == null) {
            throw new IllegalArgumentException("Both competency and prerequisite must be non-null");
        }

        // 1. Self-dependency check
        if (competency.getId() != null && competency.getId().equals(prerequisite.getId())) {
            throw new IllegalArgumentException("A competency cannot depend on itself: " + competency.getCompetencyKey());
        }
        if (competency.getCompetencyKey().equalsIgnoreCase(prerequisite.getCompetencyKey())) {
            throw new IllegalArgumentException("A competency cannot depend on itself: " + competency.getCompetencyKey());
        }

        // 2. Duplicate check
        if (prerequisiteRepository.existsByCompetencyAndPrerequisite(competency, prerequisite)) {
            return prerequisiteRepository.findByCompetencyAndPrerequisite(competency, prerequisite).orElseThrow();
        }

        // 3. Circular dependency check (DFS)
        if (isReachable(prerequisite, competency)) {
            throw new IllegalStateException(String.format(
                    "Circular dependency detected: adding %s -> %s creates a cycle",
                    competency.getCompetencyKey(), prerequisite.getCompetencyKey()
            ));
        }

        CompetencyPrerequisite record = CompetencyPrerequisite.builder()
                .competency(competency)
                .prerequisite(prerequisite)
                .reason(reason)
                .build();

        return prerequisiteRepository.save(record);
    }

    /**
     * Performs Depth-First Search (DFS) to detect if targetNode is reachable from startNode
     * through existing prerequisite edges.
     */
    @Transactional(readOnly = true)
    public boolean isReachable(CompetencyNode startNode, CompetencyNode targetNode) {
        if (startNode == null || targetNode == null) {
            return false;
        }
        if (startNode.getId() != null && startNode.getId().equals(targetNode.getId())) {
            return true;
        }

        Set<Long> visited = new HashSet<>();
        Queue<CompetencyNode> queue = new LinkedList<>();
        queue.add(startNode);
        if (startNode.getId() != null) {
            visited.add(startNode.getId());
        }

        while (!queue.isEmpty()) {
            CompetencyNode current = queue.poll();
            if (current.getId() != null && current.getId().equals(targetNode.getId())) {
                return true;
            }

            List<CompetencyNode> prerequisites = prerequisiteRepository.findPrerequisiteNodes(current.getId());
            for (CompetencyNode next : prerequisites) {
                if (next.getId() != null && !visited.contains(next.getId())) {
                    visited.add(next.getId());
                    queue.add(next);
                }
            }
        }

        return false;
    }

    public CatalogCareerDto mapToCareerDto(CareerTrack track) {
        List<CatalogTechnologyDto> techDtos = track.getTechnologies().stream()
                .filter(Technology::isActive)
                .sorted(Comparator.comparing(Technology::getName))
                .map(this::mapToTechnologyDto)
                .collect(Collectors.toList());

        return CatalogCareerDto.builder()
                .id(track.getId())
                .title(track.getTitle())
                .slug(track.getSlug())
                .description(track.getDescription())
                .overview(track.getOverview())
                .category(track.getCategory())
                .level(track.getLevel())
                .duration(track.getDuration())
                .salaryRange(track.getSalaryRange())
                .technologies(techDtos)
                .build();
    }

    public CatalogTechnologyDto mapToTechnologyDto(Technology tech) {
        List<CompetencyNode> competencies = competencyNodeRepository.findByTechnologyAndActiveTrueOrderBySequenceOrderAsc(tech);
        return CatalogTechnologyDto.builder()
                .id(tech.getId())
                .name(tech.getName())
                .slug(tech.getSlug())
                .category(tech.getCategory())
                .icon(tech.getIcon())
                .description(tech.getDescription())
                .competencyCount(competencies.size())
                .build();
    }

    public CatalogCompetencyDto mapToCompetencyDto(CompetencyNode node) {
        List<CompetencyNode> prereqNodes = prerequisiteRepository.findPrerequisiteNodes(node.getId());
        List<String> prereqKeys = prereqNodes.stream()
                .map(CompetencyNode::getCompetencyKey)
                .collect(Collectors.toList());

        return CatalogCompetencyDto.builder()
                .id(node.getId())
                .competencyKey(node.getCompetencyKey())
                .title(node.getTitle())
                .description(node.getDescription())
                .level(node.getLevel())
                .sequenceOrder(node.getSequenceOrder())
                .technologyName(node.getTechnology().getName())
                .technologySlug(node.getTechnology().getSlug())
                .prerequisiteKeys(prereqKeys)
                .build();
    }
}
