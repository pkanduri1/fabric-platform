package com.fabric.batch.controller;

import com.fabric.batch.dto.monitoring.MonitoringDashboardResponse;
import com.fabric.batch.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "https://localhost:3000"})
@Tag(name = "Monitoring Dashboard", description = "Real-time job monitoring and system health")
public class MonitoringController {

    private final MonitoringService monitoringService;

    @Operation(summary = "Get monitoring dashboard data")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard data retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('OPERATIONS_MANAGER', 'ADMIN', 'JOB_VIEWER', 'JOB_EXECUTOR')")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        MonitoringDashboardResponse data = monitoringService.getDashboardData();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", data.getLastUpdate(),
                "correlationId", data.getCorrelationId()
        ));
    }
}
