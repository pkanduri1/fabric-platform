package com.truist.batch.audit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify NoOpAuditTrailManager can be instantiated without JPA dependencies.
 */
@SpringBootTest(classes = {NoOpAuditTrailManager.class})
@ActiveProfiles("test")
public class AuditTrailManagerIntegrationTest {

    @Autowired(required = false)
    private NoOpAuditTrailManager auditTrailManager;

    @Test
    public void testAuditTrailManagerCanBeInstantiated() {
        // This test verifies that AuditTrailManager can be created without JPA repository dependencies
        assertThat(auditTrailManager).isNotNull();
    }

    @Test
    public void testAuditOperationsDoNotFail() {
        // Test that basic audit operations don't throw exceptions when no repository is available
        try {
            String correlationId = auditTrailManager.auditDataLoadStart(
                "test-config", "test-job", "test-file.dat", "TEST_SYSTEM", "test_table");
            
            assertThat(correlationId).isNotNull().startsWith("AUDIT-");
            
            // Complete the audit trail
            auditTrailManager.auditDataLoadComplete(correlationId, true, 100L, 100L, 0L, 5000L);
            
            // Test passes if no exceptions are thrown
        } catch (Exception e) {
            throw new AssertionError("Audit operations should not fail when repository is unavailable", e);
        }
    }
}