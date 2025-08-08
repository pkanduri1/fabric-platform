package com.truist.batch.dao;

import com.truist.batch.model.ConfigurationAudit;
import java.time.LocalDateTime;
import java.util.List;

public interface ConfigurationAuditDao {
    
    ConfigurationAudit save(ConfigurationAudit audit);
    
    List<ConfigurationAudit> findByConfigIdOrderByChangeDateDesc(String configId);
    
    List<ConfigurationAudit> findByChangedByOrderByChangeDateDesc(String changedBy);
    
    List<ConfigurationAudit> findRecentChanges(LocalDateTime startDate);
    
    List<ConfigurationAudit> findByConfigIdAndAction(String configId, String action);
}