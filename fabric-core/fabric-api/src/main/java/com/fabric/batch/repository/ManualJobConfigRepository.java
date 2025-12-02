package com.fabric.batch.repository;

import com.fabric.batch.entity.ManualJobConfigEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Manual Job Configuration management.
 * Uses JdbcTemplate implementation for direct database access.
 */
public interface ManualJobConfigRepository {
    
    // Basic CRUD operations
    Optional<ManualJobConfigEntity> findById(String id);
    
    List<ManualJobConfigEntity> findAll();
    
    ManualJobConfigEntity save(ManualJobConfigEntity entity);
    
    void deleteById(String id);
    
    boolean existsById(String id);
    
    long count();
    
    // Query methods expected by service
    List<ManualJobConfigEntity> findByJobName(String jobName);
    
    List<ManualJobConfigEntity> findByStatus(String status);
    
    List<ManualJobConfigEntity> findBySourceSystem(String sourceSystem);
    
    List<ManualJobConfigEntity> findByJobType(String jobType);
    
    // Combined filter methods
    List<ManualJobConfigEntity> findByJobTypeAndSourceSystemAndStatus(String jobType, String sourceSystem, String status);
    
    List<ManualJobConfigEntity> findByJobTypeAndStatus(String jobType, String status);
    
    List<ManualJobConfigEntity> findBySourceSystemAndStatus(String sourceSystem, String status);
    
    // Existence and count methods
    boolean existsActiveConfigurationByJobName(String jobName);
    
    long countByStatus(String status);
    
    // Time-based query methods
    long countConfigurationsCreatedToday();
    
    long countConfigurationsModifiedThisWeek();
}