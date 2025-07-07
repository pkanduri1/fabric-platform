package com.truist.batch.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.truist.batch.entity.BatchConfigurationEntity;

@Repository
public interface BatchConfigurationRepository extends JpaRepository<BatchConfigurationEntity, String> {
    
    
    Optional<BatchConfigurationEntity> findBySourceSystemAndJobNameAndTransactionType(
        String sourceSystem, String jobName, String transactionType);
    
    List<BatchConfigurationEntity> findBySourceSystem(String sourceSystem);
    
    @Query("SELECT c FROM BatchConfigurationEntity c WHERE c.enabled = 'Y'")
    List<BatchConfigurationEntity> findAllEnabled();
    
    @Query("SELECT COUNT(c) FROM BatchConfigurationEntity c WHERE c.sourceSystem = :sourceSystem")
    long countBySourceSystem(@Param("sourceSystem") String sourceSystem);
    
    @Query("SELECT b FROM BatchConfigurationEntity b WHERE b.sourceSystem = :sourceSystem AND b.jobName = :jobName ORDER BY b.transactionType")
    List<BatchConfigurationEntity> findBySourceSystemAndJobName(@Param("sourceSystem") String sourceSystem, 
                                                               @Param("jobName") String jobName);
}
