package com.example.micko.controller;

import com.example.micko.rule.RuleCacheManager;
import com.example.micko.rule.RuleExecutor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rules")
@Tag(name = "Rules API", description = "Manage and Execute YAML Rules")
public class RulesApiController {

    private static final Logger logger = LoggerFactory.getLogger(RulesApiController.class);
    private final RuleCacheManager cacheManager;
    private final RuleExecutor ruleExecutor;

    public RulesApiController(RuleCacheManager cacheManager, RuleExecutor ruleExecutor) {
        this.cacheManager = cacheManager;
        this.ruleExecutor = ruleExecutor;
    }

    /**
     * List All Cached Rules
     */
    @Operation(summary = "List Rules", description = "Retrieve all cached YAML rules")
    @GetMapping("/list")
    public Map<String, Map<String, Map<String, Object>>> listAllRules() {
        logger.info("Listing all cached rules.");
        return cacheManager.getAllRules();
    }

    /**
     * List Rules for a Specific Table
     */
    @Operation(summary = "List Table Rules", description = "Retrieve rules for a specific table")
    @GetMapping("/list/{tableName}")
    public Map<String, Map<String, Object>> listRulesByTable(@PathVariable String tableName) {
        logger.info("Listing rules for table: {}", tableName);
        Map<String, Map<String, Object>> rules = cacheManager.getAllRules(tableName);
        if (rules.isEmpty()) {
            logger.warn("Table not found or no rules defined for: {}", tableName);
            throw new RuntimeException("Table not found or no rules defined.");
        }
        return rules;
    }

    /**
     * Execute a Rule by Table Name and Action
     */
    @Operation(summary = "Execute Rule", description = "Execute a specific rule from the cache")
    @PostMapping("/{tableName}/{action}")
    public ResponseEntity<Object> executeRule(
            @PathVariable String tableName,
            @PathVariable String action,
            @RequestBody Map<String, Object> inputs) {

        logger.info("Executing rule for table: {}, action: {}, inputs: {}", tableName, action, inputs);
        Map<String, Object> rule = cacheManager.getRule(tableName, action);

        if (rule == null) {
            logger.error("Rule not found for table: {}, action: {}", tableName, action);
            return ResponseEntity.status(404).body("Rule not found");
        }

        ResponseEntity<Object> response = ruleExecutor.execute(rule, inputs);
        logger.info("Execution completed for table: {}, action: {}", tableName, action);
        return response;
    }
}
