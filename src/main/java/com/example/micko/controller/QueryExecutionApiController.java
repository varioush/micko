package com.example.micko.controller;

import com.example.micko.data.DatabaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
@Tag(name = "Query API", description = "Execute custom database queries")
public class QueryExecutionApiController {

    private static final Logger logger = LoggerFactory.getLogger(QueryExecutionApiController.class);
    private final DatabaseService dbService;

    public QueryExecutionApiController(DatabaseService dbService) {
        this.dbService = dbService;
    }

    /**
     * Execute Custom Query
     */
    @Operation(summary = "Execute Custom Query", description = "Executes a custom SQL query and returns the result.")
    @PostMapping("/execute")
    public Object executeQuery(@RequestBody String query) {
        logger.info("Received query execution request: {}", query);
        Object result = dbService.executeCustomQuery(query);
        logger.info("Query execution result: {}", result);
        return result;
    }
}
