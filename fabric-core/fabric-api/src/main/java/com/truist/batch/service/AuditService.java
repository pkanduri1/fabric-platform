package com.truist.batch.service;

import com.truist.batch.dao.ConfigurationAuditDao;
import com.truist.batch.model.ConfigurationAudit;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {
    
    private final ConfigurationAuditDao auditDao;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public void logCreate(String configId, Object newValue, String changedBy, String reason) {
        ConfigurationAudit audit = new ConfigurationAudit();
        audit.setConfigId(configId);
        audit.setAction("CREATE");
        audit.setNewValue(toJson(newValue));
        audit.setChangedBy(changedBy);
        audit.setReason(reason);
        auditDao.save(audit);
    }
    
    @Transactional
    public void logUpdate(String configId, Object oldValue, Object newValue, String changedBy, String reason) {
        ConfigurationAudit audit = new ConfigurationAudit();
        audit.setConfigId(configId);
        audit.setAction("UPDATE");
        audit.setOldValue(toJson(oldValue));
        audit.setNewValue(toJson(newValue));
        audit.setChangedBy(changedBy);
        audit.setReason(reason);
        auditDao.save(audit);
    }
    
    @Transactional
    public void logDelete(String configId, Object oldValue, String changedBy, String reason) {
        ConfigurationAudit audit = new ConfigurationAudit();
        audit.setConfigId(configId);
        audit.setAction("DELETE");
        audit.setOldValue(toJson(oldValue));
        audit.setChangedBy(changedBy);
        audit.setReason(reason);
        auditDao.save(audit);
    }
    
    /**
     * Log security event with String parameters
     */
    public void logSecurityEvent(String eventType, String description, String changedBy, java.util.Map<String, String> additionalData) {
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
    }
    
    /**
     * Log security event with Object parameters (renamed to avoid erasure conflict)
     */
    public void logSecurityEventWithObjects(String eventType, String description, String changedBy, java.util.Map<String, ? extends Object> additionalData) {
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
    }
    
    private ConfigurationAudit createAdditionalDataAudit(String eventType, java.util.Map<String, String> data, String changedBy) {
        ConfigurationAudit audit = new ConfigurationAudit();
        audit.setConfigId("SECURITY_EVENT_DATA");
        audit.setAction(eventType + "_DATA");
        audit.setNewValue(toJson(data));
        audit.setChangedBy(changedBy);
        audit.setReason("Additional data for security event: " + eventType);
        return audit;
    }
    
    private ConfigurationAudit createAdditionalDataAuditFromObjects(String eventType, java.util.Map<String, ? extends Object> data, String changedBy) {
        ConfigurationAudit audit = new ConfigurationAudit();
        audit.setConfigId("SECURITY_EVENT_DATA");
        audit.setAction(eventType + "_DATA");
        audit.setNewValue(toJson(data));
        audit.setChangedBy(changedBy);
        audit.setReason("Additional data for security event: " + eventType);
        return audit;
    }
    
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj != null ? obj.toString() : null;
        }
    }
}
