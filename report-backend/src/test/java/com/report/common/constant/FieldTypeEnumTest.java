package com.report.common.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FieldTypeEnumTest {

    @Test
    public void testGetType() {
        assertEquals("STRING", FieldTypeEnum.STRING.getType());
        assertEquals("INTEGER", FieldTypeEnum.INTEGER.getType());
        assertEquals("DECIMAL", FieldTypeEnum.DECIMAL.getType());
        assertEquals("DATE", FieldTypeEnum.DATE.getType());
        assertEquals("DATETIME", FieldTypeEnum.DATETIME.getType());
    }

    @Test
    public void testGetDescription() {
        assertEquals("字符串", FieldTypeEnum.STRING.getDescription());
        assertEquals("整数", FieldTypeEnum.INTEGER.getDescription());
        assertEquals("小数", FieldTypeEnum.DECIMAL.getDescription());
        assertEquals("日期", FieldTypeEnum.DATE.getDescription());
        assertEquals("日期时间", FieldTypeEnum.DATETIME.getDescription());
    }

    @Test
    public void testFindByType() {
        assertEquals(FieldTypeEnum.STRING, FieldTypeEnum.findByType("STRING"));
        assertEquals(FieldTypeEnum.INTEGER, FieldTypeEnum.findByType("INTEGER"));
        assertNull(FieldTypeEnum.findByType("UNKNOWN"));
    }
}
