package com.truist.batch.service.impl;

import com.truist.batch.dto.CreateSourceSystemRequest;
import com.truist.batch.dto.SourceSystemInfo;
import com.truist.batch.dto.SourceSystemWithUsage;
import com.truist.batch.entity.SourceSystemEntity;
import com.truist.batch.entity.JobDefinitionEntity;
import com.truist.batch.repository.SourceSystemRepository;
import com.truist.batch.repository.JobDefinitionRepository;
import com.truist.batch.service.SourceSystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of SourceSystemService for US002 Template Configuration Enhancement
 */
@Service
@Transactional
public class SourceSystemServiceImpl implements SourceSystemService {

    private static final Logger logger = LoggerFactory.getLogger(SourceSystemServiceImpl.class);

    @Autowired
    private SourceSystemRepository sourceSystemRepository;

    @Autowired
    private JobDefinitionRepository jobDefinitionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<SourceSystemInfo> getAllEnabledSourceSystems() {
        logger.info("Fetching all enabled source systems");
        try {
            List<SourceSystemEntity> entities = sourceSystemRepository.findByEnabledOrderByName("Y");
            return entities.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching enabled source systems", e);
            throw new RuntimeException("Failed to fetch enabled source systems", e);
        }
    }

    @Override
    public List<SourceSystemInfo> getAllSourceSystems() {
        logger.info("Fetching all source systems");
        try {
            List<SourceSystemEntity> entities = sourceSystemRepository.findAllByOrderByName();
            return entities.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching all source systems", e);
            throw new RuntimeException("Failed to fetch all source systems", e);
        }
    }

    @Override
    public SourceSystemInfo getSourceSystemById(String id) {
        logger.info("Fetching source system by ID: {}", id);
        try {
            return sourceSystemRepository.findById(id)
                    .map(this::convertToDto)
                    .orElse(null);
        } catch (Exception e) {
            logger.error("Error fetching source system by ID: {}", id, e);
            throw new RuntimeException("Failed to fetch source system: " + id, e);
        }
    }

    @Override
    public boolean existsById(String id) {
        try {
            return sourceSystemRepository.existsById(id);
        } catch (Exception e) {
            logger.error("Error checking if source system exists: {}", id, e);
            return false;
        }
    }

    @Override
    public SourceSystemInfo createSourceSystem(CreateSourceSystemRequest request) {
        logger.info("Creating new source system: {}", request.getId());
        
        try {
            // Validate request
            if (request.getId() == null || request.getId().trim().isEmpty()) {
                throw new IllegalArgumentException("Source system ID is required");
            }
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Source system name is required");
            }
            if (request.getType() == null || request.getType().trim().isEmpty()) {
                throw new IllegalArgumentException("Source system type is required");
            }

            // Check if already exists
            if (existsById(request.getId())) {
                throw new IllegalArgumentException("Source system with ID " + request.getId() + " already exists");
            }

            // Create LocalDateTime for created_date
            LocalDateTime createdDate = LocalDateTime.now();

            // Use JdbcTemplate for direct insert to avoid potential JPA issues
            String insertSql = """
                INSERT INTO CM3INT.SOURCE_SYSTEMS 
                (ID, NAME, TYPE, DESCRIPTION, CONNECTION_STRING, ENABLED, CREATED_DATE, JOB_COUNT)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

            int rowsAffected = jdbcTemplate.update(insertSql,
                    request.getId(),
                    request.getName(),
                    request.getType(),
                    request.getDescription(),
                    request.getConnectionString(),
                    request.isEnabled() ? "Y" : "N",
                    createdDate,
                    request.getJobs() != null ? request.getJobs().size() : 0
            );

            if (rowsAffected == 1) {
                logger.info("Successfully created source system: {}", request.getId());
                
                // Create associated jobs
                int jobsCreated = 0;
                if (request.getJobs() != null && !request.getJobs().isEmpty()) {
                    for (CreateSourceSystemRequest.JobRequest jobRequest : request.getJobs()) {
                        try {
                            JobDefinitionEntity jobEntity = new JobDefinitionEntity();
                            jobEntity.setSourceSystemId(request.getId());
                            jobEntity.setJobName(jobRequest.getName());
                            jobEntity.setDescription(jobRequest.getDescription());
                            jobEntity.setTransactionTypes(jobRequest.getTransactionTypes());
                            jobEntity.setEnabled("Y");
                            
                            jobDefinitionRepository.save(jobEntity);
                            jobsCreated++;
                            logger.info("Created job definition: {}-{}", request.getId(), jobRequest.getName());
                        } catch (Exception e) {
                            logger.warn("Failed to create job definition {}-{}: {}", request.getId(), jobRequest.getName(), e.getMessage());
                        }
                    }
                    
                    // Update job count in source system
                    if (jobsCreated > 0) {
                        String updateJobCountSql = "UPDATE CM3INT.SOURCE_SYSTEMS SET JOB_COUNT = ? WHERE ID = ?";
                        jdbcTemplate.update(updateJobCountSql, jobsCreated, request.getId());
                    }
                }
                
                // Construct the response DTO
                SourceSystemInfo createdSystem = new SourceSystemInfo();
                createdSystem.setId(request.getId());
                createdSystem.setName(request.getName());
                createdSystem.setType(request.getType());
                createdSystem.setDescription(request.getDescription());
                createdSystem.setConnectionString(request.getConnectionString());
                createdSystem.setEnabled(request.isEnabled());
                createdSystem.setCreatedDate(createdDate);
                createdSystem.setJobCount(jobsCreated);
                
                return createdSystem;
            } else {
                throw new RuntimeException("Failed to create source system - no rows affected");
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for creating source system: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error creating source system: {}", request.getId(), e);
            throw new RuntimeException("Failed to create source system: " + request.getId(), e);
        }
    }

    @Override
    public SourceSystemInfo updateSourceSystem(String id, CreateSourceSystemRequest request) {
        logger.info("Updating source system: {}", id);
        
        try {
            // Validate request
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Source system name is required");
            }
            if (request.getType() == null || request.getType().trim().isEmpty()) {
                throw new IllegalArgumentException("Source system type is required");
            }

            // Check if exists
            if (!existsById(id)) {
                throw new IllegalArgumentException("Source system not found: " + id);
            }

            // Use JdbcTemplate for direct update
            String updateSql = """
                UPDATE CM3INT.SOURCE_SYSTEMS 
                SET NAME = ?, TYPE = ?, DESCRIPTION = ?, CONNECTION_STRING = ?, ENABLED = ?
                WHERE ID = ?
            """;

            int rowsAffected = jdbcTemplate.update(updateSql,
                    request.getName(),
                    request.getType(),
                    request.getDescription(),
                    request.getConnectionString(),
                    request.isEnabled() ? "Y" : "N",
                    id
            );

            if (rowsAffected == 1) {
                logger.info("Successfully updated source system: {}", id);
                return getSourceSystemById(id);
            } else {
                throw new RuntimeException("Failed to update source system - no rows affected");
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for updating source system: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error updating source system: {}", id, e);
            throw new RuntimeException("Failed to update source system: " + id, e);
        }
    }

    @Override
    public void disableSourceSystem(String id) {
        logger.info("Disabling source system: {}", id);
        try {
            String updateSql = "UPDATE CM3INT.SOURCE_SYSTEMS SET ENABLED = 'N' WHERE ID = ?";
            int rowsAffected = jdbcTemplate.update(updateSql, id);
            
            if (rowsAffected == 1) {
                logger.info("Successfully disabled source system: {}", id);
            } else {
                throw new RuntimeException("Failed to disable source system - no rows affected");
            }
        } catch (Exception e) {
            logger.error("Error disabling source system: {}", id, e);
            throw new RuntimeException("Failed to disable source system: " + id, e);
        }
    }

    @Override
    public void enableSourceSystem(String id) {
        logger.info("Enabling source system: {}", id);
        try {
            String updateSql = "UPDATE CM3INT.SOURCE_SYSTEMS SET ENABLED = 'Y' WHERE ID = ?";
            int rowsAffected = jdbcTemplate.update(updateSql, id);
            
            if (rowsAffected == 1) {
                logger.info("Successfully enabled source system: {}", id);
            } else {
                throw new RuntimeException("Failed to enable source system - no rows affected");
            }
        } catch (Exception e) {
            logger.error("Error enabling source system: {}", id, e);
            throw new RuntimeException("Failed to enable source system: " + id, e);
        }
    }

    @Override
    public List<SourceSystemWithUsage> getSourceSystemsWithUsageForTemplate(String fileType, String transactionType) {
        logger.info("Fetching source systems with usage for template: {}/{}", fileType, transactionType);
        
        try {
            // Get all enabled source systems
            List<SourceSystemInfo> allSystems = getAllEnabledSourceSystems();
            
            // For each source system, check if it has existing configurations for this template
            return allSystems.stream()
                    .map(system -> {
                        SourceSystemWithUsage usage = SourceSystemWithUsage.fromSourceSystemInfo(system);
                        
                        // Check for existing configurations using batch_configurations table
                        // This assumes batch configurations are stored with source_system field
                        String checkExistingSql = """
                            SELECT COUNT(*), MAX(created_date) as last_configured, 
                                   LISTAGG(job_name, ', ') WITHIN GROUP (ORDER BY created_date) as job_names
                            FROM (
                                SELECT job_name, created_date 
                                FROM batch_configurations 
                                WHERE source_system = ? 
                                AND file_type = ? 
                                AND transaction_type = ?
                                AND ROWNUM <= 5
                            )
                        """;
                        
                        try {
                            jdbcTemplate.queryForObject(checkExistingSql, 
                                    (rs, rowNum) -> {
                                        int count = rs.getInt(1);
                                        usage.setHasExistingConfiguration(count > 0);
                                        if (count > 0) {
                                            usage.setLastConfigured(rs.getTimestamp(2) != null ? 
                                                    rs.getTimestamp(2).toLocalDateTime() : null);
                                            usage.setExistingJobName(rs.getString(3));
                                        }
                                        usage.setTemplateCount(count);
                                        return usage;
                                    }, 
                                    system.getId(), fileType, transactionType);
                        } catch (Exception e) {
                            logger.debug("No existing configurations found for system {} and template {}/{}", 
                                    system.getId(), fileType, transactionType);
                            // Leave defaults (hasExistingConfiguration = false)
                        }
                        
                        return usage;
                    })
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("Error fetching source systems with usage for template {}/{}", fileType, transactionType, e);
            throw new RuntimeException("Failed to fetch source systems with usage", e);
        }
    }

    @Override
    public void incrementJobCount(String sourceSystemId) {
        logger.info("Incrementing job count for source system: {}", sourceSystemId);
        try {
            String updateSql = "UPDATE CM3INT.SOURCE_SYSTEMS SET JOB_COUNT = NVL(JOB_COUNT, 0) + 1 WHERE ID = ?";
            jdbcTemplate.update(updateSql, sourceSystemId);
        } catch (Exception e) {
            logger.error("Error incrementing job count for source system: {}", sourceSystemId, e);
            // Don't throw exception as this is a supporting operation
        }
    }

    @Override
    public void decrementJobCount(String sourceSystemId) {
        logger.info("Decrementing job count for source system: {}", sourceSystemId);
        try {
            String updateSql = "UPDATE CM3INT.SOURCE_SYSTEMS SET JOB_COUNT = GREATEST(NVL(JOB_COUNT, 0) - 1, 0) WHERE ID = ?";
            jdbcTemplate.update(updateSql, sourceSystemId);
        } catch (Exception e) {
            logger.error("Error decrementing job count for source system: {}", sourceSystemId, e);
            // Don't throw exception as this is a supporting operation
        }
    }

    /**
     * Convert SourceSystemEntity to SourceSystemInfo DTO
     */
    private SourceSystemInfo convertToDto(SourceSystemEntity entity) {
        SourceSystemInfo dto = new SourceSystemInfo();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setDescription(entity.getDescription());
        dto.setConnectionString(entity.getConnectionString());
        dto.setEnabled(entity.isEnabled());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setJobCount(entity.getJobCount());
        return dto;
    }
}