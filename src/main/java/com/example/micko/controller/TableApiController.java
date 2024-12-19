package com.example.micko.controller;

import com.example.micko.data.DatabaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tables")
@Tag(name = "Tables API", description = "List all tables and their columns")
public class TableApiController {

    private static final Logger logger = LoggerFactory.getLogger(TableApiController.class);
    private final DatabaseService dbService;

    public TableApiController(DatabaseService dbService) {
        this.dbService = dbService;
    }

    /**
     * List All Tables
     */
    @Operation(summary = "List Tables", description = "Lists all available tables from the database.")
    @GetMapping("/list")
    public List<Map<String, Object>> listTables() {
        logger.info("Fetching list of all tables.");
        List<Map<String, Object>> tables = dbService.listAllTables();
        logger.info("Fetched {} tables from the database.", tables.size());
        return tables;
    }

    /**
     * List Columns of a Specific Table
     */
    @Operation(summary = "List Table Columns", description = "Lists all columns from a specific table.")
    @GetMapping("/columns/{tableName}")
    public List<Map<String, Object>> listTableColumns(@PathVariable String tableName) {
        logger.info("Fetching columns for table: {}", tableName);
        List<Map<String, Object>> columns = dbService.listTableColumns(tableName);
        if (columns.isEmpty()) {
            logger.warn("No columns found for table: {}", tableName);
        } else {
            logger.info("Fetched {} columns for table: {}", columns.size(), tableName);
        }
        return columns;
    }
}
