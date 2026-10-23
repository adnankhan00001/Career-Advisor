package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.Technology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TechnologyRepository extends JpaRepository<Technology, Long> {

    Optional<Technology> findBySlug(String slug);

    Optional<Technology> findByNameIgnoreCase(String name);

    List<Technology> findByActiveTrueOrderByNameAsc();

    List<Technology> findByCategoryIgnoreCaseAndActiveTrueOrderByNameAsc(String category);

    boolean existsBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT t FROM CareerTrack c JOIN c.technologies t WHERE c.slug = :careerSlug AND t.active = true ORDER BY t.name ASC")
    List<Technology> findByCareerTrackSlug(@Param("careerSlug") String careerSlug);
}
