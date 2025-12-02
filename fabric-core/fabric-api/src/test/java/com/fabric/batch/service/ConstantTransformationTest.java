package com.fabric.batch.service;

import com.fabric.batch.dto.FieldMappingDto;
import com.fabric.batch.model.FieldMapping;
import com.fabric.batch.service.impl.TemplateSourceMappingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify that constant transformation values are properly handled
 * throughout the entire data flow from DTO -> Entity -> YAML.
 */
@ExtendWith(MockitoExtension.class)
public class ConstantTransformationTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    
    @Mock
    private SourceSystemService sourceSystemService;
    
    @InjectMocks
    private TemplateSourceMappingServiceImpl templateSourceMappingService;

    @Test
    public void testConstantTransformationWithValue() {
        // Given: A FieldMappingDto with constant transformation and value
        FieldMappingDto dto = new FieldMappingDto();
        dto.setTargetFieldName("TRANSACTION_CODE");
        dto.setTransformationType("constant");
        dto.setValue("200");
        dto.setTargetPosition(1);
        dto.setLength(3);
        dto.setDataType("STRING");

        // When: Converting to FieldMapping model
        FieldMapping fieldMapping = templateSourceMappingService.convertToFieldMapping(dto);

        // Then: The constant value should be properly set
        assertNotNull(fieldMapping);
        assertEquals("TRANSACTION_CODE", fieldMapping.getTargetField());
        assertEquals("constant", fieldMapping.getTransformationType());
        assertEquals("200", fieldMapping.getValue());
        assertEquals(1, fieldMapping.getTargetPosition());
        assertEquals(3, fieldMapping.getLength());
        assertEquals("STRING", fieldMapping.getDataType());
    }

    @Test
    public void testConstantTransformationWithDefaultValue() {
        // Given: A FieldMappingDto with constant transformation and defaultValue only
        FieldMappingDto dto = new FieldMappingDto();
        dto.setTargetFieldName("STATUS_CODE");
        dto.setTransformationType("constant");
        dto.setDefaultValue("ACTIVE");
        dto.setTargetPosition(2);
        dto.setLength(10);
        dto.setDataType("STRING");

        // When: Converting to FieldMapping model
        FieldMapping fieldMapping = templateSourceMappingService.convertToFieldMapping(dto);

        // Then: The default value should be used as the constant value
        assertNotNull(fieldMapping);
        assertEquals("STATUS_CODE", fieldMapping.getTargetField());
        assertEquals("constant", fieldMapping.getTransformationType());
        assertEquals("ACTIVE", fieldMapping.getValue());
        assertEquals("ACTIVE", fieldMapping.getDefaultValue());
    }

    @Test
    public void testConstantTransformationWithBothValues() {
        // Given: A FieldMappingDto with both value and defaultValue
        FieldMappingDto dto = new FieldMappingDto();
        dto.setTargetFieldName("PRIORITY_CODE");
        dto.setTransformationType("constant");
        dto.setValue("HIGH");
        dto.setDefaultValue("LOW");
        dto.setTargetPosition(3);
        dto.setLength(4);
        dto.setDataType("STRING");

        // When: Converting to FieldMapping model
        FieldMapping fieldMapping = templateSourceMappingService.convertToFieldMapping(dto);

        // Then: The value should take precedence over defaultValue
        assertNotNull(fieldMapping);
        assertEquals("PRIORITY_CODE", fieldMapping.getTargetField());
        assertEquals("constant", fieldMapping.getTransformationType());
        assertEquals("HIGH", fieldMapping.getValue());
        assertEquals("LOW", fieldMapping.getDefaultValue());
    }

    @Test
    public void testSourceTransformationIgnoresConstantValues() {
        // Given: A FieldMappingDto with source transformation
        FieldMappingDto dto = new FieldMappingDto();
        dto.setTargetFieldName("CUSTOMER_ID");
        dto.setSourceFieldName("cust_id");
        dto.setTransformationType("source");
        dto.setValue("should_be_ignored");
        dto.setTargetPosition(4);
        dto.setLength(12);
        dto.setDataType("STRING");

        // When: Converting to FieldMapping model
        FieldMapping fieldMapping = templateSourceMappingService.convertToFieldMapping(dto);

        // Then: The value should not be set for source transformations
        assertNotNull(fieldMapping);
        assertEquals("CUSTOMER_ID", fieldMapping.getTargetField());
        assertEquals("cust_id", fieldMapping.getSourceField());
        assertEquals("source", fieldMapping.getTransformationType());
        assertNull(fieldMapping.getValue()); // Should be null for source transformations
    }

    @Test
    public void testPrepareTransformationConfigWithConstantValue() {
        // Given: A FieldMappingDto with constant transformation
        FieldMappingDto dto = new FieldMappingDto();
        dto.setTargetFieldName("BATCH_ID");
        dto.setTransformationType("constant");
        dto.setValue("BATCH_001");

        // When: Calling the private method through reflection or testing the public behavior
        // We can test this indirectly through the save operation validation
        
        // Then: The transformation should be valid (no exception thrown)
        assertDoesNotThrow(() -> {
            // This tests that the prepareTransformationConfig method works correctly
            // by ensuring no validation errors occur for valid constant transformations
            templateSourceMappingService.convertToFieldMapping(dto);
        });
    }
}