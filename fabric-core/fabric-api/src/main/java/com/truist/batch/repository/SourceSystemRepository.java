package com.truist.batch.repository;

import com.truist.batch.entity.SourceSystemEntity;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SourceSystemEntity in US002 Template Configuration Enhancement
 * Maps to existing CM3INT.SOURCE_SYSTEMS table
 * 
 * Converted from JPA to JdbcTemplate-based repository to eliminate JPA dependencies
 */
@Repository
public interface SourceSystemRepository {

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
    List<SourceSystemEntity> findByJobCountGreaterThanOrderByJobCountDescNameAsc(@Param("jobCount") Integer jobCount);

    /**
     * Find source systems by name containing (case insensitive)
     * 
     * @param name The name pattern to search for
     * @return List of matching source systems
     */
    List<SourceSystemEntity> findByNameContainingIgnoreCaseOrderByName(@Param("name") String name);

    /**
     * Check if source system exists by ID (case insensitive)
     * 
     * @param id The source system ID
     * @return true if exists, false otherwise
     */
    boolean existsByIdIgnoreCase(@Param("id") String id);

    // Standard CRUD operations to replace JpaRepository methods
    <S extends SourceSystemEntity> S save(S entity);
    <S extends SourceSystemEntity> Iterable<S> saveAll(Iterable<S> entities);
    Optional<SourceSystemEntity> findById(String id);
    boolean existsById(String id);
    List<SourceSystemEntity> findAll();
    Iterable<SourceSystemEntity> findAllById(Iterable<String> ids);
    long count();
    void deleteById(String id);
    void delete(SourceSystemEntity entity);
    void deleteAllById(Iterable<? extends String> ids);
    void deleteAll(Iterable<? extends SourceSystemEntity> entities);
    void deleteAll();
}