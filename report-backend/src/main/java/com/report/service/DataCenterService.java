package com.report.service;

import com.report.entity.TableLayerMapping;
import java.util.List;
import java.util.Map;

public interface DataCenterService {
    List<TableLayerMapping> listTables(String tableLayer, String sourceType, String businessDomain);

    TableLayerMapping getTableByName(String tableName);

    boolean updateTableMapping(TableLayerMapping mapping);

    List<TableLayerMapping> listUntaggedTables();

    List<String> scanNewTables();

    Map<String, Object> getTableData(String tableName, Integer pageNum, Integer pageSize, String condition);

    List<Map<String, Object>> getTableColumns(String tableName);
}