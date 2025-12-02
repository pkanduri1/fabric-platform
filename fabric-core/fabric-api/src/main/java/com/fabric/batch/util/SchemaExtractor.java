package com.fabric.batch.util;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.io.*;

/**
 * Utility to extract current Oracle database schema metadata
 */
@Component
public class SchemaExtractor {

    private final JdbcTemplate jdbcTemplate;

    public SchemaExtractor(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void extractSchemaToFile(String outputPath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println("=== CM3INT SCHEMA SNAPSHOT ===");
            writer.println("Generated: " + new java.util.Date());
            writer.println();

            // Extract tables
            writer.println("=== TABLES ===");
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                "SELECT table_name, num_rows, last_analyzed FROM user_tables ORDER BY table_name"
            );
            for (Map<String, Object> table : tables) {
                writer.println(String.format("%-40s Rows: %-10s Analyzed: %s",
                    table.get("TABLE_NAME"),
                    table.get("NUM_ROWS"),
                    table.get("LAST_ANALYZED")
                ));
            }
            writer.println();

            // Extract columns
            writer.println("=== COLUMNS ===");
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT table_name, column_name, data_type, data_length, data_precision, " +
                "data_scale, nullable, column_id FROM user_tab_columns ORDER BY table_name, column_id"
            );

            String currentTable = "";
            for (Map<String, Object> col : columns) {
                String tableName = (String) col.get("TABLE_NAME");
                if (!tableName.equals(currentTable)) {
                    writer.println("\n" + tableName + ":");
                    currentTable = tableName;
                }

                String dataType = (String) col.get("DATA_TYPE");
                String typeInfo = dataType;
                if ("VARCHAR2".equals(dataType) || "CHAR".equals(dataType)) {
                    typeInfo += "(" + col.get("DATA_LENGTH") + ")";
                } else if ("NUMBER".equals(dataType) && col.get("DATA_PRECISION") != null) {
                    typeInfo += "(" + col.get("DATA_PRECISION") + "," + col.get("DATA_SCALE") + ")";
                }

                writer.println(String.format("  %-40s %-20s %s",
                    col.get("COLUMN_NAME"),
                    typeInfo,
                    "Y".equals(col.get("NULLABLE")) ? "NULL" : "NOT NULL"
                ));
            }
            writer.println();

            // Extract constraints
            writer.println("=== CONSTRAINTS ===");
            List<Map<String, Object>> constraints = jdbcTemplate.queryForList(
                "SELECT constraint_name, constraint_type, table_name, r_constraint_name " +
                "FROM user_constraints ORDER BY table_name, constraint_type"
            );

            currentTable = "";
            for (Map<String, Object> con : constraints) {
                String tableName = (String) con.get("TABLE_NAME");
                if (!tableName.equals(currentTable)) {
                    writer.println("\n" + tableName + ":");
                    currentTable = tableName;
                }

                String type = (String) con.get("CONSTRAINT_TYPE");
                String typeDesc = switch(type) {
                    case "P" -> "PRIMARY KEY";
                    case "U" -> "UNIQUE";
                    case "R" -> "FOREIGN KEY";
                    case "C" -> "CHECK";
                    default -> type;
                };

                writer.println(String.format("  %-40s %-15s %s",
                    con.get("CONSTRAINT_NAME"),
                    typeDesc,
                    con.get("R_CONSTRAINT_NAME") != null ? "-> " + con.get("R_CONSTRAINT_NAME") : ""
                ));
            }
            writer.println();

            // Extract indexes
            writer.println("=== INDEXES ===");
            List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
                "SELECT index_name, table_name, uniqueness, index_type " +
                "FROM user_indexes ORDER BY table_name, index_name"
            );

            currentTable = "";
            for (Map<String, Object> idx : indexes) {
                String tableName = (String) idx.get("TABLE_NAME");
                if (!tableName.equals(currentTable)) {
                    writer.println("\n" + tableName + ":");
                    currentTable = tableName;
                }

                writer.println(String.format("  %-40s %-10s %s",
                    idx.get("INDEX_NAME"),
                    idx.get("UNIQUENESS"),
                    idx.get("INDEX_TYPE")
                ));
            }

            writer.flush();
        }
    }
}
