#!/bin/bash

# Test the configuration save endpoint directly

echo "Testing Configuration Save Endpoint..."
echo "====================================="

# Test data for a simple configuration
CONFIG_DATA='{
  "sourceSystem": "hr",
  "jobName": "test_config_job",
  "transactionType": "200",
  "description": "Test configuration from script",
  "fieldMappings": [
    {
      "fieldName": "test_field",
      "sourceField": "source_test",
      "targetField": "target_test",
      "targetPosition": 1,
      "length": 10,
      "dataType": "String",
      "transformationType": "source",
      "transactionType": "200"
    }
  ],
  "createdBy": "test-user",
  "version": 1
}'

echo "Sending POST request to: http://localhost:8080/api/ui/mappings/save"
echo "Payload:"
echo "$CONFIG_DATA" | jq .

echo ""
echo "Response:"
curl -X POST \
  -H "Content-Type: application/json" \
  -d "$CONFIG_DATA" \
  http://localhost:8080/api/ui/mappings/save \
  -w "\nHTTP Status: %{http_code}\n" \
  -v

echo ""
echo "Test completed."