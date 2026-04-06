package com.report.util;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.Assert.*;

public class FileNameDateExtractorTest {

    @Before
    public void setUp() {
    }

    @Test
    public void testExtractDate_yyyyMMdd_format() {
        String fileName = "test_flow20260406.xlsx";
        
        LocalDate result = FileNameDateExtractor.extractDate(fileName);
        
        assertNotNull("应该能提取到日期", result);
        assertEquals("年份应该是2026", 2026, result.getYear());
        assertEquals("月份应该是4月", Month.APRIL, result.getMonth());
        assertEquals("日应该是6号", 6, result.getDayOfMonth());
    }

    @Test
    public void testExtractDate_yyyy_MM_dd_format() {
        String fileName = "sales_2026-04-06.xlsx";
        
        LocalDate result = FileNameDateExtractor.extractDate(fileName);
        
        assertNotNull("应该能提取到日期", result);
        assertEquals(2026, result.getYear());
        assertEquals(Month.APRIL, result.getMonth());
        assertEquals(6, result.getDayOfMonth());
    }

    @Test
    public void testExtractDate_with_time_component() {
        String fileName = "data20260406_1430.xlsx";
        
        LocalDate result = FileNameDateExtractor.extractDate(fileName);
        
        assertNotNull("应该能提取到日期", result);
        assertEquals(2026, result.getYear());
        assertEquals(Month.APRIL, result.getMonth());
        assertEquals(6, result.getDayOfMonth());
    }

    @Test
    public void testExtractDate_no_date_in_filename() {
        String fileName = "random_data.xlsx";
        
        LocalDate result = FileNameDateExtractor.extractDate(fileName);
        
        assertNull("无日期的文件名应该返回null", result);
    }

    @Test
    public void testExtractDate_empty_filename() {
        assertNull("null输入应返回null", FileNameDateExtractor.extractDate(null));
        assertNull("空字符串应返回null", FileNameDateExtractor.extractDate(""));
        assertNull("空白字符串应返回null", FileNameDateExtractor.extractDate("   "));
    }

    @Test
    public void testExtractDate_multiple_dates() {
        String fileName = "report_2025-01-01_20260406.xlsx";
        
        LocalDate result = FileNameDateExtractor.extractDate(fileName);
        
        assertNotNull("应该能提取到日期", result);
        assertEquals("应该取第一个匹配的日期（2025年）", 2025, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(1, result.getDayOfMonth());
    }
}
