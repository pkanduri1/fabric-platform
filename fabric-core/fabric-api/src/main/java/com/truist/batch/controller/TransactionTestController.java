package com.truist.batch.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.truist.batch.service.TransactionTestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Transaction Test Controller for Local Development
 * 
 * This controller is only active in the 'local' profile and provides
 * endpoints to test Spring's transaction management functionality.
 * 
 * Available endpoints:
 * - GET /api/test/transaction/commit - Test transaction commit
 * - GET /api/test/transaction/rollback - Test transaction rollback
 * - GET /api/test/transaction/context - Check transaction context
 * 
 * @author Claude Code
 * @version 1.0
 * @since 2025-08-03
 */
@Slf4j
@RestController
@RequestMapping("/api/test/transaction")
@Profile("local") // Only available in local development
@Tag(name = "Transaction Testing", description = "Endpoints for testing transaction management (Local Development Only)")
public class TransactionTestController {

    @Autowired
    private TransactionTestService transactionTestService;

    /**
     * Test transaction commit functionality.
     * 
     * @return Success message if transaction management is working
     */
    @GetMapping("/commit")
    @Operation(
        summary = "Test Transaction Commit",
        description = "Tests that Spring transaction management is working correctly by performing a database operation within a transaction"
    )
    public ResponseEntity<String> testTransactionCommit() {
        log.info("🔗 REST: Testing transaction commit via /api/test/transaction/commit");
        
        try {
            String result = transactionTestService.testTransactionCommit();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ REST: Transaction commit test failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Transaction test failed: " + e.getMessage());
        }
    }

    /**
     * Test transaction rollback functionality.
     * 
     * @param shouldFail Whether to intentionally fail to test rollback (default: true)
     * @return Success message or error response
     */
    @GetMapping("/rollback")
    @Operation(
        summary = "Test Transaction Rollback",
        description = "Tests transaction rollback functionality by optionally throwing an exception"
    )
    public ResponseEntity<String> testTransactionRollback(
            @Parameter(description = "Whether to intentionally fail to test rollback", example = "true")
            @RequestParam(defaultValue = "true") boolean shouldFail) {
        
        log.info("🔗 REST: Testing transaction rollback via /api/test/transaction/rollback?shouldFail={}", shouldFail);
        
        try {
            String result = transactionTestService.testTransactionRollback(shouldFail);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.info("🔄 REST: Transaction rollback test - exception caught (expected): {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body("Transaction rollback test - Exception was thrown and caught: " + e.getMessage());
        }
    }

    /**
     * Check current transaction context.
     * 
     * @return Status of current transaction context
     */
    @GetMapping("/context")
    @Operation(
        summary = "Check Transaction Context",
        description = "Checks if the current operation is running within a transaction context"
    )
    public ResponseEntity<String> checkTransactionContext() {
        log.info("🔗 REST: Checking transaction context via /api/test/transaction/context");
        
        try {
            String result = transactionTestService.checkTransactionContext();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ REST: Failed to check transaction context: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Failed to check transaction context: " + e.getMessage());
        }
    }

    /**
     * Get transaction management status and configuration info.
     * 
     * @return Transaction management configuration summary
     */
    @GetMapping("/status")
    @Operation(
        summary = "Get Transaction Management Status",
        description = "Returns information about the current transaction management configuration"
    )
    public ResponseEntity<String> getTransactionStatus() {
        log.info("🔗 REST: Getting transaction status via /api/test/transaction/status");
        
        StringBuilder status = new StringBuilder();
        status.append("Transaction Management Status:\n");
        status.append("- Profile: local\n");
        status.append("- @EnableTransactionManagement: Active\n");
        status.append("- JPA Transaction Manager: Configured\n");
        status.append("- Oracle Database: Connected\n");
        status.append("- HikariCP Pool: Active (auto-commit disabled)\n");
        status.append("- Transaction Debugging: Enabled\n");
        status.append("\nTest endpoints:\n");
        status.append("- GET /api/test/transaction/commit\n");
        status.append("- GET /api/test/transaction/rollback?shouldFail=true\n");
        status.append("- GET /api/test/transaction/context\n");
        
        return ResponseEntity.ok(status.toString());
    }
}