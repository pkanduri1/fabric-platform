package com.fabric.batch.dao;

import com.fabric.batch.model.BatchConfiguration;
import java.util.List;
import java.util.Optional;

public interface BatchConfigurationDao {
    
    BatchConfiguration save(BatchConfiguration configuration);
    
    Optional<BatchConfiguration> findById(String id);
    
    Optional<BatchConfiguration> findBySourceSystemAndJobNameAndTransactionType(
            String sourceSystem, String jobName, String transactionType);
    
    List<BatchConfiguration> findBySourceSystem(String sourceSystem);
    
    List<BatchConfiguration> findAllEnabled();
    
    List<BatchConfiguration> findBySourceSystemAndJobName(String sourceSystem, String jobName);
    
    long countBySourceSystem(String sourceSystem);
    
    void deleteById(String id);
    
    boolean existsById(String id);
}