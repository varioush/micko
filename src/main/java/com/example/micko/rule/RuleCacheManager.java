package com.example.micko.rule;

import com.example.micko.data.DatabaseManager;
import com.example.micko.parser.YamlParser;
import com.example.micko.storage.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@SuppressWarnings({"unchecked"})
public class RuleCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(RuleCacheManager.class);

    private final StorageService storageService;
    private final YamlParser yamlParser;
    private final DatabaseManager dbManager;
    private final DataSource dataSource;
    private final JsonSchema schema;

    private final Map<String, Map<String, Object>> tableCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Map<String, Object>>> ruleCache = new ConcurrentHashMap<>();

    public RuleCacheManager(StorageService storageService, DataSource dataSource) {
        this.storageService = storageService;
        this.yamlParser = new YamlParser();
        this.dbManager = new DatabaseManager();
        this.dataSource = dataSource;
        this.schema = loadSchema();
    }

    private JsonSchema loadSchema() {
        try (InputStream schemaStream = new ClassPathResource("schema.json").getInputStream()) {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
            return factory.getSchema(schemaStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON Schema.", e);
        }
    }

    private void validateSchema(JsonNode yamlData) {
        Set<ValidationMessage> errors = schema.validate(yamlData);
        if (!errors.isEmpty()) {
            throw new RuntimeException("Schema validation failed: " + errors);
        }
    }

    public void loadRules() {
        try (Connection connection = dataSource.getConnection()) {
            List<String> fileKeys = storageService.listYamlFiles("local-storage");

            for (String fileKey : fileKeys) {
            	 String resourcePath = "local-storage/" + fileKey;
                 InputStream yamlFile = getClass().getClassLoader().getResourceAsStream(resourcePath);

                 if (yamlFile == null) {
                     logger.error("File not found in resources: {}", resourcePath);
                     continue;  // Skip the missing file
                 }
                 long fileTimestamp = System.currentTimeMillis();  // Use current time if no last modified time
                 logger.info("Loading file: {} with timestamp: {}", fileKey, fileTimestamp);

                List<Map<String, Object>> parsedData = yamlParser.parseYamlFiles(List.of(yamlFile));

                for (Map<String, Object> fileData : parsedData) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonData = mapper.valueToTree(fileData);
                    validateSchema(jsonData);

                    if (fileData.containsKey("tables")) {
                        List<Map<String, Object>> tables = (List<Map<String, Object>>) fileData.get("tables");
                        for (Map<String, Object> table : tables) {
                            String tableName = (String) table.get("name");
                            Map<String, Object> cachedTable = tableCache.get(tableName);

                            if (cachedTable == null) {
                                logger.info("Creating new table: {}", tableName);
                                dbManager.createAndLoadTables(connection, List.of(table));
                                table.put("timestamp", fileTimestamp);
                                tableCache.put(tableName, table);
                            } else {
                                long cachedTimestamp = (long) cachedTable.getOrDefault("timestamp", 0L);
                                if (fileTimestamp > cachedTimestamp) {
                                    logger.info("Table {} has changed. Dropping and recreating.", tableName);
                                    dbManager.dropTableIfExists(connection, tableName);
                                    dbManager.createAndLoadTables(connection, List.of(table));
                                    table.put("timestamp", fileTimestamp);
                                    tableCache.put(tableName, table);
                                } else {
                                    logger.info("Table {} is up-to-date.", tableName);
                                }
                            }

                            if (table.containsKey("rules")) {
                                List<Map<String, Object>> rules = (List<Map<String, Object>>) table.get("rules");
                                rules.forEach(rule -> {
                                    String action = (String) rule.get("action");
                                    ruleCache.computeIfAbsent(tableName, k -> new ConcurrentHashMap<>()).put(action, rule);
                                });
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load YAML files: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load YAML files.", e);
        }
    }

    public Map<String, Map<String, Object>> getAllRules(String tableName) {
        return ruleCache.getOrDefault(tableName, Map.of());
    }

    public Map<String, Map<String, Map<String, Object>>> getAllRules() {
        return ruleCache;
    }

    public Map<String, Object> getRule(String tableName, String action) {
        return ruleCache.getOrDefault(tableName, Map.of()).get(action);
    }
}
