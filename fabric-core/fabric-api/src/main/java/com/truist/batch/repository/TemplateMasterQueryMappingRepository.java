package com.truist.batch.repository;

import com.truist.batch.entity.TemplateMasterQueryMappingEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class TemplateMasterQueryMappingRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<TemplateMasterQueryMappingEntity> rowMapper = new RowMapper<TemplateMasterQueryMappingEntity>() {
        @Override
        public TemplateMasterQueryMappingEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            TemplateMasterQueryMappingEntity entity = new TemplateMasterQueryMappingEntity();
            entity.setMappingId(rs.getString("MAPPING_ID"));
            entity.setConfigId(rs.getString("CONFIG_ID"));
            entity.setMasterQueryId(rs.getString("MASTER_QUERY_ID"));
            entity.setQueryName(rs.getString("QUERY_NAME"));
            entity.setQuerySql(rs.getString("QUERY_SQL"));
            entity.setQueryDescription(rs.getString("QUERY_DESCRIPTION"));
            entity.setQueryType(rs.getString("QUERY_TYPE"));
            entity.setMaxExecutionTimeSeconds(rs.getObject("MAX_EXECUTION_TIME_SECONDS", Integer.class));
            entity.setMaxResultRows(rs.getObject("MAX_RESULT_ROWS", Integer.class));
            entity.setQueryParameters(rs.getString("QUERY_PARAMETERS"));
            entity.setParameterValidationRules(rs.getString("PARAMETER_VALIDATION_RULES"));
            entity.setStatus(rs.getString("STATUS"));
            entity.setIsReadOnly(rs.getString("IS_READ_ONLY"));
            entity.setSecurityClassification(rs.getString("SECURITY_CLASSIFICATION"));
            entity.setRequiresApproval(rs.getString("REQUIRES_APPROVAL"));
            entity.setCreatedBy(rs.getString("CREATED_BY"));

            java.sql.Timestamp createdTimestamp = rs.getTimestamp("CREATED_DATE");
            if (createdTimestamp != null) {
                entity.setCreatedDate(createdTimestamp.toLocalDateTime());
            }

            entity.setUpdatedBy(rs.getString("UPDATED_BY"));

            java.sql.Timestamp updatedTimestamp = rs.getTimestamp("UPDATED_DATE");
            if (updatedTimestamp != null) {
                entity.setUpdatedDate(updatedTimestamp.toLocalDateTime());
            }

            entity.setCorrelationId(rs.getString("CORRELATION_ID"));

            return entity;
        }
    };

    public TemplateMasterQueryMappingEntity save(TemplateMasterQueryMappingEntity entity) {
        // Check if exists (by CONFIG_ID since we usually have one query per config in
        // this context)
        // Or by MAPPING_ID if provided.

        boolean exists = false;
        if (entity.getMappingId() != null) {
            String checkSql = "SELECT COUNT(*) FROM cm3int.template_master_query_mapping WHERE MAPPING_ID = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, entity.getMappingId());
            exists = count != null && count > 0;
        }

        if (exists) {
            // Update
            String updateSql = "UPDATE cm3int.template_master_query_mapping SET " +
                    "CONFIG_ID = ?, MASTER_QUERY_ID = ?, QUERY_NAME = ?, QUERY_SQL = ?, " +
                    "QUERY_DESCRIPTION = ?, QUERY_TYPE = ?, MAX_EXECUTION_TIME_SECONDS = ?, " +
                    "MAX_RESULT_ROWS = ?, QUERY_PARAMETERS = ?, PARAMETER_VALIDATION_RULES = ?, " +
                    "STATUS = ?, IS_READ_ONLY = ?, SECURITY_CLASSIFICATION = ?, REQUIRES_APPROVAL = ?, " +
                    "UPDATED_BY = ?, UPDATED_DATE = SYSDATE, CORRELATION_ID = ? " +
                    "WHERE MAPPING_ID = ?";

            jdbcTemplate.update(updateSql,
                    entity.getConfigId(), entity.getMasterQueryId(), entity.getQueryName(), entity.getQuerySql(),
                    entity.getQueryDescription(), entity.getQueryType(), entity.getMaxExecutionTimeSeconds(),
                    entity.getMaxResultRows(), entity.getQueryParameters(), entity.getParameterValidationRules(),
                    entity.getStatus(), entity.getIsReadOnly(), entity.getSecurityClassification(),
                    entity.getRequiresApproval(),
                    entity.getCreatedBy(), // Using createdBy as updatedBy for simplicity if updatedBy is null
                    entity.getCorrelationId(),
                    entity.getMappingId());
        } else {
            // Insert
            if (entity.getMappingId() == null) {
                entity.setMappingId("tmq_" + System.currentTimeMillis());
            }

            String insertSql = "INSERT INTO cm3int.template_master_query_mapping (" +
                    "MAPPING_ID, CONFIG_ID, MASTER_QUERY_ID, QUERY_NAME, QUERY_SQL, " +
                    "QUERY_DESCRIPTION, QUERY_TYPE, MAX_EXECUTION_TIME_SECONDS, MAX_RESULT_ROWS, " +
                    "QUERY_PARAMETERS, PARAMETER_VALIDATION_RULES, STATUS, IS_READ_ONLY, " +
                    "SECURITY_CLASSIFICATION, REQUIRES_APPROVAL, CREATED_BY, CREATED_DATE, CORRELATION_ID) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE, ?)";

            jdbcTemplate.update(insertSql,
                    entity.getMappingId(), entity.getConfigId(), entity.getMasterQueryId(), entity.getQueryName(),
                    entity.getQuerySql(),
                    entity.getQueryDescription(), entity.getQueryType(), entity.getMaxExecutionTimeSeconds(),
                    entity.getMaxResultRows(),
                    entity.getQueryParameters(), entity.getParameterValidationRules(), entity.getStatus(),
                    entity.getIsReadOnly(),
                    entity.getSecurityClassification(), entity.getRequiresApproval(), entity.getCreatedBy(),
                    entity.getCorrelationId());
        }

        return entity;
    }

    public Optional<TemplateMasterQueryMappingEntity> findByConfigId(String configId) {
        try {
            String sql = "SELECT * FROM cm3int.template_master_query_mapping WHERE CONFIG_ID = ?";
            List<TemplateMasterQueryMappingEntity> results = jdbcTemplate.query(sql, rowMapper, configId);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            log.error("Error finding query mapping by configId: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
