package com.report.service.impl;

import com.report.service.OdsBackupService;
import com.report.service.TransformResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OdsBackupServiceImpl implements OdsBackupService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void backup(TransformResult result, String sourceFileName) {
        if (result.getTableName() == null || result.getTableName().isEmpty()) {
            log.warn("ODS table name not configured, skipping backup");
            return;
        }

        try {
            String sql = "INSERT INTO " + result.getTableName() + " (source_file, pt_dt, create_time) VALUES (?, ?, NOW())";
            jdbcTemplate.update(sql, sourceFileName, result.getPtDt());
            log.info("ODS backup completed: file={}, table={}", sourceFileName, result.getTableName());
        } catch (Exception e) {
            log.error("ODS backup failed: file={}, table={}", sourceFileName, result.getTableName(), e);
            throw e;
        }
    }
}
