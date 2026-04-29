package com.report.packing.generator.impl;

import com.report.entity.ProcessedFile;
import com.report.entity.ReportConfig;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class ConfigTableGeneratorImplTest {

    @Test
    public void testBuildPartitionInfo_withPartitionModeAndDate() {
        ConfigTableGeneratorImpl generator = new ConfigTableGeneratorImpl();

        ReportConfig config = new ReportConfig();
        config.setLoadMode("partitioned-append");
        config.setPartitionInfo("pt_dt");

        ProcessedFile file = new ProcessedFile();
        file.setPtDt("20260429");

        String result = invokeBuildPartitionInfo(generator, config, file);
        assertEquals("pt_dt='2026-04-29'", result);
    }

    @Test
    public void testBuildPartitionInfo_withCustomFieldName() {
        ConfigTableGeneratorImpl generator = new ConfigTableGeneratorImpl();

        ReportConfig config = new ReportConfig();
        config.setLoadMode("partitioned-overwrite");
        config.setPartitionInfo("pt_date");

        ProcessedFile file = new ProcessedFile();
        file.setPtDt("20260501");

        String result = invokeBuildPartitionInfo(generator, config, file);
        assertEquals("pt_date='2026-05-01'", result);
    }

    @Test
    public void testBuildPartitionInfo_nonPartitionMode() {
        ConfigTableGeneratorImpl generator = new ConfigTableGeneratorImpl();

        ReportConfig config = new ReportConfig();
        config.setLoadMode("non-partitioned-append");
        config.setPartitionInfo("pt_dt");

        ProcessedFile file = new ProcessedFile();
        file.setPtDt("20260429");

        String result = invokeBuildPartitionInfo(generator, config, file);
        assertEquals("", result);
    }

    @Test
    public void testBuildPartitionInfo_noPartitionInfoConfigured() {
        ConfigTableGeneratorImpl generator = new ConfigTableGeneratorImpl();

        ReportConfig config = new ReportConfig();
        config.setLoadMode("partitioned-append");
        config.setPartitionInfo(null);

        ProcessedFile file = new ProcessedFile();
        file.setPtDt("20260429");

        String result = invokeBuildPartitionInfo(generator, config, file);
        assertTrue(result.startsWith("pt_dt='"));
        assertTrue(result.endsWith("'"));
    }

    @Test
    public void testBuildPartitionInfo_withExistingHyphenFormat() {
        ConfigTableGeneratorImpl generator = new ConfigTableGeneratorImpl();

        ReportConfig config = new ReportConfig();
        config.setLoadMode("partitioned-append");
        config.setPartitionInfo("pt_dt");

        ProcessedFile file = new ProcessedFile();
        file.setPtDt("2026-04-29");

        String result = invokeBuildPartitionInfo(generator, config, file);
        assertEquals("pt_dt='2026-04-29'", result);
    }

    @Test
    public void testFormatDateToHyphen() throws Exception {
        ConfigTableGeneratorImpl generator = new ConfigTableGeneratorImpl();
        Method method = ConfigTableGeneratorImpl.class.getDeclaredMethod("formatDateToHyphen", String.class);
        method.setAccessible(true);

        assertEquals("2026-04-29", method.invoke(generator, "20260429"));
        assertEquals("2026-04-29", method.invoke(generator, "2026-04-29"));
        assertEquals("2026-04-29", method.invoke(generator, "2026/04/29"));
    }

    @Test
    public void testExtractPartitionFieldName() throws Exception {
        ConfigTableGeneratorImpl generator = new ConfigTableGeneratorImpl();
        Method method = ConfigTableGeneratorImpl.class.getDeclaredMethod("extractPartitionFieldName", String.class);
        method.setAccessible(true);

        assertEquals("pt_dt", method.invoke(generator, "pt_dt"));
        assertEquals("pt_dt", method.invoke(generator, "pt_dt='2022-01-01'"));
        assertEquals("custom_partition", method.invoke(generator, "custom_partition"));
    }

    private String invokeBuildPartitionInfo(ConfigTableGeneratorImpl generator, ReportConfig config, ProcessedFile file) {
        try {
            Method method = ConfigTableGeneratorImpl.class.getDeclaredMethod("buildPartitionInfo", ReportConfig.class, ProcessedFile.class);
            method.setAccessible(true);
            return (String) method.invoke(generator, config, file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testBuildFieldTypeJson_withColumnMappings() throws Exception {
        ConfigTableGeneratorImpl generator = new ConfigTableGeneratorImpl();
        ReportConfig config = new ReportConfig();
        config.setColumnMapping("[{\"fieldName\":\"id\",\"fieldType\":\"INTEGER\"},{\"fieldName\":\"name\",\"fieldType\":\"STRING\"},{\"fieldName\":\"birthday\",\"fieldType\":\"DATE\"}]");

        Method method = ConfigTableGeneratorImpl.class.getDeclaredMethod("buildFieldTypeJson", ReportConfig.class);
        method.setAccessible(true);
        String result = (String) method.invoke(generator, config);

        assertTrue(result.contains("\"id\":\"int\""));
        assertTrue(result.contains("\"name\":\"string\""));
        assertTrue(result.contains("\"birthday\":\"date\""));
    }

    @Test
    public void testBuildFieldTypeJson_withEmptyMapping() throws Exception {
        ConfigTableGeneratorImpl generator = new ConfigTableGeneratorImpl();
        ReportConfig config = new ReportConfig();
        config.setColumnMapping("[]");

        Method method = ConfigTableGeneratorImpl.class.getDeclaredMethod("buildFieldTypeJson", ReportConfig.class);
        method.setAccessible(true);
        String result = (String) method.invoke(generator, config);

        assertEquals("{}", result);
    }

    @Test
    public void testBuildFieldTypeJson_withNullMapping() throws Exception {
        ConfigTableGeneratorImpl generator = new ConfigTableGeneratorImpl();
        ReportConfig config = new ReportConfig();
        config.setColumnMapping(null);

        Method method = ConfigTableGeneratorImpl.class.getDeclaredMethod("buildFieldTypeJson", ReportConfig.class);
        method.setAccessible(true);
        String result = (String) method.invoke(generator, config);

        assertEquals("{}", result);
    }

    @Test
    public void testConvertToTargetType() throws Exception {
        ConfigTableGeneratorImpl generator = new ConfigTableGeneratorImpl();
        Method method = ConfigTableGeneratorImpl.class.getDeclaredMethod("convertToTargetType", String.class);
        method.setAccessible(true);

        assertEquals("string", method.invoke(generator, "STRING"));
        assertEquals("int", method.invoke(generator, "INTEGER"));
        assertEquals("double", method.invoke(generator, "DECIMAL"));
        assertEquals("date", method.invoke(generator, "DATE"));
        assertEquals("timestamp", method.invoke(generator, "DATETIME"));
        assertEquals("string", method.invoke(generator, "UNKNOWN"));
    }
}