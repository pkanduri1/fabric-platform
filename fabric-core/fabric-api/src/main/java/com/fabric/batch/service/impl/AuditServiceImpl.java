package com.fabric.batch.service.impl;

import com.fabric.batch.dao.ConfigurationAuditDao;
import com.fabric.batch.model.ConfigurationAudit;
import com.fabric.batch.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Implementation of AuditService for SOX-compliant audit logging.
 * Provides comprehensive audit trail functionality for all configuration changes,
 * user actions, and security events in the fabric platform.
 */
@Service
@Slf4j
@Transactional
public class AuditServiceImpl implements AuditService {
    
    @Autowired(required = false)
    private ConfigurationAuditDao auditDao;
    
    @Autowired(required = false) 
    private ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public void logCreate(String configId, Object newValue, String changedBy, String reason) {
        if (auditDao == null) {
            log.info("AuditService: CREATE - configId={}, changedBy={}, reason={}", configId, changedBy, reason);
            return;
        }
        
        try {
            ConfigurationAudit audit = new ConfigurationAudit();
            audit.setConfigId(configId);
            audit.setAction("CREATE");
            audit.setNewValue(toJson(newValue));
            audit.setChangedBy(changedBy);
            audit.setReason(reason);
            auditDao.save(audit);
            
            log.debug("Successfully logged CREATE audit for configId: {}", configId);
        } catch (Exception e) {
            log.error("Failed to log CREATE audit for configId: {}", configId, e);
            // Don't re-throw to avoid breaking business operations
        }
    }
    
    @Override
    @Transactional
    public void logUpdate(String configId, Object oldValue, Object newValue, String changedBy, String reason) {
        if (auditDao == null) {
            log.info("AuditService: UPDATE - configId={}, changedBy={}, reason={}", configId, changedBy, reason);
            return;
        }
        
        try {
            ConfigurationAudit audit = new ConfigurationAudit();
            audit.setConfigId(configId);
            audit.setAction("UPDATE");
            audit.setOldValue(toJson(oldValue));
            audit.setNewValue(toJson(newValue));
            audit.setChangedBy(changedBy);
            audit.setReason(reason);
            auditDao.save(audit);
            
            log.debug("Successfully logged UPDATE audit for configId: {}", configId);
        } catch (Exception e) {
            log.error("Failed to log UPDATE audit for configId: {}", configId, e);
            // Don't re-throw to avoid breaking business operations
        }
    }
    
    @Override
    @Transactional
    public void logDelete(String configId, Object oldValue, String changedBy, String reason) {
        if (auditDao == null) {
            log.info("AuditService: DELETE - configId={}, changedBy={}, reason={}", configId, changedBy, reason);
            return;
        }
        
        try {
            ConfigurationAudit audit = new ConfigurationAudit();
            audit.setConfigId(configId);
            audit.setAction("DELETE");
            audit.setOldValue(toJson(oldValue));
            audit.setChangedBy(changedBy);
            audit.setReason(reason);
            auditDao.save(audit);
            
            log.debug("Successfully logged DELETE audit for configId: {}", configId);
        } catch (Exception e) {
            log.error("Failed to log DELETE audit for configId: {}", configId, e);
            // Don't re-throw to avoid breaking business operations
        }
    }
    
    @Override
    public void logSecurityEvent(String eventType, String description, String changedBy, Map<String, String> additionalData) {
        if (auditDao == null) {
            log.info("AuditService: SECURITY_EVENT - eventType={}, changedBy={}, description={}", eventType, changedBy, description);
            return;
        }
        
        try {
            ConfigurationAudit audit = new ConfigurationAudit();
            audit.setConfigId("SECURITY_EVENT");
            audit.setAction(eventType);
            audit.setNewValue(description);
            audit.setChangedBy(changedBy);
            audit.setReason("Security Event: " + eventType);
            auditDao.save(audit);
            
            // Log additional data if present
            if (additionalData != null && !additionalData.isEmpty()) {
                auditDao.save(createAdditionalDataAudit(eventType, additionalData, changedBy));
            }
            
            log.debug("Successfully logged SECURITY_EVENT audit for eventType: {}", eventType);
        } catch (Exception e) {
            log.error("Failed to log SECURITY_EVENT audit for eventType: {}", eventType, e);
            // Don't re-throw to avoid breaking business operations
        }
    }
    
    @Override
    public void logSecurityEventWithObjects(String eventType, String description, String changedBy, Map<String, ? extends Object> additionalData) {
        if (auditDao == null) {
            log.info("AuditService: SECURITY_EVENT_OBJECTS - eventType={}, changedBy={}, description={}", eventType, changedBy, description);
            return;
        }
        
        try {
            ConfigurationAudit audit = new ConfigurationAudit();
            audit.setConfigId("SECURITY_EVENT");
            audit.setAction(eventType);
            audit.setNewValue(description);
            audit.setChangedBy(changedBy);
            audit.setReason("Security Event: " + eventType);
            auditDao.save(audit);
            
            // Log additional data if present
            if (additionalData != null && !additionalData.isEmpty()) {
                auditDao.save(createAdditionalDataAuditFromObjects(eventType, additionalData, changedBy));
            }
            
            log.debug("Successfully logged SECURITY_EVENT_OBJECTS audit for eventType: {}", eventType);
        } catch (Exception e) {
            log.error("Failed to log SECURITY_EVENT_OBJECTS audit for eventType: {}", eventType, e);
            // Don't re-throw to avoid breaking business operations
        }
    }
    
    /**
     * Helper method to create additional data audit entry for String data
     */
    private ConfigurationAudit createAdditionalDataAudit(String eventType, Map<String, String> data, String changedBy) {
        ConfigurationAudit audit = new ConfigurationAudit();
        audit.setConfigId("SECURITY_EVENT_DATA");
        audit.setAction(eventType + "_DATA");
        audit.setNewValue(toJson(data));
        audit.setChangedBy(changedBy);
        audit.setReason("Additional data for security event: " + eventType);
        return audit;
    }
    
    /**
     * Helper method to create additional data audit entry for Object data
     */
    private ConfigurationAudit createAdditionalDataAuditFromObjects(String eventType, Map<String, ? extends Object> data, String changedBy) {
        ConfigurationAudit audit = new ConfigurationAudit();
        audit.setConfigId("SECURITY_EVENT_DATA");
        audit.setAction(eventType + "_DATA");
        audit.setNewValue(toJson(data));
        audit.setChangedBy(changedBy);
        audit.setReason("Additional data for security event: " + eventType);
        return audit;
    }
    
    /**
     * Helper method to convert objects to JSON strings for audit storage
     */
    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        
        if (objectMapper == null) {
            return obj.toString();
        }
        
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Failed to serialize object to JSON: {}, falling back to toString()", e.getMessage());
            return obj.toString();
        }
    }
}