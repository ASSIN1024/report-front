package com.report.service;

import com.report.entity.dto.FieldMapping;
import java.util.List;

public interface TableCreatorService {
    
    boolean checkTableExists(String tableName);
    
    void createTable(String tableName, List<FieldMapping> columnMappings);
    
    void ensureTableExists(String tableName, List<FieldMapping> columnMappings);
    
    void addMissingColumns(String tableName, List<FieldMapping> columnMappings);
}
