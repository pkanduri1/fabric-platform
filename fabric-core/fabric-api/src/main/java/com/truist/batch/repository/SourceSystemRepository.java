package com.truist.batch.repository;

import com.truist.batch.entity.SourceSystemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for SourceSystemEntity in US002 Template Configuration Enhancement
 * Maps to existing CM3INT.SOURCE_SYSTEMS table
 */
@Repository
public interface SourceSystemRepository extends JpaRepository<SourceSystemEntity, String> {

    /**
     * Find all enabled source systems ordered by name
     * 
     * @param enabled The enabled flag ('Y' or 'N')
     * @return List of enabled source systems
     */
    List<SourceSystemEntity> findByEnabledOrderByName(String enabled);

    /**
     * Find all source systems ordered by name
     * 
     * @return List of all source systems
     */
    List<SourceSystemEntity> findAllByOrderByName();

    /**
     * Find source systems by type
     * 
     * @param type The source system type
     * @return List of source systems of the specified type
     */
    List<SourceSystemEntity> findByTypeOrderByName(String type);

    /**
     * Find enabled source systems by type
     * 
     * @param type The source system type
     * @param enabled The enabled flag ('Y' or 'N')
     * @return List of enabled source systems of the specified type
     */
    List<SourceSystemEntity> findByTypeAndEnabledOrderByName(String type, String enabled);

    /**
     * Count enabled source systems
     * 
     * @param enabled The enabled flag ('Y' or 'N')
     * @return Count of enabled source systems
     */
    long countByEnabled(String enabled);

    /**
     * Find source systems with job count greater than specified value
     * 
     * @param jobCount The minimum job count
     * @return List of source systems with jobs
     */
    @Query("SELECT s FROM SourceSystemEntity s WHERE s.jobCount > :jobCount ORDER BY s.jobCount DESC, s.name")
    List<SourceSystemEntity> findByJobCountGreaterThanOrderByJobCountDescNameAsc(@Param("jobCount") Integer jobCount);

    /**
     * Find source systems by name containing (case insensitive)
     * 
     * @param name The name pattern to search for
     * @return List of matching source systems
     */
    @Query("SELECT s FROM SourceSystemEntity s WHERE UPPER(s.name) LIKE UPPER(CONCAT('%', :name, '%')) ORDER BY s.name")
    List<SourceSystemEntity> findByNameContainingIgnoreCaseOrderByName(@Param("name") String name);

    /**
     * Check if source system exists by ID (case insensitive)
     * 
     * @param id The source system ID
     * @return true if exists, false otherwise
     */
    @Query("SELECT COUNT(s) > 0 FROM SourceSystemEntity s WHERE UPPER(s.id) = UPPER(:id)")
    boolean existsByIdIgnoreCase(@Param("id") String id);
}