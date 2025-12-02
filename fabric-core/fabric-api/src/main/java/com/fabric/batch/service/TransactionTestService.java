package com.fabric.batch.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * Transaction Test Service for Local Development
 * 
 * This service provides methods to test that Spring's transaction management
 * is working properly with Oracle database. It can be used to verify that
 * the @Transactional annotation is functioning correctly.
 * 
 * Usage:
 * - Call testTransactionCommit() to verify successful transaction commit
 * - Call testTransactionRollback() to verify rollback functionality
 * - Check logs for transaction debugging information
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-08-03
 */
@Slf4j
@Service
public class TransactionTestService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Test transaction commit functionality.
     * 
     * This method performs a simple database operation within a transaction
     * to verify that transaction management is working correctly.
     * 
     * @return Success message if transaction works
     */
    @Transactional
    public String testTransactionCommit() {
        log.info("🧪 Testing transaction commit functionality");
        
        try {
            // Simple query to verify database connectivity and transaction context
            String result = jdbcTemplate.queryForObject("SELECT 'Transaction Test: ' || SYSDATE FROM DUAL", String.class);
            
            log.info("✅ Transaction commit test successful: {}", result);
            return "Transaction management is working correctly: " + result;
            
        } catch (Exception e) {
            log.error("❌ Transaction commit test failed: {}", e.getMessage(), e);
            throw new RuntimeException("Transaction test failed", e);
        }
    }

    /**
     * Test transaction rollback functionality.
     * 
     * This method intentionally throws an exception to test that
     * transactions are properly rolled back on failure.
     * 
     * @param shouldFail If true, will throw exception to test rollback
     * @return Success message if transaction works (when shouldFail is false)
     */
    @Transactional
    public String testTransactionRollback(boolean shouldFail) {
        log.info("🧪 Testing transaction rollback functionality (shouldFail: {})", shouldFail);
        
        try {
            // Perform some database operation
            String result = jdbcTemplate.queryForObject("SELECT 'Rollback Test: ' || SYSDATE FROM DUAL", String.class);
            log.info("📊 Database operation completed: {}", result);
            
            if (shouldFail) {
                log.info("🔄 Intentionally throwing exception to test rollback");
                throw new RuntimeException("Intentional exception to test transaction rollback");
            }
            
            log.info("✅ Transaction rollback test completed successfully");
            return "Transaction rollback test completed: " + result;
            
        } catch (RuntimeException e) {
            log.error("❌ Transaction rollback test - exception thrown (this is expected): {}", e.getMessage());
            throw e; // Re-throw to trigger rollback
        } catch (Exception e) {
            log.error("❌ Unexpected error in transaction rollback test: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error in rollback test", e);
        }
    }

    /**
     * Test if we're currently within a transaction context.
     * 
     * @return Status message indicating transaction context
     */
    @Transactional
    public String checkTransactionContext() {
        log.info("🔍 Checking transaction context");
        
        try {
            // Use Oracle-specific query to check transaction status
            String txnStatus = jdbcTemplate.queryForObject(
                "SELECT CASE WHEN dbms_transaction.local_transaction_id IS NOT NULL THEN 'ACTIVE' ELSE 'NONE' END FROM DUAL", 
                String.class
            );
            
            log.info("📋 Transaction status: {}", txnStatus);
            return "Transaction context check - Status: " + txnStatus;
            
        } catch (Exception e) {
            log.error("❌ Failed to check transaction context: {}", e.getMessage(), e);
            return "Failed to check transaction context: " + e.getMessage();
        }
    }
}