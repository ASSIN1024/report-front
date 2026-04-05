package com.report.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ColumnMappingValidatorTest {

    @Test
    public void testValidate_ValidJson() {
        String validJson = "[\n" +
                "  {\n" +
                "    \"excelColumn\": \"A\",\n" +
                "    \"fieldName\": \"amount\",\n" +
                "    \"fieldType\": \"DECIMAL\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"excelColumn\": \"B\",\n" +
                "    \"fieldName\": \"createDate\",\n" +
                "    \"fieldType\": \"DATE\"\n" +
                "  }\n" +
                "]";

        List<ColumnMappingValidator.ValidationError> errors = ColumnMappingValidator.validate(validJson);

        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidate_ValidJsonWithCleanRules() {
        String validJson = "[\n" +
                "  {\n" +
                "    \"excelColumn\": \"A\",\n" +
                "    \"fieldName\": \"amount\",\n" +
                "    \"fieldType\": \"DECIMAL\",\n" +
                "    \"cleanRules\": [\n" +
                "      {\"pattern\": \"-\", \"replace\": \"0\"},\n" +
                "      {\"pattern\": \"N/A\", \"replace\": \"\"}\n" +
                "    ]\n" +
                "  }\n" +
                "]";

        List<ColumnMappingValidator.ValidationError> errors = ColumnMappingValidator.validate(validJson);

        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidate_InvalidFieldType() {
        String invalidJson = "[\n" +
                "  {\n" +
                "    \"excelColumn\": \"A\",\n" +
                "    \"fieldName\": \"amount\",\n" +
                "    \"fieldType\": \"DECIMALL\"\n" +
                "  }\n" +
                "]";

        List<ColumnMappingValidator.ValidationError> errors = ColumnMappingValidator.validate(invalidJson);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessage().contains("无效的字段类型"));
        assertEquals(1, errors.get(0).getLine());
    }

    @Test
    public void testValidate_MissingExcelColumn() {
        String invalidJson = "[\n" +
                "  {\n" +
                "    \"fieldName\": \"amount\",\n" +
                "    \"fieldType\": \"DECIMAL\"\n" +
                "  }\n" +
                "]";

        List<ColumnMappingValidator.ValidationError> errors = ColumnMappingValidator.validate(invalidJson);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessage().contains("excelColumn"));
    }

    @Test
    public void testValidate_MissingFieldName() {
        String invalidJson = "[\n" +
                "  {\n" +
                "    \"excelColumn\": \"A\",\n" +
                "    \"fieldType\": \"DECIMAL\"\n" +
                "  }\n" +
                "]";

        List<ColumnMappingValidator.ValidationError> errors = ColumnMappingValidator.validate(invalidJson);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessage().contains("fieldName"));
    }

    @Test
    public void testValidate_MissingFieldType() {
        String invalidJson = "[\n" +
                "  {\n" +
                "    \"excelColumn\": \"A\",\n" +
                "    \"fieldName\": \"amount\"\n" +
                "  }\n" +
                "]";

        List<ColumnMappingValidator.ValidationError> errors = ColumnMappingValidator.validate(invalidJson);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessage().contains("fieldType"));
    }

    @Test
    public void testValidate_InvalidJsonSyntax() {
        String invalidJson = "[{ invalid json }]";

        List<ColumnMappingValidator.ValidationError> errors = ColumnMappingValidator.validate(invalidJson);

        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).getMessage().contains("JSON格式错误"));
    }

    @Test
    public void testValidate_EmptyJson() {
        String emptyJson = "";

        List<ColumnMappingValidator.ValidationError> errors = ColumnMappingValidator.validate(emptyJson);

        assertFalse(errors.isEmpty());
        assertEquals(0, errors.get(0).getLine());
    }

    @Test
    public void testValidate_CleanRuleMissingPattern() {
        String invalidJson = "[\n" +
                "  {\n" +
                "    \"excelColumn\": \"A\",\n" +
                "    \"fieldName\": \"amount\",\n" +
                "    \"fieldType\": \"DECIMAL\",\n" +
                "    \"cleanRules\": [\n" +
                "      {\"replace\": \"0\"}\n" +
                "    ]\n" +
                "  }\n" +
                "]";

        List<ColumnMappingValidator.ValidationError> errors = ColumnMappingValidator.validate(invalidJson);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessage().contains("缺少pattern字段"));
    }

    @Test
    public void testValidate_AllFieldTypes() {
        String json = "[\n" +
                "  {\"excelColumn\": \"A\", \"fieldName\": \"f1\", \"fieldType\": \"STRING\"},\n" +
                "  {\"excelColumn\": \"B\", \"fieldName\": \"f2\", \"fieldType\": \"INTEGER\"},\n" +
                "  {\"excelColumn\": \"C\", \"fieldName\": \"f3\", \"fieldType\": \"DECIMAL\"},\n" +
                "  {\"excelColumn\": \"D\", \"fieldName\": \"f4\", \"fieldType\": \"DATE\"},\n" +
                "  {\"excelColumn\": \"E\", \"fieldName\": \"f5\", \"fieldType\": \"DATETIME\"}\n" +
                "]";

        List<ColumnMappingValidator.ValidationError> errors = ColumnMappingValidator.validate(json);

        assertTrue(errors.isEmpty());
    }

    @Test
    public void testCountMappings() {
        String json = "[\n" +
                "  {\"excelColumn\": \"A\", \"fieldName\": \"f1\", \"fieldType\": \"STRING\"},\n" +
                "  {\"excelColumn\": \"B\", \"fieldName\": \"f2\", \"fieldType\": \"STRING\"},\n" +
                "  {\"excelColumn\": \"C\", \"fieldName\": \"f3\", \"fieldType\": \"STRING\"}\n" +
                "]";

        int count = ColumnMappingValidator.countMappings(json);

        assertEquals(3, count);
    }

    @Test
    public void testCountMappings_InvalidJson() {
        String invalidJson = "not a json array";

        int count = ColumnMappingValidator.countMappings(invalidJson);

        assertEquals(0, count);
    }
}
