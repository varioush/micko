package com.example.micko.rule;

import com.example.micko.data.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked"})
@Service
public class RuleExecutor {

    private static final Logger logger = LoggerFactory.getLogger(RuleExecutor.class);

    private final DatabaseService dbService;

    public RuleExecutor(DatabaseService dbService) {
        this.dbService = dbService;
    }

    /**
     * Execute a Rule from YAML
     */
    public ResponseEntity<Object> execute(Map<String, Object> rule, Map<String, Object> inputs) {
        logger.info("Executing rule with inputs: {}", inputs);

        try {
            // Step 1: Validate Rule Inputs
            if (rule.containsKey("validations")) {
                List<Map<String, Object>> validations = (List<Map<String, Object>>) rule.get("validations");
                for (Map<String, Object> validationData : validations) {
                    Validation validation = mapToValidation(validationData);
                    logger.debug("Performing validation: {}", validation);
                    if (!performValidation(validation, inputs)) {
                        logger.warn("Validation failed: {}", validation);
                        return handleValidationError(validation);
                    }
                }
            }

            // Step 2: Execute the Rule Action
            if (rule.containsKey("executionQuery")) {
                Map<String, Object> executionQuery = (Map<String, Object>) rule.get("executionQuery");

                String query = (String) executionQuery.get("query");
                String successMessage = (String) executionQuery.getOrDefault("successMessage", "Operation successful.");
                String errorMessage = (String) executionQuery.getOrDefault("errorMessage", "Operation failed.");

                logger.info("Executing query: {}", query);
                boolean isDml = isDmlQuery(query);
                Object result = isDml
                    ? dbService.executeDmlQuery(query, inputs)
                    : dbService.executeSelectQuery(query, inputs);

                if (result instanceof Map<?, ?> resultMap && resultMap.containsKey("error")) {
                    logger.error("Query execution failed: {}", resultMap);
                    return ResponseEntity.status(500).body(Map.of("message", errorMessage, "details", resultMap));
                }

                logger.info("Query executed successfully: {}", result);
                return ResponseEntity.ok(Map.of("message", successMessage, "data", result));
            }

            logger.warn("Action not implemented for rule.");
            return ResponseEntity.status(501).body("Action not implemented.");

        } catch (Exception e) {
            logger.error("Execution error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Internal server error.");
        }
    }

    private boolean performValidation(Validation validation, Map<String, Object> inputs) {
        try {
            switch (validation.getType()) {
                case "query" -> {
                    logger.debug("Validating query: {}", validation.getQuery());
                    List<Map<String, Object>> results = dbService.executeSelectQuery(validation.getQuery(), inputs);
                    if (results.isEmpty()) {
                        logger.warn("Query validation failed: no results found.");
                        return false;
                    }
                    return results.get(0).values().stream()
                        .map(Object::toString)
                        .anyMatch(value -> value.equalsIgnoreCase(validation.getExpectedResult()));
                }
                case "regex" -> {
                    String fieldValue = (String) inputs.get(validation.getField());
                    logger.debug("Validating regex for field: {}, value: {}", validation.getField(), fieldValue);
                    return fieldValue.matches(validation.getPattern());
                }
                case "length" -> {
                    String fieldValue = (String) inputs.get(validation.getField());
                    logger.debug("Validating length for field: {}, value: {}", validation.getField(), fieldValue);
                    return fieldValue.length() >= validation.getMinLength() &&
                           fieldValue.length() <= validation.getMaxLength();
                }
                case "range" -> {
                    Comparable<Object> value = (Comparable<Object>) inputs.get(validation.getField());
                    logger.debug("Validating range for field: {}, value: {}", validation.getField(), value);
                    return value.compareTo(validation.getMinValue()) >= 0 &&
                           value.compareTo(validation.getMaxValue()) <= 0;
                }
                default -> {
                    logger.warn("Unsupported validation type: {}", validation.getType());
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Validation error: {}", e.getMessage(), e);
            return false;
        }
    }

    private ResponseEntity<Object> handleValidationError(Validation validation) {
        if (validation.getError() != null) {
            int statusCode = (int) validation.getError().getOrDefault("statusCode", 400);
            String message = (String) validation.getError().getOrDefault("message", "Validation failed.");
            logger.warn("Validation error: {}, status: {}", message, statusCode);
            return ResponseEntity.status(statusCode).body(message);
        }
        logger.warn("Validation failed with no error message defined.");
        return ResponseEntity.badRequest().body("Validation failed. Check your input.");
    }

    private boolean isDmlQuery(String query) {
        String queryType = query.trim().split("\\s+")[0].toUpperCase();
        logger.debug("Determined query type: {}", queryType);
        return Set.of("INSERT", "UPDATE", "DELETE").contains(queryType);
    }

    private Validation mapToValidation(Map<String, Object> validationData) {
        Validation validation = new Validation();
        validation.setType((String) validationData.get("type"));
        validation.setQuery((String) validationData.get("query"));
        validation.setField((String) validationData.get("field"));
        validation.setPattern((String) validationData.get("pattern"));
        validation.setExpectedResult((String) validationData.get("expectedResult"));
        validation.setMinLength((Integer) validationData.get("minLength"));
        validation.setMaxLength((Integer) validationData.get("maxLength"));
        validation.setMinValue(validationData.get("minValue"));
        validation.setMaxValue(validationData.get("maxValue"));
        validation.setError((Map<String, Object>) validationData.get("error"));
        logger.debug("Mapped validation: {}", validation);
        return validation;
    }
}
