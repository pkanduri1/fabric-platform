package com.fabric.batch.service;

import java.util.List;
import java.util.Map;

import com.fabric.batch.model.FieldMappingConfig;
import com.fabric.batch.model.JobConfig;
import com.fabric.batch.model.SourceField;
import com.fabric.batch.model.SourceSystem;
import com.fabric.batch.model.TestResult;
import com.fabric.batch.model.ValidationResult;

public interface ConfigurationService {

	// Source Systems
	List<SourceSystem> getAllSourceSystems();

	SourceSystem getSourceSystem(String systemId);

	List<JobConfig> getJobsForSystem(String systemId);

	List<SourceField> getSourceFields(String systemId, String jobName);

	// Field Mappings
	FieldMappingConfig getFieldMappings(String sourceSystem, String jobName);

	FieldMappingConfig getFieldMappings(String sourceSystem, String jobName, String transactionType);

	String saveConfiguration(FieldMappingConfig config);

	// Validation & Generation
	ValidationResult validateConfiguration(FieldMappingConfig config);

	String generateYaml(FieldMappingConfig config);

	List<String> generatePreview(FieldMappingConfig mapping, List<Map<String, Object>> sampleData);

	TestResult testConfiguration(String sourceSystem, String jobName);

	List<FieldMappingConfig> getAllTransactionTypesForJob(String sourceSystem, String jobName);

}