package com.truist.batch.controller;

import com.truist.batch.util.SchemaExtractor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Controller to extract database schema metadata
 */
@RestController
@RequestMapping("/api/schema")
public class SchemaController {

    private final SchemaExtractor schemaExtractor;

    public SchemaController(SchemaExtractor schemaExtractor) {
        this.schemaExtractor = schemaExtractor;
    }

    @GetMapping("/extract")
    public ResponseEntity<String> extractSchema() {
        try {
            String outputPath = "/tmp/schema_snapshot.txt";
            schemaExtractor.extractSchemaToFile(outputPath);
            String content = Files.readString(Paths.get(outputPath));
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
