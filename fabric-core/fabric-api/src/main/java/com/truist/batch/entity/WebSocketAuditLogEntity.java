package com.truist.batch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * US008: WebSocket Audit Log POJO for SOX Compliance
 * 
 * Simplified POJO for WebSocket audit events to eliminate JPA dependencies.
 * Full implementation will be provided when WebSocket audit functionality is required.
 * 
 * Converted from JPA Entity to simple POJO to eliminate JPA dependencies
 * 
 * TODO: Implement full WebSocket audit log fields when needed
 * 
 * @author Senior Full Stack Developer
 * @version 1.0
 * @since US008 - Real-Time Job Monitoring Dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketAuditLogEntity {

    // ============================================================================
    // PRIMARY KEY
    // ============================================================================
    
    private Long auditId;

    // ============================================================================
    // EVENT IDENTIFICATION
    // ============================================================================
    
    private String eventType;
    private String eventSubtype;
    private String eventDescription;

    // ============================================================================
    // USER AND SESSION CONTEXT
    // ============================================================================
    
    private String sessionId;
    private String userId;
    private String clientIp;
    private String userAgent;
    private String originUrl;

    // ============================================================================
    // EVENT TIMING
    // ============================================================================
    
    private Instant eventTimestamp;

    // ============================================================================
    // SECURITY AND COMPLIANCE FIELDS (Simplified)
    // ============================================================================
    
    private String securityEventFlag;
    private String complianceEventFlag;
    private String eventSeverity;
    private String riskLevel;
    private String businessFunction;

    // ============================================================================
    // AUDIT TRACKING FIELDS (Simplified)
    // ============================================================================
    
    private String correlationId;
    private String auditHash;
    private String previousAuditHash;
    private String digitalSignature;
    private String securityClassification;

    // ============================================================================
    // TECHNICAL METADATA (Simplified)
    // ============================================================================
    
    private String sourceSystem;
    private String threadId;
    private String serverHostname;
    private Instant createdDate;
    private String eventData;
    private String createdBy;

    // TODO: Add remaining fields from original entity when full implementation is needed
    // This simplified version allows the application to start without JPA dependencies
}