package com.report.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestAlignment {

    public static void main(String[] args) throws Exception {
        String templatePath = "src/main/resources/informationTemplate.xlsx";
        String outputPath = "test_alignment_output.xlsx";

        System.out.println("=== 1. 检查模板原始样式 ===");
        try (FileInputStream fis = new FileInputStream(templatePath);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheetAt(0);

            Row templateRow = sheet.getRow(3);
            if (templateRow == null) {
                templateRow = sheet.getRow(2);
            }
            if (templateRow == null) {
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row r = sheet.getRow(i);
                    if (r != null && r.getRowNum() > 1) {
                        templateRow = r;
                        break;
                    }
                }
            }

            System.out.println("模板行 " + templateRow.getRowNum() + " 的单元格样式:");
            for (int i = 0; i < 9; i++) {
                Cell cell = templateRow.getCell(i);
                if (cell != null) {
                    CellStyle style = cell.getCellStyle();
                    System.out.println("  列" + i + ": 对齐=" + style.getAlignment() +
                        ", 垂直=" + style.getVerticalAlignment());
                }
            }

            System.out.println("\n=== 2. 使用 ConfigExcelWriter 生成文件 ===");
            List<Map<String, Object>> configRecords = new ArrayList<>();
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("standard_file", "test_file.xlsx");
            record.put("table_type", "hive");
            record.put("db_name", "test_db");
            record.put("table_name", "test_table");
            record.put("is_overseas", 0);
            record.put("field_type_json", "{\"id\":\"int\",\"name\":\"string\"}");
            record.put("load_mode", "partitioned-append");
            record.put("partition_info", "pt_dt='2026-05-09'");
            configRecords.add(record);

            ConfigExcelWriter.write(outputPath, configRecords);

            System.out.println("文件已生成: " + outputPath);

            System.out.println("\n=== 3. 验证生成文件的居中样式 ===");
            try (FileInputStream fis2 = new FileInputStream(outputPath);
                 Workbook wb2 = new XSSFWorkbook(fis2)) {
                Sheet sheet2 = wb2.getSheetAt(0);
                Row dataRow = sheet2.getRow(3);

                if (dataRow == null) {
                    for (int i = 0; i <= sheet2.getLastRowNum(); i++) {
                        Row r = sheet2.getRow(i);
                        if (r != null && r.getRowNum() > 1) {
                            dataRow = r;
                            break;
                        }
                    }
                }

                System.out.println("数据行的单元格样式:");
                boolean allCenter = true;
                for (int i = 0; i < 9; i++) {
                    Cell cell = dataRow.getCell(i);
                    if (cell != null) {
                        CellStyle style = cell.getCellStyle();
                        HorizontalAlignment hAlign = style.getAlignment();
                        VerticalAlignment vAlign = style.getVerticalAlignment();
                        boolean isHCenter = hAlign == HorizontalAlignment.CENTER;
                        boolean isVCenter = vAlign == VerticalAlignment.CENTER;
                        System.out.println("  列" + i + ": 水平=" + hAlign + (isHCenter ? " ✓" : " ✗") +
                            ", 垂直=" + vAlign + (isVCenter ? " ✓" : " ✗"));
                        if (!isHCenter || !isVCenter) {
                            allCenter = false;
                        }
                    }
                }

                System.out.println("\n=== 结果 ===");
                if (allCenter) {
                    System.out.println("✓ 所有单元格都已正确居中！");
                } else {
                    System.out.println("✗ 部分单元格未居中");
                }
            }
        }
    }
}