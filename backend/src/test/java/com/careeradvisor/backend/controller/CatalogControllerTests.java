package com.careeradvisor.backend.controller;

import com.careeradvisor.backend.dto.CatalogCareerDto;
import com.careeradvisor.backend.dto.CatalogCompetencyDto;
import com.careeradvisor.backend.dto.CatalogTechnologyDto;
import com.careeradvisor.backend.service.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTests {

    private MockMvc mockMvc;

    @Mock
    private CatalogService catalogService;

    @InjectMocks
    private CatalogController catalogController;

    private CatalogCareerDto sampleCareer;
    private CatalogTechnologyDto sampleTech;
    private CatalogCompetencyDto sampleComp;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(catalogController).build();

        sampleTech = CatalogTechnologyDto.builder()
                .id(1L)
                .name("Docker")
                .slug("docker")
                .category("DevOps & Cloud")
                .icon("docker")
                .description("Containerization engine")
                .build();

        sampleCareer = CatalogCareerDto.builder()
                .id(10L)
                .title("DevOps Engineer")
                .slug("devops-engineer")
                .category("Infrastructure & Operations")
                .level("MID_LEVEL")
                .technologies(List.of(sampleTech))
                .build();

        sampleComp = CatalogCompetencyDto.builder()
                .id(100L)
                .competencyKey("docker-container-lifecycle")
                .title("Container Lifecycle & Dockerfile")
                .level("BEGINNER")
                .sequenceOrder(1)
                .prerequisiteKeys(List.of())
                .build();
    }

    @Test
    @DisplayName("GET /api/catalog/careers returns 200 OK with career list")
    void testGetAllCareers() throws Exception {
        when(catalogService.getAllCareers()).thenReturn(List.of(sampleCareer));

        mockMvc.perform(get("/api/catalog/careers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("devops-engineer"))
                .andExpect(jsonPath("$[0].title").value("DevOps Engineer"))
                .andExpect(jsonPath("$[0].technologies[0].slug").value("docker"));

        verify(catalogService, times(1)).getAllCareers();
    }

    @Test
    @DisplayName("GET /api/catalog/careers/{slug} returns 200 OK when found")
    void testGetCareerBySlugFound() throws Exception {
        when(catalogService.getCareerBySlug("devops-engineer")).thenReturn(Optional.of(sampleCareer));

        mockMvc.perform(get("/api/catalog/careers/devops-engineer")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("devops-engineer"))
                .andExpect(jsonPath("$.title").value("DevOps Engineer"));

        verify(catalogService, times(1)).getCareerBySlug("devops-engineer");
    }

    @Test
    @DisplayName("GET /api/catalog/careers/{slug} returns 404 Not Found when absent")
    void testGetCareerBySlugNotFound() throws Exception {
        when(catalogService.getCareerBySlug("unknown-slug")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/catalog/careers/unknown-slug")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(catalogService, times(1)).getCareerBySlug("unknown-slug");
    }

    @Test
    @DisplayName("GET /api/catalog/technologies returns 200 OK with technology list")
    void testGetAllTechnologies() throws Exception {
        when(catalogService.getAllTechnologies()).thenReturn(List.of(sampleTech));

        mockMvc.perform(get("/api/catalog/technologies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("docker"))
                .andExpect(jsonPath("$[0].name").value("Docker"));

        verify(catalogService, times(1)).getAllTechnologies();
    }

    @Test
    @DisplayName("GET /api/catalog/technologies/{slug} returns 200 OK when found")
    void testGetTechnologyBySlugFound() throws Exception {
        when(catalogService.getTechnologyBySlug("docker")).thenReturn(Optional.of(sampleTech));

        mockMvc.perform(get("/api/catalog/technologies/docker")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("docker"))
                .andExpect(jsonPath("$.name").value("Docker"));

        verify(catalogService, times(1)).getTechnologyBySlug("docker");
    }

    @Test
    @DisplayName("GET /api/catalog/technologies/{slug} returns 404 when absent")
    void testGetTechnologyBySlugNotFound() throws Exception {
        when(catalogService.getTechnologyBySlug("unknown-tech")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/catalog/technologies/unknown-tech")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(catalogService, times(1)).getTechnologyBySlug("unknown-tech");
    }

    @Test
    @DisplayName("GET /api/catalog/technologies/{slug}/competencies returns 200 OK with competencies")
    void testGetCompetenciesForTechnologyFound() throws Exception {
        when(catalogService.getTechnologyBySlug("docker")).thenReturn(Optional.of(sampleTech));
        when(catalogService.getCompetenciesForTechnology("docker")).thenReturn(List.of(sampleComp));

        mockMvc.perform(get("/api/catalog/technologies/docker/competencies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].competencyKey").value("docker-container-lifecycle"));

        verify(catalogService, times(1)).getCompetenciesForTechnology("docker");
    }

    @Test
    @DisplayName("GET /api/catalog/technologies/{slug}/competencies returns 404 when technology absent")
    void testGetCompetenciesForTechnologyNotFound() throws Exception {
        when(catalogService.getTechnologyBySlug("unknown-tech")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/catalog/technologies/unknown-tech/competencies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(catalogService, never()).getCompetenciesForTechnology("unknown-tech");
    }
}
