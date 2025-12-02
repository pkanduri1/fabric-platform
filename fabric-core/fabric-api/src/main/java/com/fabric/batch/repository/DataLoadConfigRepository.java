package com.fabric.batch.repository;

import com.fabric.batch.entity.DataLoadConfigEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for data load configuration entities
 * Stub implementation to satisfy dependency injection
 */
@Repository
public class DataLoadConfigRepository {
    
    public Optional<DataLoadConfigEntity> findById(String id) {
        return Optional.empty();
    }
    
    public List<DataLoadConfigEntity> findAll() {
        return List.of();
    }
    
    public DataLoadConfigEntity save(DataLoadConfigEntity entity) {
        return entity;
    }
    
    public void deleteById(String id) {
        // Stub implementation
    }
    
    public List<DataLoadConfigEntity> findAllEnabled() {
        return List.of();
    }
    
    public Optional<DataLoadConfigEntity> findByConfigurationId(String configId) {
        return Optional.empty();
    }
}