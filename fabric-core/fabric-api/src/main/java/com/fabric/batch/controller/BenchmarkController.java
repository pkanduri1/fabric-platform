package com.fabric.batch.controller;

import com.fabric.batch.benchmark.BenchmarkReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/benchmark")
@RequiredArgsConstructor
public class BenchmarkController {

    private final BenchmarkReportService reportService;

    @GetMapping("/latest")
    public ResponseEntity<String> getLatestReport() {
        return ResponseEntity.ok("Benchmark endpoint ready. Use POST /run to execute.");
    }
}
