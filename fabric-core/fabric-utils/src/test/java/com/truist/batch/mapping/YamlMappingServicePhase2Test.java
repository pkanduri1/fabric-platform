package com.truist.batch.mapping;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.truist.batch.model.Condition;
import com.truist.batch.model.FieldMapping;

/**
 * Unit tests for Phase 2 enhancements to YamlMappingService
 * Tests new composite transformations (AVG, MIN, MAX, UPPER, LOWER, TRIM)
 * Tests new conditional operators (IN, BETWEEN, LIKE)
 */
@DisplayName("YamlMappingService - Phase 2 Transformations")
class YamlMappingServicePhase2Test {

    private YamlMappingService service;
    private Map<String, Object> testRow;

    @BeforeEach
    void setUp() {
        service = new YamlMappingService();
        testRow = new HashMap<>();
    }

    // ========================================
    // COMPOSITE TRANSFORMATIONS - MATHEMATICAL
    // ========================================

    @Test
    @DisplayName("AVG: Calculate average of multiple numeric fields")
    void testAverageOperation() {
        // Setup test data
        testRow.put("score1", "85");
        testRow.put("score2", "90");
        testRow.put("score3", "88");

        // Create field mapping
        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("composite");
        mapping.setTransform("avg");

        List<Map<String, String>> sources = new ArrayList<>();
        sources.add(Map.of("sourceField", "score1"));
        sources.add(Map.of("sourceField", "score2"));
        sources.add(Map.of("sourceField", "score3"));
        mapping.setSources(sources);

        mapping.setLength(0);

        // Execute transformation
        String result = service.transformField(testRow, mapping);

        // Verify result - average of 85, 90, 88 = 87.666...
        assertNotNull(result);
        double avg = Double.parseDouble(result);
        assertEquals(87.666, avg, 0.01, "Average should be approximately 87.67");
    }

    @Test
    @DisplayName("AVG: Handle empty or invalid values")
    void testAverageWithInvalidValues() {
        testRow.put("score1", "100");
        testRow.put("score2", "invalid");
        testRow.put("score3", "80");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("composite");
        mapping.setTransform("average");

        List<Map<String, String>> sources = new ArrayList<>();
        sources.add(Map.of("sourceField", "score1"));
        sources.add(Map.of("sourceField", "score2"));
        sources.add(Map.of("sourceField", "score3"));
        mapping.setSources(sources);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        // Invalid values treated as 0, so (100 + 0 + 80) / 3 = 60
        assertNotNull(result);
        double avg = Double.parseDouble(result);
        assertEquals(60.0, avg, 0.01);
    }

    @Test
    @DisplayName("MIN: Find minimum value among fields")
    void testMinimumOperation() {
        testRow.put("payment1", "500");
        testRow.put("payment2", "1400");
        testRow.put("payment3", "5000");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("composite");
        mapping.setTransform("min");

        List<Map<String, String>> sources = new ArrayList<>();
        sources.add(Map.of("sourceField", "payment1"));
        sources.add(Map.of("sourceField", "payment2"));
        sources.add(Map.of("sourceField", "payment3"));
        mapping.setSources(sources);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertNotNull(result);
        assertEquals("500.0", result);
    }

    @Test
    @DisplayName("MAX: Find maximum value among fields")
    void testMaximumOperation() {
        testRow.put("amount1", "100");
        testRow.put("amount2", "500");
        testRow.put("amount3", "250");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("composite");
        mapping.setTransform("max");

        List<Map<String, String>> sources = new ArrayList<>();
        sources.add(Map.of("sourceField", "amount1"));
        sources.add(Map.of("sourceField", "amount2"));
        sources.add(Map.of("sourceField", "amount3"));
        mapping.setSources(sources);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertNotNull(result);
        assertEquals("500.0", result);
    }

    // ========================================
    // COMPOSITE TRANSFORMATIONS - STRING
    // ========================================

    @Test
    @DisplayName("UPPER: Convert text to uppercase")
    void testUppercaseOperation() {
        testRow.put("name", "john doe");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("composite");
        mapping.setTransform("upper");

        List<Map<String, String>> sources = new ArrayList<>();
        sources.add(Map.of("sourceField", "name"));
        mapping.setSources(sources);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("JOHN DOE", result);
    }

    @Test
    @DisplayName("LOWER: Convert text to lowercase")
    void testLowercaseOperation() {
        testRow.put("status", "ACTIVE");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("composite");
        mapping.setTransform("lower");

        List<Map<String, String>> sources = new ArrayList<>();
        sources.add(Map.of("sourceField", "status"));
        mapping.setSources(sources);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("active", result);
    }

    @Test
    @DisplayName("TRIM: Remove leading and trailing whitespace")
    void testTrimOperation() {
        testRow.put("value", "  data with spaces  ");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("composite");
        mapping.setTransform("trim");

        List<Map<String, String>> sources = new ArrayList<>();
        sources.add(Map.of("sourceField", "value"));
        mapping.setSources(sources);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("data with spaces", result);
    }

    // ========================================
    // CONDITIONAL TRANSFORMATIONS - IN OPERATOR
    // ========================================

    @Test
    @DisplayName("IN: Match value in list - positive case")
    void testInOperatorMatch() {
        testRow.put("status", "ACTIVE");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("conditional");

        List<Condition> conditions = new ArrayList<>();
        Condition condition = new Condition();
        condition.setIfExpr("status IN ('ACTIVE', 'PENDING', 'APPROVED')");
        condition.setThen("VALID");
        conditions.add(condition);

        Condition elseCondition = new Condition();
        elseCondition.setElseExpr("INVALID");
        conditions.add(elseCondition);

        mapping.setConditions(conditions);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("VALID", result);
    }

    @Test
    @DisplayName("IN: No match value in list - negative case")
    void testInOperatorNoMatch() {
        testRow.put("status", "CANCELLED");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("conditional");

        List<Condition> conditions = new ArrayList<>();
        Condition condition = new Condition();
        condition.setIfExpr("status IN ('ACTIVE', 'PENDING', 'APPROVED')");
        condition.setThen("VALID");
        condition.setElseExpr("INVALID");  // Set else value in the same condition
        conditions.add(condition);

        mapping.setConditions(conditions);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("INVALID", result);
    }

    @Test
    @DisplayName("IN: Support double quotes in values")
    void testInOperatorWithDoubleQuotes() {
        testRow.put("code", "CHK");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("conditional");

        List<Condition> conditions = new ArrayList<>();
        Condition condition = new Condition();
        condition.setIfExpr("code IN (\"CHK\", \"SAV\", \"LON\")");
        condition.setThen("FOUND");
        conditions.add(condition);

        Condition elseCondition = new Condition();
        elseCondition.setElseExpr("NOT_FOUND");
        conditions.add(elseCondition);

        mapping.setConditions(conditions);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("FOUND", result);
    }

    // ========================================
    // CONDITIONAL TRANSFORMATIONS - BETWEEN OPERATOR
    // ========================================

    @Test
    @DisplayName("BETWEEN: Value within range")
    void testBetweenOperatorInRange() {
        testRow.put("amount", "500");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("conditional");

        List<Condition> conditions = new ArrayList<>();
        Condition condition = new Condition();
        condition.setIfExpr("amount BETWEEN 100 AND 1000");
        condition.setThen("MEDIUM");
        conditions.add(condition);

        Condition elseCondition = new Condition();
        elseCondition.setElseExpr("OUT_OF_RANGE");
        conditions.add(elseCondition);

        mapping.setConditions(conditions);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("MEDIUM", result);
    }

    @Test
    @DisplayName("BETWEEN: Value at lower boundary")
    void testBetweenOperatorLowerBoundary() {
        testRow.put("amount", "100");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("conditional");

        List<Condition> conditions = new ArrayList<>();
        Condition condition = new Condition();
        condition.setIfExpr("amount BETWEEN 100 AND 1000");
        condition.setThen("VALID");
        conditions.add(condition);

        Condition elseCondition = new Condition();
        elseCondition.setElseExpr("INVALID");
        conditions.add(elseCondition);

        mapping.setConditions(conditions);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("VALID", result, "BETWEEN should be inclusive of lower boundary");
    }

    @Test
    @DisplayName("BETWEEN: Value at upper boundary")
    void testBetweenOperatorUpperBoundary() {
        testRow.put("amount", "1000");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("conditional");

        List<Condition> conditions = new ArrayList<>();
        Condition condition = new Condition();
        condition.setIfExpr("amount BETWEEN 100 AND 1000");
        condition.setThen("VALID");
        conditions.add(condition);

        Condition elseCondition = new Condition();
        elseCondition.setElseExpr("INVALID");
        conditions.add(elseCondition);

        mapping.setConditions(conditions);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("VALID", result, "BETWEEN should be inclusive of upper boundary");
    }

    @Test
    @DisplayName("BETWEEN: Value outside range")
    void testBetweenOperatorOutOfRange() {
        testRow.put("amount", "50");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("conditional");

        List<Condition> conditions = new ArrayList<>();
        Condition condition = new Condition();
        condition.setIfExpr("amount BETWEEN 100 AND 1000");
        condition.setThen("IN_RANGE");
        condition.setElseExpr("OUT_OF_RANGE");  // Set else value in the same condition
        conditions.add(condition);

        mapping.setConditions(conditions);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("OUT_OF_RANGE", result);
    }

    // ========================================
    // CONDITIONAL TRANSFORMATIONS - LIKE OPERATOR
    // ========================================

    @Test
    @DisplayName("LIKE: Pattern match with % wildcard at end")
    void testLikeOperatorStartsWith() {
        testRow.put("account_code", "CHK1234");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("conditional");

        List<Condition> conditions = new ArrayList<>();
        Condition condition = new Condition();
        condition.setIfExpr("account_code LIKE 'CHK%'");
        condition.setThen("CHECKING");
        conditions.add(condition);

        Condition elseCondition = new Condition();
        elseCondition.setElseExpr("OTHER");
        conditions.add(elseCondition);

        mapping.setConditions(conditions);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("CHECKING", result);
    }

    @Test
    @DisplayName("LIKE: Pattern match with % wildcard at beginning")
    void testLikeOperatorEndsWith() {
        testRow.put("filename", "report_2025.pdf");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("conditional");

        List<Condition> conditions = new ArrayList<>();
        Condition condition = new Condition();
        condition.setIfExpr("filename LIKE '%.pdf'");
        condition.setThen("PDF_FILE");
        conditions.add(condition);

        Condition elseCondition = new Condition();
        elseCondition.setElseExpr("OTHER_FILE");
        conditions.add(elseCondition);

        mapping.setConditions(conditions);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("PDF_FILE", result);
    }

    @Test
    @DisplayName("LIKE: Pattern match with % wildcards on both sides")
    void testLikeOperatorContains() {
        testRow.put("description", "This is ABC company");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("conditional");

        List<Condition> conditions = new ArrayList<>();
        Condition condition = new Condition();
        condition.setIfExpr("description LIKE '%ABC%'");
        condition.setThen("FOUND");
        conditions.add(condition);

        Condition elseCondition = new Condition();
        elseCondition.setElseExpr("NOT_FOUND");
        conditions.add(elseCondition);

        mapping.setConditions(conditions);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("FOUND", result);
    }

    @Test
    @DisplayName("LIKE: Pattern match with _ single character wildcard")
    void testLikeOperatorSingleCharWildcard() {
        testRow.put("id", "A1C");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("conditional");

        List<Condition> conditions = new ArrayList<>();
        Condition condition = new Condition();
        condition.setIfExpr("id LIKE 'A_C'");
        condition.setThen("MATCH");
        conditions.add(condition);

        Condition elseCondition = new Condition();
        elseCondition.setElseExpr("NO_MATCH");
        conditions.add(elseCondition);

        mapping.setConditions(conditions);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("MATCH", result);
    }

    @Test
    @DisplayName("LIKE: No pattern match")
    void testLikeOperatorNoMatch() {
        testRow.put("account_code", "SAV5678");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("conditional");

        List<Condition> conditions = new ArrayList<>();
        Condition condition = new Condition();
        condition.setIfExpr("account_code LIKE 'CHK%'");
        condition.setThen("CHECKING");
        condition.setElseExpr("OTHER");  // Set else value in the same condition
        conditions.add(condition);

        mapping.setConditions(conditions);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("OTHER", result);
    }

    // ========================================
    // COMPLEX SCENARIOS
    // ========================================

    @Test
    @DisplayName("Complex: Multiple conditions with IN and BETWEEN")
    void testComplexConditionsWithMultipleOperators() {
        testRow.put("status", "ACTIVE");
        testRow.put("amount", "500");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("conditional");

        List<Condition> conditions = new ArrayList<>();

        // First condition: status IN and amount BETWEEN
        Condition condition1 = new Condition();
        condition1.setIfExpr("status IN ('ACTIVE', 'PENDING') && amount BETWEEN 100 AND 1000");
        condition1.setThen("VALID_TRANSACTION");
        conditions.add(condition1);

        Condition elseCondition = new Condition();
        elseCondition.setElseExpr("INVALID_TRANSACTION");
        conditions.add(elseCondition);

        mapping.setConditions(conditions);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("VALID_TRANSACTION", result);
    }

    @Test
    @DisplayName("Null Handling: Composite transformation with null values")
    void testCompositeWithNullValues() {
        testRow.put("value1", "100");
        testRow.put("value2", null);
        testRow.put("value3", "200");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("composite");
        mapping.setTransform("avg");

        List<Map<String, String>> sources = new ArrayList<>();
        sources.add(Map.of("sourceField", "value1"));
        sources.add(Map.of("sourceField", "value2"));
        sources.add(Map.of("sourceField", "value3"));
        mapping.setSources(sources);
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        // Null treated as 0, so (100 + 0 + 200) / 3 = 100
        assertNotNull(result);
        double avg = Double.parseDouble(result);
        assertEquals(100.0, avg, 0.01);
    }

    @Test
    @DisplayName("Edge Case: Empty sources list for composite")
    void testCompositeWithEmptySources() {
        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("composite");
        mapping.setTransform("avg");
        mapping.setSources(new ArrayList<>());
        mapping.setDefaultValue("0");
        mapping.setLength(0);

        String result = service.transformField(testRow, mapping);

        assertEquals("0", result, "Should return default value when sources are empty");
    }
}
