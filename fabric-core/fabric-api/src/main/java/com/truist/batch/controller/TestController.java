package com.truist.batch.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple test controller to verify request mapping is working
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of(
            "status", "success",
            "message", "Hello from Fabric API!",
            "timestamp", String.valueOf(System.currentTimeMillis())
        );
    }
}