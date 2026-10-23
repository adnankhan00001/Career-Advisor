package com.careeradvisor.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CatalogModelTests {

    @Test
    @DisplayName("Technology builder sets default active and timestamps")
    void testTechnologyDefaults() {
        Technology tech = Technology.builder()
                .name("PostgreSQL")
                .slug("postgresql")
                .category("Database")
                .build();

        assertTrue(tech.isActive());
        assertNotNull(tech.getCreatedAt());
        assertNotNull(tech.getUpdatedAt());
        assertEquals("PostgreSQL", tech.getName());
        assertEquals("postgresql", tech.getSlug());
    }

    @Test
    @DisplayName("CareerTrack builder sets active and handles technology set")
    void testCareerTrackDefaults() {
        Technology react = Technology.builder().name("React").slug("react").build();
        Set<Technology> techSet = new HashSet<>();
        techSet.add(react);

        CareerTrack track = CareerTrack.builder()
                .title("Frontend Developer")
                .slug("frontend-developer")
                .category("Web Development")
                .technologies(techSet)
                .build();

        assertTrue(track.isActive());
        assertNotNull(track.getCreatedAt());
        assertNotNull(track.getUpdatedAt());
        assertEquals(1, track.getTechnologies().size());
    }

    @Test
    @DisplayName("CompetencyNode builder sets default level and active")
    void testCompetencyNodeDefaults() {
        CompetencyNode node = CompetencyNode.builder()
                .competencyKey("react-hooks")
                .title("React Hooks & State")
                .build();

        assertTrue(node.isActive());
        assertEquals("Intermediate", node.getLevel());
        assertNotNull(node.getCreatedAt());
        assertNotNull(node.getUpdatedAt());
    }

    @Test
    @DisplayName("CompetencyPrerequisite onCreate throws exception on identical competency and prerequisite")
    void testCompetencyPrerequisiteSelfDependencyGuard() {
        CompetencyNode node = CompetencyNode.builder()
                .id(1L)
                .competencyKey("react-hooks")
                .build();

        CompetencyPrerequisite prereq = CompetencyPrerequisite.builder()
                .competency(node)
                .prerequisite(node)
                .build();

        assertThrows(IllegalStateException.class, prereq::onCreate);
    }
}
