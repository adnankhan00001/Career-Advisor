package com.careeradvisor.backend.dto;

/**
 * Spring Data JPA projection for database-level GROUP BY category aggregations.
 */
public interface CategoryCountProjection {
    String getCategory();
    Long getCount();
}
