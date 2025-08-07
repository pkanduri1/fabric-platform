#!/bin/bash

# Test if we can query the batch_configurations table via the API

echo "Testing Database Query Endpoint..."
echo "=================================="

echo "Attempting to get configurations for hr/test_config_job:"

curl -X GET \
  "http://localhost:8080/api/ui/mappings/hr/test_config_job" \
  -w "\nHTTP Status: %{http_code}\n" \
  -H "Accept: application/json"

echo ""
echo "Test completed."