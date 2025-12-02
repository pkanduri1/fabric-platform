package com.fabric.batch.test;

import com.fabric.batch.mapping.YamlMappingService;
import com.fabric.batch.model.FieldMapping;
import com.fabric.batch.model.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Direct integration test for Phase 2 transformations
 * Tests actual business scenarios: risk classification, default values, calculations
 */
public class TestPhase2Transformations {

    private final YamlMappingService service = new YamlMappingService();

    @Test
    @DisplayName("Scenario 1: Risk Classification - HIGH_RISK (DEFAULT status)")
    void testRiskClassification_HighRisk() {
        Map<String, Object> row = new HashMap<>();
        row.put("status", "DEFAULT");
        row.put("balance", "500.00");
        row.put("days_overdue", "90");
        row.put("credit_score", "500");

        FieldMapping mapping = createRiskCategoryMapping();
        String result = service.transformField(row, mapping);

        assertEquals("HIGH_RISK", result,
            "DEFAULT status should result in HIGH_RISK");
    }

    @Test
    @DisplayName("Scenario 1: Risk Classification - MEDIUM_RISK (DELINQUENT, 45 days)")
    void testRiskClassification_MediumRisk() {
        Map<String, Object> row = new HashMap<>();
        row.put("status", "DELINQUENT");
        row.put("balance", "1000.00");
        row.put("days_overdue", "45");
        row.put("credit_score", "650");

        FieldMapping mapping = createRiskCategoryMapping();
        String result = service.transformField(row, mapping);

        assertEquals("MEDIUM_RISK", result,
            "DELINQUENT with 45 days overdue should result in MEDIUM_RISK");
    }

    @Test
    @DisplayName("Scenario 1: Risk Classification - LOW_RISK (ACTIVE, high balance)")
    void testRiskClassification_LowRisk() {
        Map<String, Object> row = new HashMap<>();
        row.put("status", "ACTIVE");
        row.put("balance", "5000.00");
        row.put("days_overdue", "0");
        row.put("credit_score", "750");

        FieldMapping mapping = createRiskCategoryMapping();
        String result = service.transformField(row, mapping);

        assertEquals("LOW_RISK", result,
            "ACTIVE status with balance >= 1000 should result in LOW_RISK");
    }

    @Test
    @DisplayName("Scenario 2: Default Values - NULL balance uses default")
    void testDefaultValue_NullBalance() {
        Map<String, Object> row = new HashMap<>();
        row.put("balance", null);

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("direct");
        mapping.setSourceField("balance");
        mapping.setDefaultValue("0.00");
        mapping.setLength(15);

        String result = service.transformField(row, mapping);

        assertEquals("0.00", result,
            "NULL balance should use default value 0.00");
    }

    @Test
    @DisplayName("Scenario 3: AVG calculation - Three balances")
    void testAverageBalance() {
        Map<String, Object> row = new HashMap<>();
        row.put("checking_balance", "1000.00");
        row.put("savings_balance", "2000.00");
        row.put("investment_balance", "3000.00");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("composite");
        mapping.setTransform("AVG");

        List<Map<String, String>> sources = new ArrayList<>();
        sources.add(Map.of("sourceField", "checking_balance"));
        sources.add(Map.of("sourceField", "savings_balance"));
        sources.add(Map.of("sourceField", "investment_balance"));
        mapping.setSources(sources);
        mapping.setLength(15);

        String result = service.transformField(row, mapping);

        double avg = Double.parseDouble(result);
        assertEquals(2000.0, avg, 0.01,
            "Average of 1000, 2000, 3000 should be 2000");
    }

    @Test
    @DisplayName("Scenario 4: UPPER transformation - lowercase name")
    void testUpperTransformation() {
        Map<String, Object> row = new HashMap<>();
        row.put("customer_name", "john smith");

        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("composite");
        mapping.setTransform("UPPER");

        List<Map<String, String>> sources = new ArrayList<>();
        sources.add(Map.of("sourceField", "customer_name"));
        mapping.setSources(sources);
        mapping.setLength(30);
        mapping.setDefaultValue("UNKNOWN");

        String result = service.transformField(row, mapping);

        assertEquals("JOHN SMITH", result,
            "Lowercase name should be converted to uppercase");
    }

    // Helper method to create risk category mapping
    private FieldMapping createRiskCategoryMapping() {
        FieldMapping mapping = new FieldMapping();
        mapping.setTransformationType("conditional");
        mapping.setDefaultValue("UNKNOWN");
        mapping.setLength(20);

        List<Condition> conditions = new ArrayList<>();

        Condition c1 = new Condition();
        c1.setIfExpr("status IN ('DEFAULT', 'CHARGED_OFF')");
        c1.setThen("HIGH_RISK");
        conditions.add(c1);

        Condition c2 = new Condition();
        c2.setIfExpr("status == 'DELINQUENT' && days_overdue >= 30");
        c2.setThen("MEDIUM_RISK");
        conditions.add(c2);

        Condition c3 = new Condition();
        c3.setIfExpr("status == 'ACTIVE' && balance >= 1000");
        c3.setThen("LOW_RISK");
        conditions.add(c3);

        Condition c4 = new Condition();
        c4.setIfExpr("credit_score < 600");
        c4.setThen("REVIEW_REQUIRED");
        c4.setElseExpr("STANDARD");
        conditions.add(c4);

        mapping.setConditions(conditions);
        return mapping;
    }
}
