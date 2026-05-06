package com.report.service.impl;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class DateExtractorLogicTest {

    private String extractDateFromFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        }
        Pattern pattern = Pattern.compile("(\\d{8})");
        Matcher matcher = pattern.matcher(fileName);
        ArrayList<String> candidates = new ArrayList<>();
        while (matcher.find()) {
            candidates.add(matcher.group(1));
        }
        for (int i = candidates.size() - 1; i >= 0; i--) {
            String dateStr = candidates.get(i);
            int month = Integer.parseInt(dateStr.substring(4, 6));
            int day = Integer.parseInt(dateStr.substring(6, 8));
            if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                return dateStr.substring(0, 4) + "-" + dateStr.substring(4, 6) + "-" + dateStr.substring(6, 8);
            }
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    @Test
    public void testValidDate_last8Digits() {
        assertEquals("2026-05-20", extractDateFromFileName("scan_12026052_test20260520.xlsx"));
    }

    @Test
    public void testValidDate_singleCandidate() {
        assertEquals("2026-05-20", extractDateFromFileName("report_20260520.xlsx"));
    }

    @Test
    public void testValidDate_multipleCandidates_lastIsValid() {
        assertEquals("2026-04-06", extractDateFromFileName("data_20260406_extra99999999.xlsx"));
    }

    @Test
    public void testInvalidMonth_zeroMonth() {
        assertEquals("2026-04-06", extractDateFromFileName("report_20260400_test20260406.xlsx"));
    }

    @Test
    public void testInvalidMonth_13() {
        assertEquals("2026-04-06", extractDateFromFileName("report_20261306_test20260406.xlsx"));
    }

    @Test
    public void testInvalidDay_32() {
        assertEquals("2026-04-06", extractDateFromFileName("report_20260432_test20260406.xlsx"));
    }

    @Test
    public void testInvalidDay_00() {
        assertEquals("2026-04-06", extractDateFromFileName("report_20260400_test20260406.xlsx"));
    }

    @Test
    public void testOnlyInvalidCandidates_usesSystemDate() {
        String result = extractDateFromFileName("scan_12026052_99999999_00000000.xlsx");
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testNullFilename_returnsSystemDate() {
        String result = extractDateFromFileName(null);
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testEmptyFilename_returnsSystemDate() {
        String result = extractDateFromFileName("");
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testBlankFilename_returnsSystemDate() {
        String result = extractDateFromFileName("   ");
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testNoDigitsInFilename_returnsSystemDate() {
        String result = extractDateFromFileName("report_file.xlsx");
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFebruary_28_valid() {
        assertEquals("2026-02-28", extractDateFromFileName("data_20260228.xlsx"));
    }

    @Test
    public void testFebruary_29_leapYear_valid() {
        assertEquals("2024-02-29", extractDateFromFileName("data_20240229.xlsx"));
    }

    @Test
    public void testDecember_31_valid() {
        assertEquals("2026-12-31", extractDateFromFileName("data_20261231.xlsx"));
    }

    @Test
    public void testJanuary_01_valid() {
        assertEquals("2026-01-01", extractDateFromFileName("data_20260101.xlsx"));
    }

    @Test
    public void testAllInvalidCandidates_allLeadingZeros() {
        String result = extractDateFromFileName("scan_00000000_test00000000.xlsx");
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testValidMonth_01() {
        assertEquals("2026-01-15", extractDateFromFileName("data_20260115.xlsx"));
    }

    @Test
    public void testValidMonth_12() {
        assertEquals("2026-12-15", extractDateFromFileName("data_20261215.xlsx"));
    }

    @Test
    public void testValidDay_01() {
        assertEquals("2026-05-01", extractDateFromFileName("data_20260501.xlsx"));
    }

    @Test
    public void testValidDay_31() {
        assertEquals("2026-07-31", extractDateFromFileName("data_20260731.xlsx"));
    }

    @Test
    public void testMiddleCandidateValid() {
        assertEquals("2026-05-20", extractDateFromFileName("scan_12026052_20260520_99999999.xlsx"));
    }

    @Test
    public void testFirstCandidateValidOthersInvalid() {
        assertEquals("2026-05-20", extractDateFromFileName("20260520_99999999_00000000.xlsx"));
    }

    @Test
    public void testMixedValidAndInvalid() {
        assertEquals("2026-04-06", extractDateFromFileName("report_20261306_20260406_20263206.xlsx"));
    }
}