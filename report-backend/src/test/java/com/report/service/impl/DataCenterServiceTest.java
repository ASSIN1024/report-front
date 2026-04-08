package com.report.service.impl;

import com.report.entity.TableLayerMapping;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DataCenterServiceTest {

    @Autowired
    private DataCenterServiceImpl dataCenterService;

    @Test
    public void testScanNewTables() {
        List<String> newTables = dataCenterService.scanNewTables();
        assertNotNull(newTables);
    }

    @Test
    public void testGetTableData_invalidTableName() {
        try {
            dataCenterService.getTableData("'; DROP TABLE users;--", 1, 20, null);
            fail("Should throw exception for invalid table name");
        } catch (IllegalArgumentException e) {
            assertEquals("无效的表名", e.getMessage());
        }
    }

    @Test
    public void testGetTableData_invalidCondition() {
        try {
            dataCenterService.getTableData("ftp_config", 1, 20, "1=1; DELETE FROM ftp_config");
            fail("Should throw exception for invalid condition");
        } catch (IllegalArgumentException e) {
            assertEquals("无效的筛选条件", e.getMessage());
        }
    }

    @Test
    public void testListUntaggedTables() {
        List<TableLayerMapping> untaggedTables = dataCenterService.listUntaggedTables();
        assertNotNull(untaggedTables);
    }

    @Test
    public void testGetTableColumns_invalidTableName() {
        try {
            dataCenterService.getTableColumns("invalid--table");
            fail("Should throw exception for invalid table name");
        } catch (IllegalArgumentException e) {
            assertEquals("无效的表名", e.getMessage());
        }
    }
}
