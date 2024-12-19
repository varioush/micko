package com.example.micko.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"unchecked"})
public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    /**
     * Create Tables and Load Data into the Database
     */
    public void createAndLoadTables(Connection connection, List<Map<String, Object>> tables) throws Exception {
        for (Map<String, Object> table : tables) {
            try {
                logger.info("Processing table: {}", table.get("name"));
                createTable(connection, table);
                insertData(connection, table);
            } catch (Exception e) {
                logger.error("Error creating or loading table {}: {}", table.get("name"), e.getMessage(), e);
                throw e;
            }
        }
    }

    /**
     * Create Table If Not Exists with All Data Types
     */
    private void createTable(Connection connection, Map<String, Object> table) throws Exception {
        String tableName = table.get("name").toString();
        List<Map<String, String>> columns = (List<Map<String, String>>) table.get("columns");

        StringBuilder createQuery = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (");

        for (Map<String, String> column : columns) {
            String columnType = resolveDataType(column.get("type"));
            createQuery.append(column.get("name")).append(" ").append(columnType).append(",");
        }

        createQuery.deleteCharAt(createQuery.length() - 1).append(");");

        try (Statement statement = connection.createStatement()) {
            statement.execute(createQuery.toString());
            logger.info("Table {} created or already exists.", tableName);
        } catch (Exception e) {
            logger.error("Error creating table {}: {}", tableName, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Drop Table If Exists
     */
    public void dropTableIfExists(Connection connection, String tableName) throws Exception {
        String dropQuery = "DROP TABLE IF EXISTS " + tableName;
        try (Statement statement = connection.createStatement()) {
            statement.execute(dropQuery);
            logger.info("Table {} dropped successfully.", tableName);
        } catch (Exception e) {
            logger.error("Error dropping table {}: {}", tableName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Insert Data into the Table
     */
    private void insertData(Connection connection, Map<String, Object> table) throws Exception {
        String tableName = table.get("name").toString();
        List<Map<String, Object>> data = (List<Map<String, Object>>) table.get("data");

        if (data.isEmpty()) {
            logger.warn("No data provided for table {}.", tableName);
            return;
        }

        Map<String, Object> firstRow = data.get(0);
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();

        for (String column : firstRow.keySet()) {
            columns.append(column).append(",");
            placeholders.append("?,");
        }

        columns.deleteCharAt(columns.length() - 1);
        placeholders.deleteCharAt(placeholders.length() - 1);

        String insertQuery = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
        logger.info("Insert query prepared: {}", insertQuery);

        try (PreparedStatement ps = connection.prepareStatement(insertQuery)) {
            for (Map<String, Object> row : data) {
                int index = 1;
                for (String column : firstRow.keySet()) {
                    ps.setObject(index++, row.get(column));
                }
                ps.addBatch();
                logger.debug("Prepared batch for row: {}", row);
            }
            int[] result = ps.executeBatch();
            logger.info("Inserted {} rows into {}.", result.length, tableName);
        } catch (Exception e) {
            logger.error("Error inserting data into table {}: {}", tableName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Resolve Data Type for Table Columns
     */
    private String resolveDataType(String type) {
        try {
            String resolvedType = switch (type.toLowerCase()) {
                case "string" -> "VARCHAR(255)";
                case "int", "integer" -> "INT";
                case "long" -> "BIGINT";
                case "float" -> "FLOAT";
                case "double" -> "DOUBLE";
                case "boolean" -> "BOOLEAN";
                case "date" -> "DATE";
                case "timestamp" -> "TIMESTAMP";
                default -> throw new IllegalArgumentException("Unsupported data type: " + type);
            };
            logger.info("Resolved data type {} to {}", type, resolvedType);
            return resolvedType;
        } catch (Exception e) {
            logger.error("Error resolving data type {}: {}", type, e.getMessage(), e);
            throw e;
        }
    }
}
