package com.truist.batch.repository;

import com.truist.batch.entity.TempStagingDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Epic 3: Temporary Staging Definition Repository
 * 
 * JPA repository for managing temporary staging table definitions with
 * lifecycle management, performance monitoring, and cleanup operations.
 * 
 * @author Senior Full Stack Developer Agent
 * @version 1.0
 * @since Epic 3
 */
@Repository
public interface TempStagingDefinitionRepository extends JpaRepository<TempStagingDefinitionEntity, Long> {

    /**
     * Find staging tables by execution ID
     */
    List<TempStagingDefinitionEntity> findByExecutionId(String executionId);

    /**
     * Find staging table by name
     */
    Optional<TempStagingDefinitionEntity> findByStagingTableName(String stagingTableName);

    /**
     * Find expired tables ready for cleanup
     */
    @Query("""
        SELECT tsd FROM TempStagingDefinitionEntity tsd 
        WHERE tsd.droppedTimestamp IS NULL
        AND tsd.cleanupPolicy IN ('AUTO_DROP', 'ARCHIVE_THEN_DROP')
        AND tsd.createdTimestamp < (CURRENT_TIMESTAMP - (tsd.ttlHours * INTERVAL '1' HOUR))
        ORDER BY tsd.createdTimestamp ASC
        """)
    List<TempStagingDefinitionEntity> findExpiredTablesForCleanup();

    /**
     * Find active staging tables
     */
    @Query("""
        SELECT tsd FROM TempStagingDefinitionEntity tsd 
        WHERE tsd.droppedTimestamp IS NULL
        ORDER BY tsd.createdTimestamp DESC
        """)
    List<TempStagingDefinitionEntity> findActiveTables();

    /**
     * Find large staging tables (above size threshold)
     */
    @Query("""
        SELECT tsd FROM TempStagingDefinitionEntity tsd 
        WHERE tsd.droppedTimestamp IS NULL
        AND tsd.tableSizeMb > :sizeMbThreshold
        ORDER BY tsd.tableSizeMb DESC
        """)
    List<TempStagingDefinitionEntity> findLargeTables(@Param("sizeMbThreshold") Integer sizeMbThreshold);
}