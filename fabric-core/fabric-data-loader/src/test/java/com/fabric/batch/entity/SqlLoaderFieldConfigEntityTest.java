package com.fabric.batch.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for SqlLoaderFieldConfigEntity.
 * Tests field configuration validation, control file generation, and business rules.
 */
@DisplayName("SQL*Loader Field Configuration Entity Tests")
class SqlLoaderFieldConfigEntityTest {
    
    private SqlLoaderFieldConfigEntity entity;
    
    @BeforeEach
    void setUp() {
        entity = SqlLoaderFieldConfigEntity.builder()
                .configId("TEST-CONFIG-001")
                .fieldName("CUSTOMER_ID")
                .columnName("CUSTOMER_ID")
                .dataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.NUMBER)
                .nullable("N")
                .fieldOrder(1)
                .createdBy("test-user")
                .build();
    }
    
    @Nested
    @DisplayName("Entity Construction and Defaults")
    class EntityConstructionTests {
        
        @Test
        @DisplayName("Should create entity with builder pattern")
        void shouldCreateEntityWithBuilder() {
            SqlLoaderFieldConfigEntity field = SqlLoaderFieldConfigEntity.builder()
                    .configId("TEST-001")
                    .fieldName("SSN")
                    .columnName("SSN_ENCRYPTED")
                    .dataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.CHAR)
                    .fieldOrder(1)
                    .createdBy("admin")
                    .build();
            
            assertNotNull(field);
            assertEquals("TEST-001", field.getConfigId());
            assertEquals("SSN", field.getFieldName());
            assertEquals("SSN_ENCRYPTED", field.getColumnName());
            assertEquals(SqlLoaderFieldConfigEntity.SqlLoaderDataType.CHAR, field.getDataType());
            assertEquals("admin", field.getCreatedBy());
        }
        
        @Test
        @DisplayName("Should set default values correctly")
        void shouldSetDefaultValues() {
            SqlLoaderFieldConfigEntity field = new SqlLoaderFieldConfigEntity();
            
            assertEquals("Y", field.getNullable());
            assertEquals("BLANKS", field.getNullIfCondition());
            assertEquals("Y", field.getTrimField());
            assertEquals(SqlLoaderFieldConfigEntity.CaseSensitive.PRESERVE, field.getCaseSensitive());
            assertEquals("N", field.getUniqueConstraint());
            assertEquals("N", field.getPrimaryKey());
            assertEquals("N", field.getEncrypted());
            assertEquals("N", field.getAuditField());
            assertEquals(Integer.valueOf(1), field.getVersion());
            assertEquals("Y", field.getEnabled());
        }
        
        @Test
        @DisplayName("Should handle JPA lifecycle callbacks")
        void shouldHandleJpaLifecycleCallbacks() {
            SqlLoaderFieldConfigEntity field = SqlLoaderFieldConfigEntity.builder()
                    .configId("TEST-001")
                    .fieldName("EMAIL")
                    .columnName("EMAIL")
                    .dataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.VARCHAR2)
                    .fieldOrder(1)
                    .createdBy("admin")
                    .build();
            
            // Simulate @PrePersist
            field.onCreate();
            
            assertNotNull(field.getCreatedDate());
            assertEquals(Integer.valueOf(1), field.getVersion());
        }
    }
    
    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {
        
        @Test
        @DisplayName("Should correctly check boolean flags")
        void shouldCheckBooleanFlags() {
            entity.setNullable("Y");
            entity.setEncrypted("Y");
            entity.setTrimField("Y");
            entity.setUniqueConstraint("Y");
            entity.setPrimaryKey("N");
            entity.setAuditField("Y");
            entity.setEnabled("Y");
            
            assertTrue(entity.isNullable());
            assertTrue(entity.isEncrypted());
            assertTrue(entity.isTrimEnabled());
            assertTrue(entity.hasUniqueConstraint());
            assertFalse(entity.isPrimaryKey());
            assertTrue(entity.isAuditField());
            assertTrue(entity.isEnabled());
        }
        
        @Test
        @DisplayName("Should check business rule presence")
        void shouldCheckBusinessRulePresence() {
            entity.setBusinessRuleClass("com.fabric.validation.EmailValidator");
            assertTrue(entity.hasBusinessRule());
            
            entity.setBusinessRuleClass("");
            assertFalse(entity.hasBusinessRule());
            
            entity.setBusinessRuleClass(null);
            assertFalse(entity.hasBusinessRule());
        }
        
        @Test
        @DisplayName("Should check validation rule presence")
        void shouldCheckValidationRulePresence() {
            entity.setValidationRule("LENGTH > 5");
            assertTrue(entity.hasValidationRule());
            
            entity.setValidationRule("");
            assertFalse(entity.hasValidationRule());
            
            entity.setValidationRule(null);
            assertFalse(entity.hasValidationRule());
        }
        
        @Test
        @DisplayName("Should check lookup reference")
        void shouldCheckLookupReference() {
            entity.setLookupTable("CUSTOMER_TYPES");
            entity.setLookupColumn("TYPE_CODE");
            assertTrue(entity.hasLookupReference());
            
            entity.setLookupTable("");
            assertFalse(entity.hasLookupReference());
            
            entity.setLookupColumn(null);
            assertFalse(entity.hasLookupReference());
        }
        
        @Test
        @DisplayName("Should check SQL expression presence")
        void shouldCheckSqlExpressionPresence() {
            entity.setSqlExpression("UPPER(TRIM(:CUSTOMER_NAME))");
            assertTrue(entity.hasSqlExpression());
            
            entity.setSqlExpression("  ");
            assertFalse(entity.hasSqlExpression());
            
            entity.setSqlExpression(null);
            assertFalse(entity.hasSqlExpression());
        }
        
        @Test
        @DisplayName("Should check fixed-width configuration")
        void shouldCheckFixedWidthConfiguration() {
            entity.setFieldPosition(1);
            entity.setFieldLength(10);
            assertTrue(entity.isFixedWidth());
            
            entity.setFieldPosition(null);
            assertFalse(entity.isFixedWidth());
            
            entity.setFieldPosition(1);
            entity.setFieldLength(0);
            assertFalse(entity.isFixedWidth());
        }
    }
    
    @Nested
    @DisplayName("Control File Generation Tests")
    class ControlFileGenerationTests {
        
        @Test
        @DisplayName("Should generate basic field specification")
        void shouldGenerateBasicFieldSpecification() {
            entity.setColumnName("CUSTOMER_ID");
            entity.setDataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.NUMBER);
            entity.setMaxLength(10);
            
            String spec = entity.generateControlFileFieldSpec();
            
            assertNotNull(spec);
            assertTrue(spec.contains("CUSTOMER_ID"));
            assertTrue(spec.contains("DECIMAL EXTERNAL"));
        }
        
        @Test
        @DisplayName("Should generate fixed-width field specification")
        void shouldGenerateFixedWidthFieldSpecification() {
            entity.setColumnName("CUSTOMER_NAME");
            entity.setFieldPosition(1);
            entity.setFieldLength(50);
            entity.setDataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.CHAR);
            
            String spec = entity.generateControlFileFieldSpec();
            
            assertTrue(spec.contains("POSITION(1:50)"));
            assertTrue(spec.contains("CHAR"));
        }
        
        @Test
        @DisplayName("Should generate field specification with trimming")
        void shouldGenerateFieldSpecificationWithTrimming() {
            entity.setColumnName("EMAIL");
            entity.setTrimField("Y");
            
            String spec = entity.generateControlFileFieldSpec();
            
            assertTrue(spec.contains("TRIM(:EMAIL)"));
        }
        
        @Test
        @DisplayName("Should generate field specification with case conversion")
        void shouldGenerateFieldSpecificationWithCaseConversion() {
            entity.setColumnName("STATE_CODE");
            entity.setCaseSensitive(SqlLoaderFieldConfigEntity.CaseSensitive.UPPER);
            
            String spec = entity.generateControlFileFieldSpec();
            
            assertTrue(spec.contains("UPPER(:STATE_CODE)"));
        }
        
        @Test
        @DisplayName("Should generate field specification with SQL expression")
        void shouldGenerateFieldSpecificationWithSqlExpression() {
            entity.setColumnName("FULL_NAME");
            entity.setSqlExpression("UPPER(TRIM(:FULL_NAME))");
            
            String spec = entity.generateControlFileFieldSpec();
            
            assertTrue(spec.contains("UPPER(TRIM(:FULL_NAME))"));
        }
        
        @Test
        @DisplayName("Should generate field specification with encryption")
        void shouldGenerateFieldSpecificationWithEncryption() {
            entity.setColumnName("SSN");
            entity.setEncrypted("Y");
            entity.setEncryptionFunction("ENCRYPT_SSN");
            
            String spec = entity.generateControlFileFieldSpec();
            
            assertTrue(spec.contains("ENCRYPT_SSN(:SSN)"));
        }
    }
    
    @Nested
    @DisplayName("Data Type Specification Generation Tests")
    class DataTypeSpecificationTests {
        
        @Test
        @DisplayName("Should generate CHAR data type specification")
        void shouldGenerateCharDataTypeSpecification() {
            entity.setDataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.CHAR);
            entity.setMaxLength(50);
            
            String spec = entity.generateControlFileFieldSpec();
            
            assertTrue(spec.contains("CHAR(50)"));
        }
        
        @Test
        @DisplayName("Should generate VARCHAR2 data type specification")
        void shouldGenerateVarchar2DataTypeSpecification() {
            entity.setDataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.VARCHAR2);
            entity.setMaxLength(100);
            
            String spec = entity.generateControlFileFieldSpec();
            
            assertTrue(spec.contains("VARCHAR2(100)"));
        }
        
        @Test
        @DisplayName("Should generate NUMBER data type specification")
        void shouldGenerateNumberDataTypeSpecification() {
            entity.setDataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.NUMBER);
            entity.setFormatMask("999999.99");
            
            String spec = entity.generateControlFileFieldSpec();
            
            assertTrue(spec.contains("DECIMAL EXTERNAL"));
            assertTrue(spec.contains("999999.99"));
        }
        
        @Test
        @DisplayName("Should generate DATE data type specification")
        void shouldGenerateDateDataTypeSpecification() {
            entity.setDataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.DATE);
            entity.setFormatMask("MM/DD/YYYY");
            
            String spec = entity.generateControlFileFieldSpec();
            
            assertTrue(spec.contains("DATE"));
            assertTrue(spec.contains("MM/DD/YYYY"));
        }
        
        @Test
        @DisplayName("Should generate TIMESTAMP data type specification")
        void shouldGenerateTimestampDataTypeSpecification() {
            entity.setDataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.TIMESTAMP);
            entity.setFormatMask("YYYY-MM-DD HH24:MI:SS.FF");
            
            String spec = entity.generateControlFileFieldSpec();
            
            assertTrue(spec.contains("TIMESTAMP"));
            assertTrue(spec.contains("YYYY-MM-DD HH24:MI:SS.FF"));
        }
        
        @Test
        @DisplayName("Should generate default date format when none specified")
        void shouldGenerateDefaultDateFormatWhenNoneSpecified() {
            entity.setDataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.DATE);
            entity.setFormatMask(null);
            
            String spec = entity.generateControlFileFieldSpec();
            
            assertTrue(spec.contains("YYYY-MM-DD HH24:MI:SS"));
        }
        
        @Test
        @DisplayName("Should generate CLOB data type specification")
        void shouldGenerateClobDataTypeSpecification() {
            entity.setDataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.CLOB);
            entity.setMaxLength(2000);
            
            String spec = entity.generateControlFileFieldSpec();
            
            assertTrue(spec.contains("CHAR(2000)"));
        }
        
        @Test
        @DisplayName("Should generate RAW data type specification")
        void shouldGenerateRawDataTypeSpecification() {
            entity.setDataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.RAW);
            entity.setMaxLength(100);
            
            String spec = entity.generateControlFileFieldSpec();
            
            assertTrue(spec.contains("RAW(100)"));
        }
    }
    
    @Nested
    @DisplayName("Field Validation Tests")
    class FieldValidationTests {
        
        @Test
        @DisplayName("Should pass validation for valid field configuration")
        void shouldPassValidationForValidFieldConfiguration() {
            assertDoesNotThrow(() -> entity.validateFieldConfiguration());
        }
        
        @Test
        @DisplayName("Should fail validation for empty field name")
        void shouldFailValidationForEmptyFieldName() {
            entity.setFieldName("");
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateFieldConfiguration()
            );
            
            assertEquals("Field name cannot be empty", exception.getMessage());
        }
        
        @Test
        @DisplayName("Should fail validation for empty column name")
        void shouldFailValidationForEmptyColumnName() {
            entity.setColumnName("");
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateFieldConfiguration()
            );
            
            assertTrue(exception.getMessage().contains("Column name cannot be empty"));
        }
        
        @Test
        @DisplayName("Should fail validation for invalid field order")
        void shouldFailValidationForInvalidFieldOrder() {
            entity.setFieldOrder(0);
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateFieldConfiguration()
            );
            
            assertTrue(exception.getMessage().contains("Field order must be positive"));
        }
        
        @Test
        @DisplayName("Should fail validation for null data type")
        void shouldFailValidationForNullDataType() {
            entity.setDataType(null);
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateFieldConfiguration()
            );
            
            assertTrue(exception.getMessage().contains("Data type must be specified"));
        }
        
        @Test
        @DisplayName("Should fail validation for invalid field length with position")
        void shouldFailValidationForInvalidFieldLengthWithPosition() {
            entity.setFieldPosition(1);
            entity.setFieldLength(0);
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateFieldConfiguration()
            );
            
            assertTrue(exception.getMessage().contains("Field length must be positive when position is specified"));
        }
        
        @Test
        @DisplayName("Should fail validation for invalid length range")
        void shouldFailValidationForInvalidLengthRange() {
            entity.setMaxLength(5);
            entity.setMinLength(10);
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateFieldConfiguration()
            );
            
            assertTrue(exception.getMessage().contains("Max length cannot be less than min length"));
        }
        
        @Test
        @DisplayName("Should fail validation for encrypted field without encryption function")
        void shouldFailValidationForEncryptedFieldWithoutEncryptionFunction() {
            entity.setEncrypted("Y");
            entity.setEncryptionFunction(null);
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateFieldConfiguration()
            );
            
            assertTrue(exception.getMessage().contains("Encryption function must be specified for encrypted field"));
        }
        
        @Test
        @DisplayName("Should fail validation for lookup table without column")
        void shouldFailValidationForLookupTableWithoutColumn() {
            entity.setLookupTable("CUSTOMER_TYPES");
            entity.setLookupColumn(null);
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateFieldConfiguration()
            );
            
            assertTrue(exception.getMessage().contains("Lookup column must be specified when lookup table is provided"));
        }
        
        @Test
        @DisplayName("Should validate business rule parameters JSON format")
        void shouldValidateBusinessRuleParametersJsonFormat() {
            entity.setBusinessRuleClass("com.test.Validator");
            entity.setBusinessRuleParameters("invalid json");
            
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> entity.validateFieldConfiguration()
            );
            
            assertTrue(exception.getMessage().contains("Business rule parameters must be valid JSON"));
        }
        
        @Test
        @DisplayName("Should pass validation for valid business rule parameters JSON")
        void shouldPassValidationForValidBusinessRuleParametersJson() {
            entity.setBusinessRuleClass("com.test.Validator");
            entity.setBusinessRuleParameters("{\"minLength\": 5, \"pattern\": \"[A-Z]+\"}");
            
            assertDoesNotThrow(() -> entity.validateFieldConfiguration());
        }
    }
    
    @Nested
    @DisplayName("Enum Handling Tests")
    class EnumHandlingTests {
        
        @Test
        @DisplayName("Should handle SqlLoaderDataType enum correctly")
        void shouldHandleSqlLoaderDataTypeEnum() {
            for (SqlLoaderFieldConfigEntity.SqlLoaderDataType dataType : SqlLoaderFieldConfigEntity.SqlLoaderDataType.values()) {
                entity.setDataType(dataType);
                assertEquals(dataType, entity.getDataType());
                
                String spec = entity.generateControlFileFieldSpec();
                assertNotNull(spec);
                assertFalse(spec.trim().isEmpty());
            }
        }
        
        @Test
        @DisplayName("Should handle CaseSensitive enum correctly")
        void shouldHandleCaseSensitiveEnum() {
            for (SqlLoaderFieldConfigEntity.CaseSensitive caseSensitive : SqlLoaderFieldConfigEntity.CaseSensitive.values()) {
                entity.setCaseSensitive(caseSensitive);
                assertEquals(caseSensitive, entity.getCaseSensitive());
                
                String spec = entity.generateControlFileFieldSpec();
                assertNotNull(spec);
                
                if (caseSensitive != SqlLoaderFieldConfigEntity.CaseSensitive.PRESERVE) {
                    String expectedFunction = caseSensitive == SqlLoaderFieldConfigEntity.CaseSensitive.UPPER ? "UPPER" : "LOWER";
                    assertTrue(spec.contains(expectedFunction));
                }
            }
        }
    }
    
    @Nested
    @DisplayName("PreUpdate Lifecycle Tests")
    class PreUpdateLifecycleTests {
        
        @Test
        @DisplayName("Should update modification fields on update")
        void shouldUpdateModificationFieldsOnUpdate() {
            entity.setVersion(2);
            LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
            
            entity.onUpdate();
            
            assertNotNull(entity.getModifiedDate());
            assertTrue(entity.getModifiedDate().isAfter(beforeUpdate));
            assertEquals(Integer.valueOf(3), entity.getVersion());
        }
        
        @Test
        @DisplayName("Should handle null version on update")
        void shouldHandleNullVersionOnUpdate() {
            entity.setVersion(null);
            
            entity.onUpdate();
            
            assertNotNull(entity.getModifiedDate());
            assertNull(entity.getVersion()); // Should remain null
        }
    }
    
    @Nested
    @DisplayName("Complex Field Scenarios Tests")
    class ComplexFieldScenariosTests {
        
        @Test
        @DisplayName("Should generate specification for encrypted PII field")
        void shouldGenerateSpecificationForEncryptedPiiField() {
            entity.setFieldName("SSN");
            entity.setColumnName("SSN_ENCRYPTED");
            entity.setDataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.CHAR);
            entity.setMaxLength(11);
            entity.setEncrypted("Y");
            entity.setEncryptionFunction("ENCRYPT_SSN");
            entity.setAuditField("Y");
            entity.setTrimField("Y");
            
            String spec = entity.generateControlFileFieldSpec();
            
            assertTrue(spec.contains("SSN_ENCRYPTED"));
            assertTrue(spec.contains("CHAR(11)"));
            assertTrue(spec.contains("TRIM(:SSN_ENCRYPTED)"));
            assertTrue(spec.contains("ENCRYPT_SSN(:SSN_ENCRYPTED)"));
            assertTrue(entity.isEncrypted());
            assertTrue(entity.isAuditField());
        }
        
        @Test
        @DisplayName("Should generate specification for transformed field")
        void shouldGenerateSpecificationForTransformedField() {
            entity.setFieldName("EMAIL");
            entity.setColumnName("EMAIL_ADDRESS");
            entity.setDataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.VARCHAR2);
            entity.setMaxLength(100);
            entity.setSqlExpression("LOWER(TRIM(:EMAIL_ADDRESS))");
            entity.setCaseSensitive(SqlLoaderFieldConfigEntity.CaseSensitive.LOWER);
            entity.setValidationRule("EMAIL_FORMAT_VALID");
            
            String spec = entity.generateControlFileFieldSpec();
            
            assertTrue(spec.contains("EMAIL_ADDRESS"));
            assertTrue(spec.contains("VARCHAR2(100)"));
            assertTrue(spec.contains("LOWER(TRIM(:EMAIL_ADDRESS))"));
            assertTrue(entity.hasValidationRule());
        }
        
        @Test
        @DisplayName("Should handle field with all optional features")
        void shouldHandleFieldWithAllOptionalFeatures() {
            entity.setFieldName("ACCOUNT_NUMBER");
            entity.setColumnName("ACCT_NUM");
            entity.setDataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.VARCHAR2);
            entity.setMaxLength(20);
            entity.setMinLength(10);
            entity.setNullable("N");
            entity.setUniqueConstraint("Y");
            entity.setTrimField("Y");
            entity.setCaseSensitive(SqlLoaderFieldConfigEntity.CaseSensitive.UPPER);
            entity.setValidationRule("ACCOUNT_NUMBER_FORMAT");
            entity.setLookupTable("VALID_ACCOUNTS");
            entity.setLookupColumn("ACCOUNT_ID");
            entity.setBusinessRuleClass("com.fabric.validation.AccountValidator");
            entity.setBusinessRuleParameters("{\"checkDigit\": true}");
            entity.setAuditField("Y");
            
            assertDoesNotThrow(() -> entity.validateFieldConfiguration());
            
            String spec = entity.generateControlFileFieldSpec();
            assertNotNull(spec);
            assertTrue(spec.contains("ACCT_NUM"));
            assertTrue(spec.contains("VARCHAR2(20)"));
            assertTrue(spec.contains("TRIM(:ACCT_NUM)"));
            assertTrue(spec.contains("UPPER(:ACCT_NUM)"));
            
            assertTrue(entity.hasValidationRule());
            assertTrue(entity.hasLookupReference());
            assertTrue(entity.hasBusinessRule());
            assertTrue(entity.hasUniqueConstraint());
            assertTrue(entity.isAuditField());
        }
    }
    
    @Test
    @DisplayName("Should handle toString without circular references")
    void shouldHandleToStringWithoutCircularReferences() {
        String toString = entity.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("SqlLoaderFieldConfigEntity"));
        assertTrue(toString.contains("fieldName=CUSTOMER_ID"));
        // Should exclude lazy-loaded parent reference
        assertFalse(toString.contains("sqlLoaderConfig"));
    }
    
    @Test
    @DisplayName("Should handle equals and hashCode correctly")
    void shouldHandleEqualsAndHashCodeCorrectly() {
        SqlLoaderFieldConfigEntity entity1 = SqlLoaderFieldConfigEntity.builder()
                .fieldConfigId(1L)
                .configId("TEST-001")
                .fieldName("EMAIL")
                .columnName("EMAIL")
                .dataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.VARCHAR2)
                .fieldOrder(1)
                .createdBy("admin")
                .build();
        
        SqlLoaderFieldConfigEntity entity2 = SqlLoaderFieldConfigEntity.builder()
                .fieldConfigId(1L)
                .configId("TEST-001")
                .fieldName("EMAIL")
                .columnName("EMAIL")
                .dataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.VARCHAR2)
                .fieldOrder(1)
                .createdBy("admin")
                .build();
        
        SqlLoaderFieldConfigEntity entity3 = SqlLoaderFieldConfigEntity.builder()
                .fieldConfigId(2L)
                .configId("TEST-001")
                .fieldName("PHONE")
                .columnName("PHONE")
                .dataType(SqlLoaderFieldConfigEntity.SqlLoaderDataType.VARCHAR2)
                .fieldOrder(2)
                .createdBy("admin")
                .build();
        
        assertEquals(entity1, entity2);
        assertNotEquals(entity1, entity3);
        assertEquals(entity1.hashCode(), entity2.hashCode());
        assertNotEquals(entity1.hashCode(), entity3.hashCode());
    }
}