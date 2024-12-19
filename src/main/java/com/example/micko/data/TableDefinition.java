package com.example.micko.data;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TableDefinition {
    private String name;
    private List<ColumnDefinition> columns;
    private List<Map<String, Object>> data;

    // Getters and Setters
}
