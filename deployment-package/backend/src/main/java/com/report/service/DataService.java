package com.report.service;

import com.report.entity.dto.DataQueryDTO;

import java.util.List;
import java.util.Map;

public interface DataService {

    Map<String, Object> queryPage(DataQueryDTO queryDTO);

    List<Map<String, Object>> queryList(DataQueryDTO queryDTO);

    List<String> getTableColumns(String tableName);

    List<String> getOutputTables();
}
