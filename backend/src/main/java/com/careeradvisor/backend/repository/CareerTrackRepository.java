package com.careeradvisor.backend.repository;

import com.careeradvisor.backend.model.CareerTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CareerTrackRepository extends JpaRepository<CareerTrack, Long> {

    Optional<CareerTrack> findBySlug(String slug);

    Optional<CareerTrack> findByTitleIgnoreCase(String title);

    List<CareerTrack> findByActiveTrueOrderByTitleAsc();

    boolean existsBySlug(String slug);

    boolean existsByTitleIgnoreCase(String title);

    @Query("SELECT DISTINCT c FROM CareerTrack c LEFT JOIN FETCH c.technologies WHERE c.slug = :slug")
    Optional<CareerTrack> findBySlugWithTechnologies(@Param("slug") String slug);

    @Query("SELECT DISTINCT c FROM CareerTrack c LEFT JOIN FETCH c.technologies WHERE c.active = true ORDER BY c.title ASC")
    List<CareerTrack> findAllActiveWithTechnologies();
}
