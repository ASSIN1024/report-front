package com.report.util;

import com.report.entity.ReportConfig;
import com.report.entity.dto.FieldMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ExcelUtilTest {

    @TempDir
    File tempDir;

    private File createTestExcelFile() throws IOException {
        File excelFile = new File(tempDir, "test_data.xlsx");
        org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("TestSheet");

        org.apache.poi.xssf.usermodel.XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("order_id");
        headerRow.createCell(1).setCellValue("product_name");
        headerRow.createCell(2).setCellValue("quantity");
        headerRow.createCell(3).setCellValue("amount");
        headerRow.createCell(4).setCellValue("order_date");

        org.apache.poi.xssf.usermodel.XSSFRow dataRow1 = sheet.createRow(1);
        dataRow1.createCell(0).setCellValue("ORD001");
        dataRow1.createCell(1).setCellValue("Product A");
        dataRow1.createCell(2).setCellValue(100);
        dataRow1.createCell(3).setCellValue(1999.99);
        dataRow1.createCell(4).setCellValue("2026-03-29");

        org.apache.poi.xssf.usermodel.XSSFRow dataRow2 = sheet.createRow(2);
        dataRow2.createCell(0).setCellValue("ORD002");
        dataRow2.createCell(1).setCellValue("Product B");
        dataRow2.createCell(2).setCellValue(50);
        dataRow2.createCell(3).setCellValue(999.50);
        dataRow2.createCell(4).setCellValue("2026-03-30");

        FileOutputStream fos = new FileOutputStream(excelFile);
        workbook.write(fos);
        workbook.close();
        fos.close();

        return excelFile;
    }

    private ReportConfig createTestReportConfig() {
        ReportConfig config = new ReportConfig();
        config.setSheetIndex(0);
        config.setHeaderRow(0);
        config.setDataStartRow(1);
        return config;
    }

    private List<FieldMapping> createTestColumnMappings() {
        List<FieldMapping> mappings = new ArrayList<>();

        FieldMapping m1 = new FieldMapping();
        m1.setExcelColumn("A");
        m1.setFieldName("order_id");
        m1.setFieldType("STRING");
        mappings.add(m1);

        FieldMapping m2 = new FieldMapping();
        m2.setExcelColumn("B");
        m2.setFieldName("product_name");
        m2.setFieldType("STRING");
        mappings.add(m2);

        FieldMapping m3 = new FieldMapping();
        m3.setExcelColumn("C");
        m3.setFieldName("quantity");
        m3.setFieldType("INTEGER");
        mappings.add(m3);

        FieldMapping m4 = new FieldMapping();
        m4.setExcelColumn("D");
        m4.setFieldName("amount");
        m4.setFieldType("DECIMAL");
        mappings.add(m4);

        FieldMapping m5 = new FieldMapping();
        m5.setExcelColumn("E");
        m5.setFieldName("order_date");
        m5.setFieldType("DATE");
        m5.setDateFormat("yyyy-MM-dd");
        mappings.add(m5);

        return mappings;
    }

    @Test
    public void testReadExcel() throws Exception {
        File excelFile = createTestExcelFile();
        ReportConfig config = createTestReportConfig();
        List<FieldMapping> columnMappings = createTestColumnMappings();

        List<Map<String, Object>> dataList = ExcelUtil.readExcel(excelFile, config, columnMappings);

        assertNotNull(dataList);
        assertEquals(2, dataList.size());

        Map<String, Object> firstRow = dataList.get(0);
        assertEquals("ORD001", firstRow.get("order_id"));
        assertEquals("Product A", firstRow.get("product_name"));
        assertEquals(100, ((Number) firstRow.get("quantity")).intValue());
    }

    @Test
    public void testParseColumnMapping() {
        String json = "[{\"excelColumn\":\"A\",\"fieldName\":\"order_id\",\"fieldType\":\"STRING\"}," +
                "{\"excelColumn\":\"B\",\"fieldName\":\"product_name\",\"fieldType\":\"STRING\"}]";

        List<FieldMapping> mappings = ExcelUtil.parseColumnMapping(json);

        assertNotNull(mappings);
        assertEquals(2, mappings.size());
        assertEquals("A", mappings.get(0).getExcelColumn());
        assertEquals("order_id", mappings.get(0).getFieldName());
        assertEquals("STRING", mappings.get(0).getFieldType());
    }

    @Test
    public void testParseColumnMappingWithEmptyString() {
        List<FieldMapping> mappings = ExcelUtil.parseColumnMapping("");
        assertNotNull(mappings);
        assertTrue(mappings.isEmpty());
    }
}
