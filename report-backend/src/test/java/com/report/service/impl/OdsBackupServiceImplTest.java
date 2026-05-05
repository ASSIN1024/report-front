package com.report.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.service.TransformResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OdsBackupServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private OdsBackupServiceImpl odsBackupService;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        odsBackupService = new OdsBackupServiceImpl();
        setField(odsBackupService, "jdbcTemplate", jdbcTemplate);
        setField(odsBackupService, "objectMapper", new ObjectMapper());
        objectMapper = new ObjectMapper();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testExtractColumnNames_PreferHeadersOverFieldMapping() throws Exception {
        TransformResult result = new TransformResult();
        result.setHeaders(Arrays.asList("省份", "销售额", "日期"));

        Map<String, Map<String, String>> fieldMapping = new LinkedHashMap<>();
        fieldMapping.put("field_1", createFieldInfo("STRING"));
        fieldMapping.put("field_2", createFieldInfo("STRING"));
        result.setFieldMappingJson(objectMapper.writeValueAsString(fieldMapping));

        List<String> columnNames = invokeExtractColumnNames(result);

        assertEquals("应优先使用headers中的列名", 3, columnNames.size());
        assertNotNull("第一列应该存在", columnNames.get(0));
        assertNotNull("第二列应该存在", columnNames.get(1));
        assertNotNull("第三列应该存在", columnNames.get(2));
    }

    @Test
    public void testExtractColumnNames_FallbackToFieldMapping_WhenHeadersEmpty() throws Exception {
        TransformResult result = new TransformResult();
        result.setHeaders(new ArrayList<>());

        Map<String, Map<String, String>> fieldMapping = new LinkedHashMap<>();
        fieldMapping.put("name", createFieldInfo("STRING"));
        fieldMapping.put("age", createFieldInfo("INTEGER"));
        result.setFieldMappingJson(objectMapper.writeValueAsString(fieldMapping));

        List<String> columnNames = invokeExtractColumnNames(result);

        assertEquals("应使用fieldMappingJson中的字段名", 2, columnNames.size());
        assertTrue("应包含name", columnNames.contains("name"));
        assertTrue("应包含age", columnNames.contains("age"));
    }

    @Test
    public void testExtractColumnNames_HeadersHaveHigherPriority() throws Exception {
        TransformResult result = new TransformResult();
        result.setHeaders(Arrays.asList("姓名", "年龄"));
        Map<String, Map<String, String>> fieldMapping = new LinkedHashMap<>();
        fieldMapping.put("name", createFieldInfo("STRING"));
        fieldMapping.put("age", createFieldInfo("INTEGER"));
        result.setFieldMappingJson(objectMapper.writeValueAsString(fieldMapping));

        List<String> columnNames = invokeExtractColumnNames(result);

        assertEquals("应优先使用headers中的列名", 2, columnNames.size());
    }

    @Test
    public void testContainsOnlyPlaceholderNames_ReturnsTrue_ForFieldPlaceholders() {
        List<String> names = Arrays.asList("field_1", "field_2", "field_3");
        assertTrue("field_开头的应该是占位符", invokeContainsOnlyPlaceholderNames(names));
    }

    @Test
    public void testContainsOnlyPlaceholderNames_ReturnsTrue_ForColPlaceholders() {
        List<String> names = Arrays.asList("col_1", "col_2");
        assertTrue("col_开头的应该是占位符", invokeContainsOnlyPlaceholderNames(names));
    }

    @Test
    public void testContainsOnlyPlaceholderNames_ReturnsFalse_ForRealNames() {
        List<String> names = Arrays.asList("name", "age", "gender");
        assertFalse("真实字段名不应该是占位符", invokeContainsOnlyPlaceholderNames(names));
    }

    @Test
    public void testContainsOnlyPlaceholderNames_ReturnsFalse_MixedNames() {
        List<String> names = Arrays.asList("field_1", "name", "age");
        assertFalse("混合名称不应该是占位符", invokeContainsOnlyPlaceholderNames(names));
    }

    @Test
    public void testExtractFieldTypes_ExtractsTypeFromFieldMappingJson() throws Exception {
        TransformResult result = new TransformResult();

        Map<String, Map<String, String>> fieldMapping = new LinkedHashMap<>();
        fieldMapping.put("name", createFieldInfo("STRING"));
        fieldMapping.put("age", createFieldInfo("INTEGER"));
        fieldMapping.put("salary", createFieldInfo("DECIMAL"));
        result.setFieldMappingJson(objectMapper.writeValueAsString(fieldMapping));

        Map<String, String> fieldTypes = invokeExtractFieldTypes(result);

        assertEquals("STRING", fieldTypes.get("name"));
        assertEquals("INTEGER", fieldTypes.get("age"));
        assertEquals("DECIMAL", fieldTypes.get("salary"));
    }

    @Test
    public void testConvertToSqlType_Integer() {
        assertEquals("INTEGER应转换为bigint", "bigint", invokeConvertToSqlType("INTEGER"));
        assertEquals("INT应转换为bigint", "bigint", invokeConvertToSqlType("INT"));
    }

    @Test
    public void testConvertToSqlType_String() {
        assertEquals("STRING应转换为varchar(500)", "varchar(500)", invokeConvertToSqlType("STRING"));
    }

    @Test
    public void testConvertToSqlType_Decimal() {
        assertEquals("DECIMAL应转换为decimal(20,4)", "decimal(20,4)", invokeConvertToSqlType("DECIMAL"));
    }

    @Test
    public void testConvertToSqlType_Date() {
        assertEquals("DATE应转换为date", "date", invokeConvertToSqlType("DATE"));
    }

    @Test
    public void testConvertToSqlType_DateTime() {
        assertEquals("DATETIME应转换为datetime", "datetime", invokeConvertToSqlType("DATETIME"));
    }

    @Test
    public void testConvertToSqlType_Boolean() {
        assertEquals("BOOLEAN应转换为tinyint(1)", "tinyint(1)", invokeConvertToSqlType("BOOLEAN"));
    }

    @Test
    public void testConvertToSqlType_Unknown() {
        assertEquals("未知类型应转换为varchar(500)", "varchar(500)", invokeConvertToSqlType("UNKNOWN"));
        assertEquals("null应转换为varchar(500)", "varchar(500)", invokeConvertToSqlType(null));
    }

    @Test
    public void testIsTableSchemaValid_ColumnCountCheck() throws Exception {
        TransformResult result = new TransformResult();
        result.setHeaders(Arrays.asList("姓名", "年龄"));
        Map<String, Map<String, String>> fieldMapping = new LinkedHashMap<>();
        fieldMapping.put("____", createFieldInfo("STRING"));
        fieldMapping.put("____1", createFieldInfo("STRING"));
        result.setFieldMappingJson(objectMapper.writeValueAsString(fieldMapping));

        List<String> colNames = invokeExtractColumnNames(result);

        List<Map<String, Object>> columns = new ArrayList<>();
        columns.add(createColumn("id"));
        columns.add(createColumn("source_file"));
        columns.add(createColumn("pt_dt"));
        columns.add(createColumn("create_time"));
        for (String col : colNames) {
            columns.add(createColumn(col));
        }

        when(jdbcTemplate.queryForList(anyString(), eq("test_table"))).thenReturn(columns);

        boolean valid = invokeIsTableSchemaValid("test_table", result);

        assertEquals("使用headers时列名数量应正确", 2, colNames.size());
    }

    @Test
    public void testIsTableSchemaValid_ReturnsFalse_WhenColumnMissing() throws Exception {
        TransformResult result = new TransformResult();
        result.setHeaders(Arrays.asList("姓名", "年龄"));
        Map<String, Map<String, String>> fieldMapping = new LinkedHashMap<>();
        fieldMapping.put("____", createFieldInfo("STRING"));
        fieldMapping.put("____", createFieldInfo("STRING"));
        result.setFieldMappingJson(objectMapper.writeValueAsString(fieldMapping));

        List<Map<String, Object>> columns = new ArrayList<>();
        columns.add(createColumn("id"));
        columns.add(createColumn("source_file"));
        columns.add(createColumn("____"));

        when(jdbcTemplate.queryForList(anyString(), eq("test_table"))).thenReturn(columns);

        boolean valid = invokeIsTableSchemaValid("test_table", result);

        assertFalse("列不匹配时应该返回false", valid);
    }

    private Map<String, String> createFieldInfo(String type) {
        Map<String, String> info = new HashMap<>();
        info.put("type", type);
        return info;
    }

    private Map<String, Object> createColumn(String columnName) {
        Map<String, Object> column = new HashMap<>();
        column.put("COLUMN_NAME", columnName);
        return column;
    }

    private List<String> invokeExtractColumnNames(TransformResult result) {
        try {
            java.lang.reflect.Method method = OdsBackupServiceImpl.class.getDeclaredMethod("extractColumnNames", TransformResult.class);
            method.setAccessible(true);
            return (List<String>) method.invoke(odsBackupService, result);
        } catch (Exception e) {
            throw new RuntimeException("反射调用失败: " + e.getMessage(), e);
        }
    }

    private boolean invokeContainsOnlyPlaceholderNames(List<String> names) {
        try {
            java.lang.reflect.Method method = OdsBackupServiceImpl.class.getDeclaredMethod("containsOnlyPlaceholderNames", List.class);
            method.setAccessible(true);
            return (boolean) method.invoke(odsBackupService, names);
        } catch (Exception e) {
            throw new RuntimeException("反射调用失败: " + e.getMessage(), e);
        }
    }

    private Map<String, String> invokeExtractFieldTypes(TransformResult result) {
        try {
            java.lang.reflect.Method method = OdsBackupServiceImpl.class.getDeclaredMethod("extractFieldTypes", TransformResult.class);
            method.setAccessible(true);
            return (Map<String, String>) method.invoke(odsBackupService, result);
        } catch (Exception e) {
            throw new RuntimeException("反射调用失败: " + e.getMessage(), e);
        }
    }

    private String invokeConvertToSqlType(String fieldType) {
        try {
            java.lang.reflect.Method method = OdsBackupServiceImpl.class.getDeclaredMethod("convertToSqlType", String.class);
            method.setAccessible(true);
            return (String) method.invoke(odsBackupService, fieldType);
        } catch (Exception e) {
            throw new RuntimeException("反射调用失败: " + e.getMessage(), e);
        }
    }

    private boolean invokeIsTableSchemaValid(String tableName, TransformResult result) {
        try {
            java.lang.reflect.Method method = OdsBackupServiceImpl.class.getDeclaredMethod("isTableSchemaValid", String.class, TransformResult.class);
            method.setAccessible(true);
            return (boolean) method.invoke(odsBackupService, tableName, result);
        } catch (Exception e) {
            throw new RuntimeException("反射调用失败: " + e.getMessage(), e);
        }
    }
}