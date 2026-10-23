package com.careeradvisor.backend.service;

import com.careeradvisor.backend.dto.CatalogCareerDto;
import com.careeradvisor.backend.dto.CatalogCompetencyDto;
import com.careeradvisor.backend.dto.CatalogTechnologyDto;
import com.careeradvisor.backend.model.CareerTrack;
import com.careeradvisor.backend.model.CompetencyNode;
import com.careeradvisor.backend.model.CompetencyPrerequisite;
import com.careeradvisor.backend.model.Technology;
import com.careeradvisor.backend.repository.CareerTrackRepository;
import com.careeradvisor.backend.repository.CompetencyNodeRepository;
import com.careeradvisor.backend.repository.CompetencyPrerequisiteRepository;
import com.careeradvisor.backend.repository.TechnologyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTests {

    @Mock
    private CareerTrackRepository careerTrackRepository;

    @Mock
    private TechnologyRepository technologyRepository;

    @Mock
    private CompetencyNodeRepository competencyNodeRepository;

    @Mock
    private CompetencyPrerequisiteRepository prerequisiteRepository;

    @InjectMocks
    private CatalogService catalogService;

    private Technology techJava;
    private Technology techSpring;
    private CareerTrack trackBackend;
    private CompetencyNode compCoreJava;
    private CompetencyNode compAdvJava;
    private CompetencyNode compSpringCore;

    @BeforeEach
    void setUp() {
        techJava = Technology.builder()
                .id(1L)
                .name("Java")
                .slug("java")
                .category("Programming Language")
                .active(true)
                .build();

        techSpring = Technology.builder()
                .id(2L)
                .name("Spring Boot")
                .slug("spring-boot")
                .category("Backend Framework")
                .active(true)
                .build();

        trackBackend = CareerTrack.builder()
                .id(10L)
                .title("Backend Developer")
                .slug("backend-developer")
                .description("Scalable backend services")
                .active(true)
                .technologies(new HashSet<>(Arrays.asList(techJava, techSpring)))
                .build();

        compCoreJava = CompetencyNode.builder()
                .id(100L)
                .competencyKey("java-core-oop")
                .title("Object-Oriented Programming in Java")
                .technology(techJava)
                .level("BEGINNER")
                .sequenceOrder(1)
                .active(true)
                .build();

        compAdvJava = CompetencyNode.builder()
                .id(101L)
                .competencyKey("java-multithreading")
                .title("Concurrency & Multithreading")
                .technology(techJava)
                .level("INTERMEDIATE")
                .sequenceOrder(2)
                .active(true)
                .build();

        compSpringCore = CompetencyNode.builder()
                .id(102L)
                .competencyKey("spring-core-di")
                .title("Spring Dependency Injection")
                .technology(techSpring)
                .level("INTERMEDIATE")
                .sequenceOrder(1)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("getAllCareers returns active career tracks mapped to DTOs")
    void testGetAllCareers() {
        when(careerTrackRepository.findAllActiveWithTechnologies()).thenReturn(List.of(trackBackend));

        List<CatalogCareerDto> dtos = catalogService.getAllCareers();

        assertEquals(1, dtos.size());
        assertEquals("backend-developer", dtos.get(0).getSlug());
        assertEquals("Backend Developer", dtos.get(0).getTitle());
        assertEquals(2, dtos.get(0).getTechnologies().size());
        verify(careerTrackRepository, times(1)).findAllActiveWithTechnologies();
    }

    @Test
    @DisplayName("getCareerBySlug returns career when present")
    void testGetCareerBySlugFound() {
        when(careerTrackRepository.findBySlugWithTechnologies("backend-developer"))
                .thenReturn(Optional.of(trackBackend));

        Optional<CatalogCareerDto> dtoOpt = catalogService.getCareerBySlug("backend-developer");

        assertTrue(dtoOpt.isPresent());
        assertEquals("Backend Developer", dtoOpt.get().getTitle());
        verify(careerTrackRepository, times(1)).findBySlugWithTechnologies("backend-developer");
    }

    @Test
    @DisplayName("getCareerBySlug returns empty when slug not found or blank")
    void testGetCareerBySlugNotFound() {
        when(careerTrackRepository.findBySlugWithTechnologies("non-existent"))
                .thenReturn(Optional.empty());

        Optional<CatalogCareerDto> result = catalogService.getCareerBySlug("non-existent");
        assertTrue(result.isEmpty());

        assertTrue(catalogService.getCareerBySlug("").isEmpty());
        assertTrue(catalogService.getCareerBySlug(null).isEmpty());
    }

    @Test
    @DisplayName("getAllTechnologies returns active technologies mapped to DTOs")
    void testGetAllTechnologies() {
        when(technologyRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(techJava, techSpring));

        List<CatalogTechnologyDto> dtos = catalogService.getAllTechnologies();

        assertEquals(2, dtos.size());
        assertEquals("java", dtos.get(0).getSlug());
        assertEquals("spring-boot", dtos.get(1).getSlug());
    }

    @Test
    @DisplayName("getTechnologyBySlug returns technology when present")
    void testGetTechnologyBySlug() {
        when(technologyRepository.findBySlug("java")).thenReturn(Optional.of(techJava));

        Optional<CatalogTechnologyDto> dtoOpt = catalogService.getTechnologyBySlug("java");

        assertTrue(dtoOpt.isPresent());
        assertEquals("Java", dtoOpt.get().getName());
    }

    @Test
    @DisplayName("getCompetenciesForTechnology returns competency list ordered")
    void testGetCompetenciesForTechnology() {
        when(competencyNodeRepository.findByTechnologySlug("java"))
                .thenReturn(List.of(compCoreJava, compAdvJava));

        List<CatalogCompetencyDto> dtos = catalogService.getCompetenciesForTechnology("java");

        assertEquals(2, dtos.size());
        assertEquals("java-core-oop", dtos.get(0).getCompetencyKey());
        assertEquals("java-multithreading", dtos.get(1).getCompetencyKey());
    }

    @Test
    @DisplayName("addPrerequisite successfully saves valid prerequisite")
    void testAddPrerequisiteSuccess() {
        when(prerequisiteRepository.existsByCompetencyAndPrerequisite(compAdvJava, compCoreJava))
                .thenReturn(false);
        when(prerequisiteRepository.findPrerequisiteNodes(compCoreJava.getId()))
                .thenReturn(Collections.emptyList());

        CompetencyPrerequisite savedPrereq = CompetencyPrerequisite.builder()
                .id(501L)
                .competency(compAdvJava)
                .prerequisite(compCoreJava)
                .reason("Requires OOP understanding")
                .build();

        when(prerequisiteRepository.save(any(CompetencyPrerequisite.class))).thenReturn(savedPrereq);

        CompetencyPrerequisite result = catalogService.addPrerequisite(compAdvJava, compCoreJava, "Requires OOP understanding");

        assertNotNull(result);
        assertEquals(compAdvJava, result.getCompetency());
        assertEquals(compCoreJava, result.getPrerequisite());
        verify(prerequisiteRepository).save(any(CompetencyPrerequisite.class));
    }

    @Test
    @DisplayName("addPrerequisite rejects self-dependency with IllegalArgumentException")
    void testAddPrerequisiteSelfDependency() {
        assertThrows(IllegalArgumentException.class, () ->
                catalogService.addPrerequisite(compCoreJava, compCoreJava, "Self reference"));

        CompetencyNode duplicateKeyNode = CompetencyNode.builder()
                .id(999L)
                .competencyKey(compCoreJava.getCompetencyKey())
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                catalogService.addPrerequisite(compCoreJava, duplicateKeyNode, "Self reference by key"));
    }

    @Test
    @DisplayName("addPrerequisite returns existing if duplicate relationship")
    void testAddPrerequisiteDuplicateHandledIdempotently() {
        CompetencyPrerequisite existing = CompetencyPrerequisite.builder()
                .id(502L)
                .competency(compAdvJava)
                .prerequisite(compCoreJava)
                .build();

        when(prerequisiteRepository.existsByCompetencyAndPrerequisite(compAdvJava, compCoreJava))
                .thenReturn(true);
        when(prerequisiteRepository.findByCompetencyAndPrerequisite(compAdvJava, compCoreJava))
                .thenReturn(Optional.of(existing));

        CompetencyPrerequisite result = catalogService.addPrerequisite(compAdvJava, compCoreJava, "Repeat attempt");

        assertNotNull(result);
        assertEquals(502L, result.getId());
        verify(prerequisiteRepository, never()).save(any(CompetencyPrerequisite.class));
    }

    @Test
    @DisplayName("addPrerequisite detects and rejects direct circular dependency")
    void testAddPrerequisiteDirectCircularDependency() {
        when(prerequisiteRepository.existsByCompetencyAndPrerequisite(compCoreJava, compAdvJava))
                .thenReturn(false);
        // isReachable(compAdvJava, compCoreJava) checks if compAdvJava -> ... -> compCoreJava
        // Suppose compAdvJava has compCoreJava as prerequisite already:
        when(prerequisiteRepository.findPrerequisiteNodes(compAdvJava.getId()))
                .thenReturn(List.of(compCoreJava));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                catalogService.addPrerequisite(compCoreJava, compAdvJava, "Cycle attempt"));

        assertTrue(ex.getMessage().contains("Circular dependency detected"));
    }

    @Test
    @DisplayName("addPrerequisite detects and rejects multi-step transitive cycle")
    void testAddPrerequisiteTransitiveCycle() {
        // A -> B -> C -> A
        // We want to add prerequisite: A depends on C (competency=A, prereq=C).
        // If C already has prereq B, and B has prereq A, then isReachable(C, A) is true!
        CompetencyNode nodeA = compCoreJava;
        CompetencyNode nodeB = compAdvJava;
        CompetencyNode nodeC = compSpringCore;

        when(prerequisiteRepository.existsByCompetencyAndPrerequisite(nodeA, nodeC))
                .thenReturn(false);
        when(prerequisiteRepository.findPrerequisiteNodes(nodeC.getId()))
                .thenReturn(List.of(nodeB));
        when(prerequisiteRepository.findPrerequisiteNodes(nodeB.getId()))
                .thenReturn(List.of(nodeA));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                catalogService.addPrerequisite(nodeA, nodeC, "Transitive cycle"));

        assertTrue(ex.getMessage().contains("Circular dependency detected"));
    }
}
