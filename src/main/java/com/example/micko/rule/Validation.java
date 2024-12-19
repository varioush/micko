package com.example.micko.rule;

import java.util.Map;
import lombok.Data;
@Data
public class Validation {
    private String type;              // "query", "regex", "length", "range"
    private String query;             // SQL query for DB validation
    private String field;             // Field to validate
    private String pattern;           // Regex pattern for validation
    private String expectedResult;    // Expected result for query validation
    private Integer minLength;        // Min field length for validation
    private Integer maxLength;        // Max field length for validation
    private Object minValue;          // Min value for range validation
    private Object maxValue;          // Max value for range validation
    private Map<String, Object> error; // Custom error message if validation fails

    // Getters and Setters
}
