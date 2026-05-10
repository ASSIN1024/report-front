package com.report.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestTemplate {

    public static void main(String[] args) throws Exception {
        String templatePath = "src/main/resources/informationTemplate.xlsx";

        System.out.println("=== 1. 检查模板 ===");
        try (FileInputStream fis = new FileInputStream(templatePath);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                System.out.println("表头单元格样式:");
                for (int i = 0; i < 9; i++) {
                    Cell cell = headerRow.getCell(i);
                    if (cell != null) {
                        CellStyle style = cell.getCellStyle();
                        System.out.println("  列" + i + ": 对齐=" + style.getAlignment() +
                            ", 垂直=" + style.getVerticalAlignment() +
                            ", 背景=" + style.getFillForegroundColorColor());
                    }
                }
            }

            System.out.println("\n=== 2. 测试写入数据 ===");
            Row dataRow = sheet.createRow(1);
            for (int i = 0; i < 9; i++) {
                Cell cell = dataRow.createCell(i);
                cell.setCellValue("测试" + i);

                CellStyle origStyle = headerRow.getCell(i) != null ?
                    headerRow.getCell(i).getCellStyle() : null;
                if (origStyle != null) {
                    CellStyle newStyle = wb.createCellStyle();
                    newStyle.cloneStyleFrom(origStyle);
                    cell.setCellStyle(newStyle);
                }
            }

            System.out.println("数据行写入完成");

            System.out.println("\n=== 3. 验证样式是否保留 ===");
            Row testRow = sheet.getRow(1);
            for (int i = 0; i < 9; i++) {
                Cell cell = testRow.getCell(i);
                if (cell != null) {
                    CellStyle style = cell.getCellStyle();
                    System.out.println("  列" + i + ": 值=" + cell.getStringCellValue() +
                        ", 对齐=" + style.getAlignment());
                }
            }

            FileOutputStream fos = new FileOutputStream("test_output.xlsx");
            wb.write(fos);
            fos.close();
            System.out.println("\n测试文件已输出: test_output.xlsx");
        }
    }
}
