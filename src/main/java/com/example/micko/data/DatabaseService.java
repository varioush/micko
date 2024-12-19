package com.example.micko.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import jakarta.transaction.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@Transactional
@SuppressWarnings({"unchecked"})
public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    @PersistenceContext
    private EntityManager entityManager;
    
    private final DataSource dataSource;

    public DatabaseService(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    /**
     * List All Tables
     */
    public List<Map<String, Object>> listAllTables() {
        String query = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC';";
        return executeSelectQuery(query, Map.of());
    }

    /**
     * List Table Columns
     */
    public List<Map<String, Object>> listTableColumns(String tableName) {
        String query = "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = :tableName";
        return executeSelectQuery(query, Map.of("tableName", tableName.toUpperCase()));
    }

    /**
     * Execute DML Queries (INSERT, UPDATE, DELETE)
     */
    public Map<String, Object> executeDmlQuery(String query, Map<String, Object> params) {
        try {
            Query nativeQuery = entityManager.createNativeQuery(query);
            setQueryParameters(nativeQuery, query, params);
            int rowsAffected = nativeQuery.executeUpdate();
            logger.info("DML Query executed successfully: {} | Rows affected: {}", query, rowsAffected);
            return Map.of("rowsAffected", rowsAffected);
        } catch (IllegalArgumentException e) {
            logger.error("Parameter Error: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage());
        } catch (Exception e) {
            logger.error("DML Execution Error: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Execute SELECT Queries
     */
    public List<Map<String, Object>> executeSelectQuery(String query, Map<String, Object> params) {
        try {
            Query nativeQuery = entityManager.createNativeQuery(query, Tuple.class);
            setQueryParameters(nativeQuery, query, params);
            List<Tuple> results = nativeQuery.getResultList();
            logger.info("Select Query executed successfully: {} | Rows fetched: {}", query, results.size());
            return mapTupleResults(results);
        } catch (IllegalArgumentException e) {
            logger.error("Parameter Error: {}", e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Query Execution Error: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Set Query Parameters
     */
    private void setQueryParameters(Query query, String sql, Map<String, Object> params) {
        List<String> expectedParams = extractQueryParameters(sql);
        for (String param : expectedParams) {
            if (!params.containsKey(param)) {
                throw new IllegalArgumentException("Missing required parameter: " + param);
            }
        }
        for (Map.Entry<String, Object> param : params.entrySet()) {
            if (expectedParams.contains(param.getKey())) {
                query.setParameter(param.getKey(), param.getValue());
            }
        }
    }

    /**
     * Extract Named Parameters from Query
     */
    private List<String> extractQueryParameters(String query) {
        return query.lines()
                .flatMap(line -> java.util.regex.Pattern.compile(":(\\w+)").matcher(line).results())
                .map(match -> match.group(1))
                .distinct()
                .toList();
    }

    /**
     * Map Tuple Results to Tabular Format
     */
    private List<Map<String, Object>> mapTupleResults(List<Tuple> results) {
        return results.stream().map(tuple -> {
            Map<String, Object> row = new HashMap<>();
            for (TupleElement<?> element : tuple.getElements()) {
                row.put(element.getAlias(), tuple.get(element));
            }
            return row;
        }).collect(Collectors.toList());
    }

 
    
    
    /**
     * Execute Custom SQL Query Supporting DML (Insert, Update, Delete) and Select
     * @param query Custom SQL query
     * @return Query result or number of affected rows
     */
    public Object executeCustomQuery(String query) {
        if (isDmlQuery(query)) {
            int rowsAffected = entityManager.createNativeQuery(query).executeUpdate();
            return Map.of("rowsAffected", rowsAffected);
        } else {
            Query nativeQuery = entityManager.createNativeQuery(query, Tuple.class);
            List<Tuple> results = nativeQuery.getResultList();
            return mapTupleResults(results);
        }
    }

    /**
     * Check if Query is DML (Insert, Update, Delete)
     * @param query SQL query string
     * @return true if DML, false otherwise
     */
    private boolean isDmlQuery(String query) {
        String queryType = query.trim().split("\\s+")[0].toUpperCase();
        return Set.of("INSERT", "UPDATE", "DELETE").contains(queryType);
    }
}
