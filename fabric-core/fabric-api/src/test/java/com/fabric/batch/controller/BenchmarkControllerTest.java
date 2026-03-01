package com.fabric.batch.controller;

import com.fabric.batch.benchmark.BenchmarkReportService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BenchmarkControllerTest {

    @Test
    void latestReportEndpointReturnsOk() {
        var reportService = mock(BenchmarkReportService.class);
        var controller = new BenchmarkController(reportService);

        ResponseEntity<String> response = controller.getLatestReport();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Benchmark endpoint ready");
    }
}
