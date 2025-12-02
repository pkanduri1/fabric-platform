package com.fabric.batch.sqlloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Generator for SQL*Loader control files based on configuration.
 * Creates optimized control files for different file types and validation requirements.
 */
@Slf4j
@Component
public class ControlFileGenerator {
    
    private static final String CONTROL_FILE_EXTENSION = ".ctl";
    private static final String DEFAULT_ENCODING = "UTF-8";
    
    /**
     * Generate a SQL*Loader control file based on the provided configuration.
     * 
     * @param config SQL*Loader configuration
     * @return Path to the generated control file
     * @throws IOException if control file cannot be created
     */
    public Path generateControlFile(SqlLoaderConfig config) throws IOException {
        config.validate();
        
        Path controlFilePath = getControlFilePath(config);
        
        // Ensure directory exists
        Files.createDirectories(controlFilePath.getParent());
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(controlFilePath.toFile(), false))) {
            writeControlFileHeader(writer, config);
            writeLoadStatement(writer, config);
            writeFileSpecification(writer, config);
            writeFieldSpecifications(writer, config);
            writeControlFileFooter(writer, config);
        }
        
        log.info("Generated SQL*Loader control file: {}", controlFilePath);
        return controlFilePath;
    }
    
    /**
     * Generate control file for pipe-delimited files with standard configuration.
     */
    public Path generatePipeDelimitedControlFile(SqlLoaderConfig config) throws IOException {
        // Set pipe-delimited specific configurations
        config.setFieldDelimiter("|");
        config.setRecordDelimiter("\n");
        config.setStringDelimiter("\"");
        config.setOptionalEnclosures(true);
        config.setTrimWhitespace(true);
        
        return generateControlFile(config);
    }
    
    /**
     * Generate control file for fixed-width files.
     */
    public Path generateFixedWidthControlFile(SqlLoaderConfig config) throws IOException {
        // Validate fixed-width configuration
        validateFixedWidthConfig(config);
        
        // Clear delimiter settings for fixed-width
        config.setFieldDelimiter(null);
        config.setStringDelimiter(null);
        config.setOptionalEnclosures(false);
        
        return generateControlFile(config);
    }
    
    /**
     * Write the control file header with options and comments.
     */
    private void writeControlFileHeader(BufferedWriter writer, SqlLoaderConfig config) throws IOException {
        writer.write("-- SQL*Loader Control File\n");
        writer.write("-- Generated for job: " + config.getJobName() + "\n");
        writer.write("-- Target table: " + config.getTargetTable() + "\n");
        writer.write("-- Configuration ID: " + config.getConfigId() + "\n");
        if (config.getCorrelationId() != null) {
            writer.write("-- Correlation ID: " + config.getCorrelationId() + "\n");
        }
        writer.write("-- Generated at: " + java.time.LocalDateTime.now() + "\n");
        writer.write("\n");
        
        // Write load options
        writer.write("OPTIONS (\n");
        
        if (config.getDirectPath() != null && config.getDirectPath()) {
            writer.write("  DIRECT=TRUE,\n");
        }
        
        if (config.getErrors() != null) {
            writer.write("  ERRORS=" + config.getErrors() + ",\n");
        }
        
        if (config.getSkip() != null && config.getSkip() > 0) {
            writer.write("  SKIP=" + config.getSkip() + ",\n");
        }
        
        if (config.getRows() != null && (config.getDirectPath() == null || !config.getDirectPath())) {
            writer.write("  ROWS=" + config.getRows() + ",\n");
        }
        
        if (config.getBindSize() != null) {
            writer.write("  BINDSIZE=" + config.getBindSize() + ",\n");
        }
        
        if (config.getReadSize() != null) {
            writer.write("  READSIZE=" + config.getReadSize() + ",\n");
        }
        
        if (config.getSilentMode() != null && config.getSilentMode()) {
            writer.write("  SILENT=ALL,\n");
        }
        
        // Add resumable options
        if (config.getResumable() != null && config.getResumable()) {
            writer.write("  RESUMABLE=TRUE,\n");
            if (config.getResumableTimeout() != null) {
                writer.write("  RESUMABLE_TIMEOUT=" + config.getResumableTimeout() + ",\n");
            }
        }
        
        // Remove trailing comma and close options
        writer.write("  MULTITHREADING=TRUE\n");
        writer.write(")\n\n");
    }
    
    /**
     * Write the LOAD statement.
     */
    private void writeLoadStatement(BufferedWriter writer, SqlLoaderConfig config) throws IOException {
        writer.write("LOAD DATA\n");
        
        // Character set
        if (config.getCharacterSet() != null) {
            writer.write("CHARACTERSET " + config.getCharacterSet() + "\n");
        }
        
        // Input file
        writer.write("INFILE '" + config.getDataFileName() + "'\n");
        
        // Bad file
        if (config.getBadFileName() != null) {
            writer.write("BADFILE '" + config.getBadFileName() + "'\n");
        }
        
        // Discard file
        if (config.getDiscardFileName() != null) {
            writer.write("DISCARDFILE '" + config.getDiscardFileName() + "'\n");
        }
        
        writer.write("\n");
    }
    
    /**
     * Write file format specifications.
     */
    private void writeFileSpecification(BufferedWriter writer, SqlLoaderConfig config) throws IOException {
        writer.write(config.getLoadMethodForControlFile() + "\n");
        
        // Field specifications based on file type
        if (config.getFieldDelimiter() != null) {
            // Delimited file format
            writer.write("FIELDS TERMINATED BY '" + escapeDelimiter(config.getFieldDelimiter()) + "'");
            
            if (config.getStringDelimiter() != null) {
                writer.write(" OPTIONALLY ENCLOSED BY '" + config.getStringDelimiter() + "'");
            }
            
            if (config.getTrimWhitespace() != null && config.getTrimWhitespace()) {
                writer.write(" LTRIM");
            }
            
            writer.write("\n");
        }
        
        // Record terminator
        if (config.getRecordDelimiter() != null && !"\n".equals(config.getRecordDelimiter())) {
            writer.write("TERMINATED BY '" + escapeDelimiter(config.getRecordDelimiter()) + "'\n");
        }
        
        writer.write("(\n");
    }
    
    /**
     * Write field specifications for each column.
     */
    private void writeFieldSpecifications(BufferedWriter writer, SqlLoaderConfig config) throws IOException {
        List<SqlLoaderConfig.FieldConfig> fields = config.getFields();
        
        for (int i = 0; i < fields.size(); i++) {
            SqlLoaderConfig.FieldConfig field = fields.get(i);
            writeFieldSpecification(writer, field, config);
            
            // Add comma except for last field
            if (i < fields.size() - 1) {
                writer.write(",\n");
            } else {
                writer.write("\n");
            }
        }
    }
    
    /**
     * Write specification for a single field.
     */
    private void writeFieldSpecification(BufferedWriter writer, SqlLoaderConfig.FieldConfig field, SqlLoaderConfig config) throws IOException {
        writer.write("  " + field.getColumnName());
        
        // Position specification for fixed-width files
        if (field.getPosition() != null) {
            if (field.getLength() != null) {
                writer.write(" POSITION(" + field.getPosition() + ":" + (field.getPosition() + field.getLength() - 1) + ")");
            } else {
                writer.write(" POSITION(" + field.getPosition() + ")");
            }
        }
        
        // Data type specification
        if (field.getDataType() != null) {
            writer.write(" " + field.getDataType());
            
            // Add length for CHAR fields
            if ("CHAR".equalsIgnoreCase(field.getDataType()) && field.getMaxLength() != null) {
                writer.write("(" + field.getMaxLength() + ")");
            }
        }
        
        // Format specification for dates and numbers
        if (field.getFormat() != null) {
            writer.write(" \"" + field.getFormat() + "\"");
        }
        
        // Null handling
        if (field.getNullIf() != null) {
            writer.write(" NULLIF " + field.getNullIf());
        }
        
        // Default value
        if (field.getDefaultValue() != null) {
            writer.write(" DEFAULTIF " + field.getDefaultValue());
        }
        
        // Trimming
        if (field.getTrim() != null && field.getTrim()) {
            writer.write(" \"LTRIM(RTRIM(:COLUMN_NAME))\"".replace("COLUMN_NAME", field.getColumnName()));
        }
        
        // Case conversion
        if ("UPPER".equalsIgnoreCase(field.getCaseSensitive())) {
            writer.write(" \"UPPER(:COLUMN_NAME)\"".replace("COLUMN_NAME", field.getColumnName()));
        } else if ("LOWER".equalsIgnoreCase(field.getCaseSensitive())) {
            writer.write(" \"LOWER(:COLUMN_NAME)\"".replace("COLUMN_NAME", field.getColumnName()));
        }
        
        // SQL expression for transformation
        if (field.getExpression() != null) {
            writer.write(" \"" + field.getExpression() + "\"");
        }
        
        // Encryption function
        if (field.getEncrypted() != null && field.getEncrypted() && field.getEncryptionFunction() != null) {
            writer.write(" \"" + field.getEncryptionFunction() + "(:COLUMN_NAME)\"".replace("COLUMN_NAME", field.getColumnName()));
        }
    }
    
    /**
     * Write control file footer with additional SQL if needed.
     */
    private void writeControlFileFooter(BufferedWriter writer, SqlLoaderConfig config) throws IOException {
        writer.write(")\n\n");
        
        // Add pre-execution SQL
        if (config.getPreExecutionSql() != null && !config.getPreExecutionSql().isEmpty()) {
            writer.write("-- Pre-execution SQL statements\n");
            for (String sql : config.getPreExecutionSql()) {
                writer.write("-- " + sql + "\n");
            }
            writer.write("\n");
        }
        
        // Add post-execution SQL
        if (config.getPostExecutionSql() != null && !config.getPostExecutionSql().isEmpty()) {
            writer.write("-- Post-execution SQL statements\n");
            for (String sql : config.getPostExecutionSql()) {
                writer.write("-- " + sql + "\n");
            }
            writer.write("\n");
        }
        
        // Add metadata as comments
        if (config.getAdditionalMetadata() != null) {
            writer.write("-- Additional Metadata:\n");
            for (Map.Entry<String, Object> entry : config.getAdditionalMetadata().entrySet()) {
                writer.write("-- " + entry.getKey() + ": " + entry.getValue() + "\n");
            }
        }
    }
    
    /**
     * Get the path for the control file.
     */
    private Path getControlFilePath(SqlLoaderConfig config) {
        String fileName = config.getControlFileName();
        if (fileName == null) {
            fileName = config.getJobName() + "_" + System.currentTimeMillis() + CONTROL_FILE_EXTENSION;
        }
        
        // Use a configurable directory or default to temp
        String controlFileDir = System.getProperty("sqlloader.control.dir", System.getProperty("java.io.tmpdir"));
        return Paths.get(controlFileDir, fileName);
    }
    
    /**
     * Escape special characters in delimiters.
     */
    private String escapeDelimiter(String delimiter) {
        if (delimiter == null) return null;
        
        switch (delimiter) {
            case "\t":
                return "\\t";
            case "\n":
                return "\\n";
            case "\r":
                return "\\r";
            case "\"":
                return "\\\"";
            case "'":
                return "\\'";
            case "\\":
                return "\\\\";
            default:
                return delimiter;
        }
    }
    
    /**
     * Validate fixed-width file configuration.
     */
    private void validateFixedWidthConfig(SqlLoaderConfig config) {
        for (SqlLoaderConfig.FieldConfig field : config.getFields()) {
            if (field.getPosition() == null) {
                throw new IllegalArgumentException("Position must be specified for fixed-width field: " + field.getFieldName());
            }
            if (field.getLength() == null) {
                throw new IllegalArgumentException("Length must be specified for fixed-width field: " + field.getFieldName());
            }
        }
    }
    
    /**
     * Generate a template control file for reference.
     */
    public String generateTemplateControlFile(String targetTable, List<String> columnNames) {
        StringBuilder template = new StringBuilder();
        
        template.append("-- SQL*Loader Control File Template\n");
        template.append("-- Target table: ").append(targetTable).append("\n");
        template.append("-- Modify this template according to your data format\n\n");
        
        template.append("OPTIONS (\n");
        template.append("  DIRECT=TRUE,\n");
        template.append("  ERRORS=1000,\n");
        template.append("  SKIP=1,\n");
        template.append("  BINDSIZE=256000,\n");
        template.append("  READSIZE=1048576\n");
        template.append(")\n\n");
        
        template.append("LOAD DATA\n");
        template.append("CHARACTERSET UTF8\n");
        template.append("INFILE 'data_file.dat'\n");
        template.append("BADFILE 'data_file.bad'\n");
        template.append("DISCARDFILE 'data_file.dsc'\n\n");
        
        template.append("INSERT INTO TABLE ").append(targetTable).append("\n");
        template.append("FIELDS TERMINATED BY '|' OPTIONALLY ENCLOSED BY '\"' LTRIM\n");
        template.append("(\n");
        
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            template.append("  ").append(columnName).append(" CHAR");
            if (i < columnNames.size() - 1) {
                template.append(",\n");
            } else {
                template.append("\n");
            }
        }
        
        template.append(")\n");
        
        return template.toString();
    }
}