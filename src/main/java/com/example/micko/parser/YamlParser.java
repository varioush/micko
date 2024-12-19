package com.example.micko.parser;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
public class YamlParser {
	
	private final YAMLMapper yamlMapper;

    public YamlParser() {
        this.yamlMapper = new YAMLMapper(new YAMLFactory());
    }
 
    
    /**
     * Parse Multiple YAML Files into List of Maps
     */
    public List<Map<String, Object>> parseYamlFiles(List<java.io.InputStream> yamlFiles) {
        try {
            return yamlFiles.stream()
                    .map(this::parseYaml)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse YAML files", e);
        }
    }
    
    /**
     * Convert Map to YAML String
     */
    public String writeYaml(Map<String, Object> yamlStructure) {
        try (StringWriter writer = new StringWriter()) {
            yamlMapper.writeValue(writer, yamlStructure);
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write YAML content", e);
        }
    }
    
    /**
     * Parse Single YAML File
     */
    @SuppressWarnings("unchecked")
	public Map<String, Object> parseYaml(java.io.InputStream yamlFile) {
        try {
            return yamlMapper.readValue(yamlFile, Map.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse YAML file", e);
        }
    }

}
