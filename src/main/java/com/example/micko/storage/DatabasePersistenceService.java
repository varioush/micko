package com.example.micko.storage;

import com.example.micko.data.DatabaseService;
import com.example.micko.parser.YamlParser;
import com.example.micko.rule.RuleCacheManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DatabasePersistenceService implements DisposableBean {

    private final DatabaseService dbService;
    private final YamlParser yamlParser;
    private final StorageService storageService;
    private final RuleCacheManager ruleCacheManager;

    public DatabasePersistenceService(DatabaseService dbService, StorageService storageService, RuleCacheManager ruleCacheManager) {
        this.dbService = dbService;
        this.storageService = storageService;
        this.yamlParser = new YamlParser();
        this.ruleCacheManager = ruleCacheManager;
    }

    /**
     * Normalize Columns for Correct YAML Structure
     */
    private List<Map<String, String>> normalizeColumns(List<Map<String, Object>> columns) {
        return columns.stream()
            .map(column -> Map.of(
                "name", column.get("COLUMN_NAME").toString().toLowerCase(),
                "type", mapColumnType(column.get("DATA_TYPE").toString())
            ))
            .collect(Collectors.toList());
    }

    /**
     * Normalize Data to Lowercase Keys
     */
    private List<Map<String, Object>> normalizeData(List<Map<String, Object>> data) {
        return data.stream()
            .map(row -> row.entrySet().stream()
                .collect(Collectors.toMap(
                    entry -> entry.getKey().toLowerCase(),
                    Map.Entry::getValue,
                    (oldValue, newValue) -> newValue,
                    LinkedHashMap::new
                ))
            ).collect(Collectors.toList());
    }

    /**
     * Map Database Column Type to Expected YAML Format
     */
    private String mapColumnType(String dbType) {
        return switch (dbType.toUpperCase()) {
            case "CHARACTER VARYING" -> "string";
            case "BOOLEAN" -> "boolean";
            default -> dbType.toLowerCase();
        };
    }

    /**
     * Persist H2 Database and Rules on Application Exit
     */
    @Override
    public void destroy() throws Exception {
        List<Map<String, Object>> tables = dbService.listAllTables();

        for (Map<String, Object> table : tables) {
            String tableName = table.get("TABLE_NAME").toString().toLowerCase();

            // Fetch Table Metadata and Data
            List<Map<String, String>> columns = normalizeColumns(dbService.listTableColumns(tableName));
            List<Map<String, Object>> data = normalizeData(dbService.executeSelectQuery("SELECT * FROM " + tableName, Map.of()));

            // Fetch Associated Rules from RuleCacheManager
            Map<String, Map<String, Object>> rules = ruleCacheManager.getAllRules(tableName);

            // Format Data into Ordered YAML Structure
            Map<String, Object> tableStructure = new LinkedHashMap<>();
            tableStructure.put("name", tableName);
            tableStructure.put("columns", columns);
            tableStructure.put("data", data);
            tableStructure.put("rules", rules.values());

            Map<String, Object> yamlStructure = Map.of("tables", List.of(tableStructure));

            // Convert Data to YAML Format
            String yamlOutput = yamlParser.writeYaml(yamlStructure);

            // Save YAML File to S3 or Local
            storageService.saveYamlFile(tableName + ".yaml", yamlOutput);

            System.out.println("Persisted Table [" + tableName + "] and Rules to Storage.");
        }
    }
}
