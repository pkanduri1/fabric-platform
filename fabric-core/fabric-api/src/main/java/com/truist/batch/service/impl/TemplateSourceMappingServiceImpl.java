package com.truist.batch.service.impl;

import com.truist.batch.dto.FieldMappingDto;
import com.truist.batch.dto.TemplateSourceMappingRequest;
import com.truist.batch.dto.TemplateSourceMappingResponse;
import com.truist.batch.entity.TemplateSourceMappingEntity;
import com.truist.batch.repository.TemplateSourceMappingRepository;
import com.truist.batch.service.SourceSystemService;
import com.truist.batch.service.TemplateSourceMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of TemplateSourceMappingService for US002 Template Configuration Enhancement
 */
@Service
@Transactional
public class TemplateSourceMappingServiceImpl implements TemplateSourceMappingService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateSourceMappingServiceImpl.class);

    @Autowired
    private TemplateSourceMappingRepository templateSourceMappingRepository;

    @Autowired
    private SourceSystemService sourceSystemService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public TemplateSourceMappingResponse saveTemplateSourceMapping(TemplateSourceMappingRequest request) {
        logger.info("Saving template-source mappings for {}/{} -> {} with {} field mappings", 
                request.getFileType(), request.getTransactionType(), request.getSourceSystemId(), 
                request.getFieldMappings().size());

        try {
            // Validate request
            validateRequest(request);

            // Delete existing mappings for this combination
            deleteExistingMappings(request.getFileType(), request.getTransactionType(), 
                                 request.getSourceSystemId(), request.getJobName());

            // Save new mappings using JdbcTemplate for reliability
            int savedCount = saveFieldMappings(request);

            // Update source system job count
            if (savedCount > 0) {
                sourceSystemService.incrementJobCount(request.getSourceSystemId());
            }

            String message = String.format("Successfully saved %d field mappings for job %s", 
                    savedCount, request.getJobName());
            
            logger.info(message);
            
            return TemplateSourceMappingResponse.success(
                    request.getFileType(), 
                    request.getTransactionType(),
                    request.getSourceSystemId(), 
                    request.getJobName(), 
                    request.getFieldMappings(), 
                    message
            );

        } catch (Exception e) {
            logger.error("Error saving template-source mappings for {}/{} -> {}", 
                    request.getFileType(), request.getTransactionType(), request.getSourceSystemId(), e);
            throw new RuntimeException("Failed to save template-source mappings: " + e.getMessage(), e);
        }
    }

    @Override
    public TemplateSourceMappingResponse getTemplateSourceMapping(String fileType, String transactionType, String sourceSystemId) {
        logger.info("Fetching template-source mappings for {}/{} -> {}", fileType, transactionType, sourceSystemId);

        try {
            String selectSql = """
                SELECT tsm.*, ss.name as source_system_name
                FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS tsm
                LEFT JOIN CM3INT.SOURCE_SYSTEMS ss ON tsm.source_system_id = ss.id
                WHERE tsm.file_type = ? 
                AND tsm.transaction_type = ? 
                AND tsm.source_system_id = ? 
                AND tsm.enabled = 'Y'
                ORDER BY tsm.target_position
            """;

            List<TemplateSourceMappingEntity> mappings = jdbcTemplate.query(selectSql, 
                    (rs, rowNum) -> {
                        TemplateSourceMappingEntity entity = new TemplateSourceMappingEntity();
                        entity.setId(rs.getLong("id"));
                        entity.setFileType(rs.getString("file_type"));
                        entity.setTransactionType(rs.getString("transaction_type"));
                        entity.setSourceSystemId(rs.getString("source_system_id"));
                        entity.setJobName(rs.getString("job_name"));
                        entity.setTargetFieldName(rs.getString("target_field_name"));
                        entity.setSourceFieldName(rs.getString("source_field_name"));
                        entity.setTransformationType(rs.getString("transformation_type"));
                        entity.setTransformationConfig(rs.getString("transformation_config"));
                        entity.setTargetPosition(rs.getInt("target_position"));
                        entity.setLength(rs.getInt("length"));
                        entity.setDataType(rs.getString("data_type"));
                        entity.setCreatedBy(rs.getString("created_by"));
                        entity.setCreatedDate(rs.getTimestamp("created_date") != null ? 
                                rs.getTimestamp("created_date").toLocalDateTime() : null);
                        return entity;
                    }, 
                    fileType, transactionType, sourceSystemId);

            if (mappings.isEmpty()) {
                logger.info("No existing mappings found for {}/{} -> {}", fileType, transactionType, sourceSystemId);
                return null;
            }

            // Convert to response
            List<FieldMappingDto> fieldMappings = mappings.stream()
                    .map(this::convertToFieldMappingDto)
                    .collect(Collectors.toList());

            TemplateSourceMappingResponse response = new TemplateSourceMappingResponse(
                    fileType, transactionType, sourceSystemId, 
                    mappings.get(0).getJobName(), fieldMappings);
            
            response.setCreatedBy(mappings.get(0).getCreatedBy());
            response.setCreatedDate(mappings.get(0).getCreatedDate());
            
            logger.info("Found {} existing mappings for {}/{} -> {}", 
                    fieldMappings.size(), fileType, transactionType, sourceSystemId);
            
            return response;

        } catch (Exception e) {
            logger.error("Error fetching template-source mappings for {}/{} -> {}", 
                    fileType, transactionType, sourceSystemId, e);
            throw new RuntimeException("Failed to fetch template-source mappings", e);
        }
    }

    @Override
    public List<TemplateSourceMappingResponse> getTemplateSourceMappings(String fileType, String transactionType) {
        logger.info("Fetching all template-source mappings for template: {}/{}", fileType, transactionType);

        try {
            String selectSql = """
                SELECT DISTINCT tsm.source_system_id, tsm.job_name, ss.name as source_system_name,
                       COUNT(tsm.id) as mapping_count, MIN(tsm.created_date) as created_date, 
                       MIN(tsm.created_by) as created_by
                FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS tsm
                LEFT JOIN CM3INT.SOURCE_SYSTEMS ss ON tsm.source_system_id = ss.id
                WHERE tsm.file_type = ? 
                AND tsm.transaction_type = ? 
                AND tsm.enabled = 'Y'
                GROUP BY tsm.source_system_id, tsm.job_name, ss.name
                ORDER BY MIN(tsm.created_date) DESC
            """;

            List<TemplateSourceMappingResponse> responses = jdbcTemplate.query(selectSql,
                    (rs, rowNum) -> {
                        TemplateSourceMappingResponse response = new TemplateSourceMappingResponse();
                        response.setFileType(fileType);
                        response.setTransactionType(transactionType);
                        response.setSourceSystemId(rs.getString("source_system_id"));
                        response.setSourceSystemName(rs.getString("source_system_name"));
                        response.setJobName(rs.getString("job_name"));
                        response.setMappingCount(rs.getInt("mapping_count"));
                        response.setCreatedBy(rs.getString("created_by"));
                        response.setCreatedDate(rs.getTimestamp("created_date") != null ? 
                                rs.getTimestamp("created_date").toLocalDateTime() : null);
                        response.setSuccess(true);
                        return response;
                    },
                    fileType, transactionType);

            logger.info("Found {} source systems configured for template {}/{}", 
                    responses.size(), fileType, transactionType);
            
            return responses;

        } catch (Exception e) {
            logger.error("Error fetching template-source mappings for template {}/{}", fileType, transactionType, e);
            throw new RuntimeException("Failed to fetch template-source mappings for template", e);
        }
    }

    @Override
    public TemplateSourceMappingResponse updateTemplateSourceMapping(TemplateSourceMappingRequest request) {
        logger.info("Updating template-source mappings for {}/{} -> {}", 
                request.getFileType(), request.getTransactionType(), request.getSourceSystemId());

        try {
            // Check if mapping exists
            if (!existsTemplateSourceMapping(request.getFileType(), request.getTransactionType(), request.getSourceSystemId())) {
                throw new IllegalArgumentException("Template-source mapping not found for update");
            }

            // Delete existing and save new (simpler than complex update logic)
            return saveTemplateSourceMapping(request);

        } catch (Exception e) {
            logger.error("Error updating template-source mappings for {}/{} -> {}", 
                    request.getFileType(), request.getTransactionType(), request.getSourceSystemId(), e);
            throw new RuntimeException("Failed to update template-source mappings: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteTemplateSourceMapping(String fileType, String transactionType, String sourceSystemId) {
        logger.info("Deleting template-source mappings for {}/{} -> {}", fileType, transactionType, sourceSystemId);

        try {
            String updateSql = """
                UPDATE CM3INT.TEMPLATE_SOURCE_MAPPINGS 
                SET enabled = 'N', modified_date = ? 
                WHERE file_type = ? 
                AND transaction_type = ? 
                AND source_system_id = ? 
                AND enabled = 'Y'
            """;

            int rowsAffected = jdbcTemplate.update(updateSql, 
                    LocalDateTime.now(), fileType, transactionType, sourceSystemId);

            if (rowsAffected > 0) {
                logger.info("Successfully disabled {} template-source mappings for {}/{} -> {}", 
                        rowsAffected, fileType, transactionType, sourceSystemId);
                
                // Decrement source system job count
                sourceSystemService.decrementJobCount(sourceSystemId);
            } else {
                logger.warn("No template-source mappings found to delete for {}/{} -> {}", 
                        fileType, transactionType, sourceSystemId);
            }

        } catch (Exception e) {
            logger.error("Error deleting template-source mappings for {}/{} -> {}", 
                    fileType, transactionType, sourceSystemId, e);
            throw new RuntimeException("Failed to delete template-source mappings", e);
        }
    }

    @Override
    public List<TemplateSourceMappingResponse> getUsageBySourceSystem(String sourceSystemId) {
        logger.info("Fetching template usage for source system: {}", sourceSystemId);

        try {
            String selectSql = """
                SELECT DISTINCT tsm.file_type, tsm.transaction_type, tsm.job_name, ss.name as source_system_name,
                       COUNT(tsm.id) as mapping_count, MIN(tsm.created_date) as created_date, 
                       MIN(tsm.created_by) as created_by
                FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS tsm
                LEFT JOIN CM3INT.SOURCE_SYSTEMS ss ON tsm.source_system_id = ss.id
                WHERE tsm.source_system_id = ? 
                AND tsm.enabled = 'Y'
                GROUP BY tsm.file_type, tsm.transaction_type, tsm.job_name, ss.name
                ORDER BY MIN(tsm.created_date) DESC
            """;

            List<TemplateSourceMappingResponse> responses = jdbcTemplate.query(selectSql,
                    (rs, rowNum) -> {
                        TemplateSourceMappingResponse response = new TemplateSourceMappingResponse();
                        response.setFileType(rs.getString("file_type"));
                        response.setTransactionType(rs.getString("transaction_type"));
                        response.setSourceSystemId(sourceSystemId);
                        response.setSourceSystemName(rs.getString("source_system_name"));
                        response.setJobName(rs.getString("job_name"));
                        response.setMappingCount(rs.getInt("mapping_count"));
                        response.setCreatedBy(rs.getString("created_by"));
                        response.setCreatedDate(rs.getTimestamp("created_date") != null ? 
                                rs.getTimestamp("created_date").toLocalDateTime() : null);
                        response.setSuccess(true);
                        return response;
                    },
                    sourceSystemId);

            logger.info("Found {} templates configured for source system {}", responses.size(), sourceSystemId);
            return responses;

        } catch (Exception e) {
            logger.error("Error fetching usage for source system: {}", sourceSystemId, e);
            throw new RuntimeException("Failed to fetch usage for source system", e);
        }
    }

    @Override
    public boolean existsTemplateSourceMapping(String fileType, String transactionType, String sourceSystemId) {
        try {
            String countSql = """
                SELECT COUNT(*) FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS 
                WHERE file_type = ? 
                AND transaction_type = ? 
                AND source_system_id = ? 
                AND enabled = 'Y'
            """;

            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class, 
                    fileType, transactionType, sourceSystemId);
            
            return count != null && count > 0;

        } catch (Exception e) {
            logger.error("Error checking if template-source mapping exists for {}/{} -> {}", 
                    fileType, transactionType, sourceSystemId, e);
            return false;
        }
    }

    @Override
    public int getMappingCount(String fileType, String transactionType, String sourceSystemId) {
        try {
            String countSql = """
                SELECT COUNT(*) FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS 
                WHERE file_type = ? 
                AND transaction_type = ? 
                AND source_system_id = ? 
                AND enabled = 'Y'
            """;

            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class, 
                    fileType, transactionType, sourceSystemId);
            
            return count != null ? count : 0;

        } catch (Exception e) {
            logger.error("Error getting mapping count for {}/{} -> {}", 
                    fileType, transactionType, sourceSystemId, e);
            return 0;
        }
    }

    // Private helper methods

    private void validateRequest(TemplateSourceMappingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Template source mapping request is required");
        }
        if (request.getFileType() == null || request.getFileType().trim().isEmpty()) {
            throw new IllegalArgumentException("File type is required");
        }
        if (request.getTransactionType() == null || request.getTransactionType().trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction type is required");
        }
        if (request.getSourceSystemId() == null || request.getSourceSystemId().trim().isEmpty()) {
            throw new IllegalArgumentException("Source system ID is required");
        }
        if (request.getJobName() == null || request.getJobName().trim().isEmpty()) {
            throw new IllegalArgumentException("Job name is required");
        }
        if (request.getFieldMappings() == null || request.getFieldMappings().isEmpty()) {
            throw new IllegalArgumentException("Field mappings are required");
        }

        // Validate that source system exists
        if (!sourceSystemService.existsById(request.getSourceSystemId())) {
            throw new IllegalArgumentException("Source system not found: " + request.getSourceSystemId());
        }
    }

    private void deleteExistingMappings(String fileType, String transactionType, String sourceSystemId, String jobName) {
        logger.debug("Deleting existing mappings for {}/{} -> {} job {}", 
                fileType, transactionType, sourceSystemId, jobName);

        String deleteSql = """
            DELETE FROM CM3INT.TEMPLATE_SOURCE_MAPPINGS 
            WHERE file_type = ? 
            AND transaction_type = ? 
            AND source_system_id = ? 
            AND job_name = ?
        """;

        int deletedCount = jdbcTemplate.update(deleteSql, fileType, transactionType, sourceSystemId, jobName);
        
        if (deletedCount > 0) {
            logger.debug("Deleted {} existing mappings", deletedCount);
        }
    }

    private int saveFieldMappings(TemplateSourceMappingRequest request) {
        String insertSql = """
            INSERT INTO CM3INT.TEMPLATE_SOURCE_MAPPINGS 
            (FILE_TYPE, TRANSACTION_TYPE, SOURCE_SYSTEM_ID, JOB_NAME, TARGET_FIELD_NAME, 
             SOURCE_FIELD_NAME, TRANSFORMATION_TYPE, TRANSFORMATION_CONFIG, TARGET_POSITION, 
             LENGTH, DATA_TYPE, CREATED_BY, CREATED_DATE, VERSION, ENABLED)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        List<Object[]> batchArgs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (FieldMappingDto mapping : request.getFieldMappings()) {
            Object[] args = {
                request.getFileType(),
                request.getTransactionType(),
                request.getSourceSystemId(),
                request.getJobName(),
                mapping.getTargetFieldName(),
                mapping.getSourceFieldName(),
                mapping.getTransformationType() != null ? mapping.getTransformationType() : "source",
                mapping.getTransformationConfig(),
                mapping.getTargetPosition(),
                mapping.getLength(),
                mapping.getDataType(),
                request.getCreatedBy(),
                now,
                1,
                "Y"
            };
            batchArgs.add(args);
        }

        int[] results = jdbcTemplate.batchUpdate(insertSql, batchArgs);
        int savedCount = 0;
        for (int result : results) {
            savedCount += result;
        }

        logger.debug("Saved {} field mappings using batch insert", savedCount);
        return savedCount;
    }

    private FieldMappingDto convertToFieldMappingDto(TemplateSourceMappingEntity entity) {
        FieldMappingDto dto = new FieldMappingDto();
        dto.setTargetFieldName(entity.getTargetFieldName());
        dto.setSourceFieldName(entity.getSourceFieldName());
        dto.setTransformationType(entity.getTransformationType());
        dto.setTransformationConfig(entity.getTransformationConfig());
        dto.setTargetPosition(entity.getTargetPosition());
        dto.setLength(entity.getLength());
        dto.setDataType(entity.getDataType());
        return dto;
    }
}