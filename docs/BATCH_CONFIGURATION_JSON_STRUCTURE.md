# Batch Configuration JSON Structure

**Purpose:** This document defines the JSON structure used by `YamlMappingService.java` for batch processing field transformations.

**Database Storage:** The JSON configuration is stored in the `BATCH_CONFIGURATIONS.CONFIGURATION_JSON` CLOB column.

**Version:** 1.0
**Last Updated:** October 5, 2025

---

## Table of Contents

1. [Overview](#overview)
2. [Root Structure](#root-structure)
3. [Field Mapping Object](#field-mapping-object)
4. [Transformation Types](#transformation-types)
5. [Complete Examples](#complete-examples)
6. [Validation Rules](#validation-rules)

---

## Overview

The JSON configuration defines how data from source systems is transformed into fixed-width output files. The `YamlMappingService.java` class parses this JSON and applies transformations during batch processing.

### Key Concepts:

- **Source Field**: Database column name from the query result
- **Target Field**: Field name in the output file
- **Transformation Type**: How the value is generated (source, constant, composite, conditional, blank)
- **Target Position**: Position in the output file (1-based index)
- **Length**: Field length in characters for fixed-width format
- **Padding**: How to pad values (left/right) with specified character

---

## Root Structure

```json
{
  "id": "string",
  "sourceSystem": "string",
  "jobName": "string",
  "transactionType": "string",
  "description": "string",
  "fieldMappings": [],
  "templateId": "string|null",
  "masterQueryId": "number|null",
  "active": boolean,
  "version": number,
  "createdDate": "ISO-8601 timestamp",
  "lastModified": "ISO-8601 timestamp",
  "modifiedBy": "string"
}
```

### Root Field Descriptions:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique configuration identifier - format: `{sourceSystem}_{jobName}_{transactionType}_{timestamp}` |
| `sourceSystem` | string | Yes | Source system identifier (e.g., ENCORE, MTG, DDA) |
| `jobName` | string | Yes | Job name for batch processing |
| `transactionType` | string | Yes | Transaction type code (e.g., 200, 300, 400, 900) |
| `description` | string | No | Human-readable description of the configuration |
| `fieldMappings` | array | Yes | Array of field mapping objects (see below) |
| `templateId` | string | No | Reference to template used to create this configuration |
| `masterQueryId` | number | No | Foreign key to MASTER_QUERY_CONFIG.ID |
| `active` | boolean | Yes | Whether this configuration is active |
| `version` | number | Yes | Configuration version number |
| `createdDate` | string | Yes | ISO-8601 timestamp when created |
| `lastModified` | string | Yes | ISO-8601 timestamp of last modification |
| `modifiedBy` | string | Yes | User who last modified this configuration |

---

## Field Mapping Object

Each element in the `fieldMappings` array represents a single field in the output file.

### Complete Field Mapping Structure:

```json
{
  "fieldName": "string",
  "targetField": "string",
  "sourceField": "string|null",
  "from": "string|null",
  "targetPosition": number,
  "length": number,
  "dataType": "string",
  "format": "string|null",
  "sourceFormat": "string|null",
  "targetFormat": "string|null",
  "transformationType": "string",
  "value": "string|null",
  "defaultValue": "string|null",
  "transform": "string|null",
  "pad": "string",
  "padChar": "string|null",
  "conditions": [],
  "composite": boolean,
  "sources": [],
  "delimiter": "string|null",
  "expression": "string|null",
  "encryptionLevel": "string",
  "piiClassification": "string",
  "validationRequired": boolean,
  "complianceLevel": "string",
  "businessContext": "string|null",
  "businessRuleId": "number|null"
}
```

### Field Mapping Field Descriptions:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `fieldName` | string | Yes | Unique field identifier |
| `targetField` | string | Yes | Target field name in output file |
| `sourceField` | string | Conditional | Source database column name (required for `transformationType: "source"`) |
| `from` | string | No | Legacy field (not actively used) |
| `targetPosition` | number | Yes | Position in output file (1-based) - **determines output order** |
| `length` | number | Yes | Field length in characters for fixed-width format |
| `dataType` | string | Yes | Data type: `string`, `number`, `date`, `boolean` |
| `format` | string | No | Format specification (e.g., `YYYYMMDD` for dates, `9(12)v9(6)` for numbers) |
| `sourceFormat` | string | No | Source data format (for date/number transformations) |
| `targetFormat` | string | No | Target output format (for date/number transformations) |
| `transformationType` | string | Yes | **Transformation type** (see section below) |
| `value` | string | Conditional | **Constant value** (required for `transformationType: "constant"`) |
| `defaultValue` | string | No | Fallback value if source is null/empty |
| `transform` | string | Conditional | Transformation operation for composite fields (`sum`, `concat`) |
| `pad` | string | Yes | Padding direction: `left`, `right` |
| `padChar` | string | No | Padding character (default: space for strings, 0 for numbers) |
| `conditions` | array | Conditional | Conditional logic array (required for `transformationType: "conditional"`) |
| `composite` | boolean | No | Indicates if field is composite (auto-set based on transformationType) |
| `sources` | array | Conditional | Source fields array (required for `transformationType: "composite"`) |
| `delimiter` | string | Conditional | Delimiter for composite concatenation |
| `expression` | string | No | Future DSL expression support |
| `encryptionLevel` | string | No | Encryption level: `NONE`, `STANDARD`, `HIGH` (default: `NONE`) |
| `piiClassification` | string | No | PII classification: `NONE`, `LOW`, `MEDIUM`, `HIGH` (default: `NONE`) |
| `validationRequired` | boolean | No | Whether validation is required (default: `false`) |
| `complianceLevel` | string | No | Compliance level: `STANDARD`, `HIGH`, `CRITICAL` (default: `STANDARD`) |
| `businessContext` | string | No | Business context description |
| `businessRuleId` | number | No | Reference to business rule |

---

## Transformation Types

The `transformationType` field determines how the field value is generated. The `YamlMappingService.transformField()` method handles these transformations.

### 1. Source Transformation

**Purpose:** Copy value directly from a source database column.

**Required Fields:**
- `sourceField`: Database column name
- `transformationType`: `"source"`

**Example:**
```json
{
  "fieldName": "acct-num",
  "targetField": "acct-num",
  "sourceField": "acct_num",
  "targetPosition": 2,
  "length": 18,
  "dataType": "string",
  "transformationType": "source",
  "pad": "right",
  "defaultValue": ""
}
```

**Processing Logic:**
```java
// YamlMappingService.java line 116-118
case "source":
    value = resolveValue(mapping.getSourceField(), row, mapping.getDefaultValue());
    break;
```

**Case-Insensitive Lookup:** The service performs case-insensitive column name matching, so `ACCT_NUM`, `acct_num`, and `Acct_Num` all resolve to the same column.

---

### 2. Constant Transformation

**Purpose:** Use a fixed constant value for all records.

**Required Fields:**
- `value`: The constant value to use
- `transformationType`: `"constant"`

**Example:**
```json
{
  "fieldName": "location-code",
  "targetField": "location-code",
  "targetPosition": 1,
  "length": 6,
  "dataType": "string",
  "transformationType": "constant",
  "value": "000001",
  "pad": "right"
}
```

**Processing Logic:**
```java
// YamlMappingService.java line 108-114
case "constant":
    value = mapping.getValue();
    if (value == null || value.trim().isEmpty()) {
        value = mapping.getDefaultValue();
    }
    break;
```

**Important:** Always use the `value` field for constants, NOT `sourceField`. The ConfigurationController automatically corrects this during save.

---

### 3. Composite Transformation

**Purpose:** Combine multiple source fields using operations like concatenation or summation.

**Required Fields:**
- `sources`: Array of source field objects
- `transform`: Operation type (`concat` or `sum`)
- `transformationType`: `"composite"`
- `delimiter`: For `concat` operation (optional, defaults to empty string)

**Example - Concatenation:**
```json
{
  "fieldName": "full-name",
  "targetField": "full-name",
  "targetPosition": 5,
  "length": 50,
  "dataType": "string",
  "transformationType": "composite",
  "transform": "concat",
  "delimiter": " ",
  "pad": "right",
  "sources": [
    {
      "sourceField": "first_name"
    },
    {
      "sourceField": "middle_initial"
    },
    {
      "sourceField": "last_name"
    }
  ]
}
```

**Example - Summation:**
```json
{
  "fieldName": "total-balance",
  "targetField": "total-balance",
  "targetPosition": 10,
  "length": 15,
  "dataType": "number",
  "transformationType": "composite",
  "transform": "sum",
  "pad": "left",
  "padChar": "0",
  "sources": [
    {
      "sourceField": "checking_balance"
    },
    {
      "sourceField": "savings_balance"
    },
    {
      "sourceField": "money_market_balance"
    }
  ]
}
```

**Processing Logic:**
```java
// YamlMappingService.java line 120-123
case "composite":
    value = handleComposite(mapping.getSources(), row, mapping.getTransform(),
                          mapping.getDelimiter(), mapping.getDefaultValue());
    break;
```

---

### 4. Conditional Transformation

**Purpose:** Apply if-then-else logic to determine the field value based on conditions.

**Required Fields:**
- `conditions`: Array of condition objects
- `transformationType`: `"conditional"`

**Condition Object Structure:**
```json
{
  "ifExpr": "field_name operator value",
  "then": "value_or_field_name",
  "elseExpr": "value_or_field_name",
  "elseIfExprs": []
}
```

**Supported Operators:**
- Equality: `==`, `=`
- Inequality: `!=`
- Comparison: `<`, `>`, `<=`, `>=`
- Logical: `&&` (AND), `||` (OR), `!` (NOT)

**Example - Simple Conditional:**
```json
{
  "fieldName": "account-status",
  "targetField": "account-status",
  "targetPosition": 8,
  "length": 1,
  "dataType": "string",
  "transformationType": "conditional",
  "pad": "right",
  "conditions": [
    {
      "ifExpr": "status == 'ACTIVE'",
      "then": "A",
      "elseExpr": "I"
    }
  ]
}
```

**Example - Complex Conditional with Else-If:**
```json
{
  "fieldName": "risk-category",
  "targetField": "risk-category",
  "targetPosition": 12,
  "length": 10,
  "dataType": "string",
  "transformationType": "conditional",
  "pad": "right",
  "conditions": [
    {
      "ifExpr": "credit_score >= 750",
      "then": "LOW",
      "elseIfExprs": [
        {
          "ifExpr": "credit_score >= 650 && credit_score < 750",
          "then": "MEDIUM"
        },
        {
          "ifExpr": "credit_score >= 550 && credit_score < 650",
          "then": "HIGH"
        }
      ],
      "elseExpr": "CRITICAL"
    }
  ]
}
```

**Example - Logical Operators:**
```json
{
  "fieldName": "eligible-flag",
  "targetField": "eligible-flag",
  "targetPosition": 15,
  "length": 1,
  "dataType": "string",
  "transformationType": "conditional",
  "pad": "right",
  "conditions": [
    {
      "ifExpr": "(age >= 18 && age <= 65) && income > 50000",
      "then": "Y",
      "elseExpr": "N"
    }
  ]
}
```

**Processing Logic:**
```java
// YamlMappingService.java line 125-127
case "conditional":
    value = evaluateConditional(mapping.getConditions(), row, mapping.getDefaultValue());
    break;
```

---

### 5. Blank Transformation

**Purpose:** Explicitly set field to blank/empty value.

**Required Fields:**
- `transformationType`: `"blank"`
- `defaultValue`: Value to use (typically empty string or spaces)

**Example:**
```json
{
  "fieldName": "reserved-field",
  "targetField": "reserved-field",
  "targetPosition": 20,
  "length": 10,
  "dataType": "string",
  "transformationType": "blank",
  "defaultValue": "",
  "pad": "right"
}
```

**Processing Logic:**
```java
// YamlMappingService.java line 129-131
case "blank":
    value = mapping.getDefaultValue();
    break;
```

---

## Complete Examples

### Example 1: Simple Transaction File (ATOCTRAN 900)

This example demonstrates all basic transformation types for a transaction file.

```json
{
  "id": "ENCORE_atoctran_900_1725012345678",
  "sourceSystem": "ENCORE",
  "jobName": "atoctran",
  "transactionType": "900",
  "description": "ENCORE transaction file - type 900 (account updates)",
  "fieldMappings": [
    {
      "fieldName": "location-code",
      "targetField": "location-code",
      "sourceField": null,
      "targetPosition": 1,
      "length": 6,
      "dataType": "string",
      "transformationType": "constant",
      "value": "000001",
      "defaultValue": null,
      "pad": "right",
      "padChar": null
    },
    {
      "fieldName": "acct-num",
      "targetField": "acct-num",
      "sourceField": "acct_num",
      "targetPosition": 2,
      "length": 18,
      "dataType": "string",
      "transformationType": "source",
      "value": null,
      "defaultValue": "",
      "pad": "right",
      "padChar": null
    },
    {
      "fieldName": "transaction-type",
      "targetField": "transaction-type",
      "sourceField": null,
      "targetPosition": 3,
      "length": 3,
      "dataType": "string",
      "transformationType": "constant",
      "value": "900",
      "defaultValue": null,
      "pad": "right",
      "padChar": null
    },
    {
      "fieldName": "batch-date",
      "targetField": "batch-date",
      "sourceField": "batch_date",
      "targetPosition": 4,
      "length": 8,
      "dataType": "date",
      "format": "YYYYMMDD",
      "transformationType": "source",
      "value": null,
      "defaultValue": "",
      "pad": "right",
      "padChar": null
    },
    {
      "fieldName": "transaction-amt",
      "targetField": "transaction-amt",
      "sourceField": "transaction_amt",
      "targetPosition": 5,
      "length": 15,
      "dataType": "number",
      "format": "9(12)v9(2)",
      "transformationType": "source",
      "value": null,
      "defaultValue": "0",
      "pad": "left",
      "padChar": "0"
    }
  ],
  "templateId": "atoctran_900",
  "masterQueryId": 12345,
  "active": true,
  "version": 1,
  "createdDate": "2025-08-14T10:30:00.000Z",
  "lastModified": "2025-08-14T10:30:00.000Z",
  "modifiedBy": "system"
}
```

**Expected Output (Fixed-Width):**
```
000001123456789012345678900202508141234567890.00
^^^^^^ ^^^^^^^^^^^^^^^^^ ^^^ ^^^^^^^^ ^^^^^^^^^^^^^^^
loc    acct_num          txn  date     amount
```

---

### Example 2: Complex Customer File with All Transformation Types

```json
{
  "id": "MTG_customer_master_200_1725012345678",
  "sourceSystem": "MTG",
  "jobName": "customer_master",
  "transactionType": "200",
  "description": "Mortgage customer master file with complex transformations",
  "fieldMappings": [
    {
      "fieldName": "record-type",
      "targetField": "record-type",
      "targetPosition": 1,
      "length": 2,
      "dataType": "string",
      "transformationType": "constant",
      "value": "CM",
      "pad": "right"
    },
    {
      "fieldName": "customer-id",
      "targetField": "customer-id",
      "sourceField": "customer_id",
      "targetPosition": 2,
      "length": 12,
      "dataType": "string",
      "transformationType": "source",
      "defaultValue": "",
      "pad": "left",
      "padChar": "0"
    },
    {
      "fieldName": "full-name",
      "targetField": "full-name",
      "targetPosition": 3,
      "length": 50,
      "dataType": "string",
      "transformationType": "composite",
      "transform": "concat",
      "delimiter": " ",
      "pad": "right",
      "sources": [
        {"sourceField": "first_name"},
        {"sourceField": "middle_initial"},
        {"sourceField": "last_name"}
      ]
    },
    {
      "fieldName": "total-balance",
      "targetField": "total-balance",
      "targetPosition": 4,
      "length": 18,
      "dataType": "number",
      "format": "9(15)v9(2)",
      "transformationType": "composite",
      "transform": "sum",
      "pad": "left",
      "padChar": "0",
      "sources": [
        {"sourceField": "mortgage_balance"},
        {"sourceField": "heloc_balance"},
        {"sourceField": "escrow_balance"}
      ]
    },
    {
      "fieldName": "customer-status",
      "targetField": "customer-status",
      "targetPosition": 5,
      "length": 10,
      "dataType": "string",
      "transformationType": "conditional",
      "pad": "right",
      "conditions": [
        {
          "ifExpr": "status == 'ACTIVE' && delinquency_days == 0",
          "then": "CURRENT",
          "elseIfExprs": [
            {
              "ifExpr": "status == 'ACTIVE' && delinquency_days > 0 && delinquency_days <= 30",
              "then": "LATE_1"
            },
            {
              "ifExpr": "status == 'ACTIVE' && delinquency_days > 30 && delinquency_days <= 60",
              "then": "LATE_2"
            },
            {
              "ifExpr": "status == 'ACTIVE' && delinquency_days > 60",
              "then": "DEFAULT"
            }
          ],
          "elseExpr": "INACTIVE"
        }
      ]
    },
    {
      "fieldName": "risk-rating",
      "targetField": "risk-rating",
      "targetPosition": 6,
      "length": 1,
      "dataType": "string",
      "transformationType": "conditional",
      "pad": "right",
      "conditions": [
        {
          "ifExpr": "fico_score >= 750 && dti_ratio <= 0.36",
          "then": "A",
          "elseIfExprs": [
            {
              "ifExpr": "fico_score >= 700 && dti_ratio <= 0.43",
              "then": "B"
            },
            {
              "ifExpr": "fico_score >= 650",
              "then": "C"
            }
          ],
          "elseExpr": "D"
        }
      ]
    },
    {
      "fieldName": "phone-number",
      "targetField": "phone-number",
      "sourceField": "phone_number",
      "targetPosition": 7,
      "length": 10,
      "dataType": "string",
      "transformationType": "source",
      "defaultValue": "",
      "pad": "right",
      "piiClassification": "MEDIUM",
      "encryptionLevel": "STANDARD"
    },
    {
      "fieldName": "ssn-last-four",
      "targetField": "ssn-last-four",
      "sourceField": "ssn_last_four",
      "targetPosition": 8,
      "length": 4,
      "dataType": "string",
      "transformationType": "source",
      "defaultValue": "0000",
      "pad": "left",
      "padChar": "0",
      "piiClassification": "HIGH",
      "encryptionLevel": "HIGH",
      "complianceLevel": "CRITICAL"
    },
    {
      "fieldName": "reserved-1",
      "targetField": "reserved-1",
      "targetPosition": 9,
      "length": 20,
      "dataType": "string",
      "transformationType": "blank",
      "defaultValue": "",
      "pad": "right"
    },
    {
      "fieldName": "reserved-2",
      "targetField": "reserved-2",
      "targetPosition": 10,
      "length": 20,
      "dataType": "string",
      "transformationType": "blank",
      "defaultValue": "",
      "pad": "right"
    }
  ],
  "templateId": "customer_master_200",
  "masterQueryId": 67890,
  "active": true,
  "version": 2,
  "createdDate": "2025-08-14T10:30:00.000Z",
  "lastModified": "2025-08-20T15:45:00.000Z",
  "modifiedBy": "jdoe"
}
```

---

## Validation Rules

### Required Field Validations:

1. **Root Level:**
   - `sourceSystem` must not be empty
   - `jobName` must not be empty
   - `transactionType` must not be empty
   - `fieldMappings` array must contain at least one element
   - `version` must be >= 1

2. **Field Mapping Level:**
   - `fieldName` must be unique within the configuration
   - `targetPosition` must be unique and > 0
   - `length` must be > 0
   - `transformationType` must be one of: `source`, `constant`, `composite`, `conditional`, `blank`
   - `dataType` must be one of: `string`, `number`, `date`, `boolean`
   - `pad` must be one of: `left`, `right`

3. **Transformation-Specific Validations:**

   **For `transformationType: "source"`:**
   - `sourceField` must not be empty

   **For `transformationType: "constant"`:**
   - `value` must not be null (can be empty string)

   **For `transformationType: "composite"`:**
   - `sources` array must contain at least one element
   - `transform` must be one of: `concat`, `sum`
   - Each source object must have `sourceField`

   **For `transformationType: "conditional"`:**
   - `conditions` array must contain at least one element
   - Each condition must have `ifExpr` and `then`

4. **Data Type Validations:**
   - Numbers should have `pad: "left"` and `padChar: "0"`
   - Strings typically use `pad: "right"`
   - Dates require `format` specification

5. **Compliance Validations:**
   - If `piiClassification` is `HIGH` or `MEDIUM`, `encryptionLevel` should not be `NONE`
   - If `complianceLevel` is `CRITICAL`, audit trail is mandatory

---

## JSON Schema (Optional)

For automated validation, here's a JSON Schema definition:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["sourceSystem", "jobName", "transactionType", "fieldMappings"],
  "properties": {
    "id": {"type": "string"},
    "sourceSystem": {"type": "string", "minLength": 1},
    "jobName": {"type": "string", "minLength": 1},
    "transactionType": {"type": "string", "minLength": 1},
    "description": {"type": "string"},
    "fieldMappings": {
      "type": "array",
      "minItems": 1,
      "items": {
        "$ref": "#/definitions/fieldMapping"
      }
    },
    "active": {"type": "boolean"},
    "version": {"type": "integer", "minimum": 1}
  },
  "definitions": {
    "fieldMapping": {
      "type": "object",
      "required": ["fieldName", "targetPosition", "length", "dataType", "transformationType", "pad"],
      "properties": {
        "fieldName": {"type": "string", "minLength": 1},
        "targetPosition": {"type": "integer", "minimum": 1},
        "length": {"type": "integer", "minimum": 1},
        "dataType": {"enum": ["string", "number", "date", "boolean"]},
        "transformationType": {"enum": ["source", "constant", "composite", "conditional", "blank"]},
        "pad": {"enum": ["left", "right"]},
        "sourceField": {"type": ["string", "null"]},
        "value": {"type": ["string", "null"]},
        "defaultValue": {"type": ["string", "null"]}
      }
    }
  }
}
```

---

## Usage in Code

### Saving Configuration:

```java
// POST /api/config/mappings/save
ConfigurationController.saveConfiguration(BatchConfiguration config)
```

### Loading for Batch Execution:

```java
// YamlMappingService reads from BATCH_CONFIGURATIONS.CONFIGURATION_JSON
YamlMapping mapping = yamlMappingService.getMapping(template, transactionType);
List<Map.Entry<String, FieldMapping>> fields = yamlMappingService.loadFieldMappings(yamlPath);
```

### Transforming Data:

```java
// For each database row, for each field mapping
for (Map.Entry<String, FieldMapping> entry : fieldMappings) {
    FieldMapping mapping = entry.getValue();
    String transformedValue = yamlMappingService.transformField(row, mapping);
    // Add to output record at targetPosition
}
```

---

## Best Practices

1. **Always set `targetPosition` sequentially** - This determines the order of fields in the output file
2. **Use `defaultValue` for safety** - Prevents null pointer exceptions during transformation
3. **Validate JSON before saving** - Use the validation rules or JSON Schema
4. **Test with sample data** - Use ENCORE_TEST_DATA or similar test data sets
5. **Document business rules** - Use `businessContext` and `businessRuleId` fields
6. **Mark PII fields** - Set `piiClassification` and `encryptionLevel` appropriately
7. **Use case-insensitive column names** - The service handles case variations automatically
8. **Keep configurations version-controlled** - Increment `version` for each change
9. **Use templates when possible** - Create from TEMPLATE_SOURCE_MAPPINGS for consistency
10. **Test conditional logic thoroughly** - Complex conditions can be error-prone

---

## References

- **Source Code:** `YamlMappingService.java` - Line 102-353
- **Model Classes:** `FieldMapping.java`, `YamlMapping.java`, `Condition.java`
- **Database Table:** `BATCH_CONFIGURATIONS.CONFIGURATION_JSON` (CLOB)
- **Sample Data:** `test_complete_final_verification.json`
- **Related Documentation:**
  - [DATA_DICTIONARY.md](DATA_DICTIONARY.md)
  - [cm3int_schema_v1.0_20251005.sql](cm3int_schema_v1.0_20251005.sql)

---

**© 2025 Truist Financial Corporation. All rights reserved.**

*This document is confidential and proprietary. Distribution is restricted to authorized personnel only.*
