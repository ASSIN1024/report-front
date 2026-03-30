package com.report.common.constant;

import org.junit.Test;

import static org.junit.Assert.*;

public class FieldTypeEnumTest {

    @Test
    public void testGetCode() {
        assertEquals("STRING", FieldTypeEnum.STRING.getCode());
        assertEquals("INTEGER", FieldTypeEnum.INTEGER.getCode());
        assertEquals("DECIMAL", FieldTypeEnum.DECIMAL.getCode());
        assertEquals("DATE", FieldTypeEnum.DATE.getCode());
        assertEquals("DATETIME", FieldTypeEnum.DATETIME.getCode());
    }

    @Test
    public void testGetDesc() {
        assertEquals("字符串", FieldTypeEnum.STRING.getDesc());
        assertEquals("整数", FieldTypeEnum.INTEGER.getDesc());
        assertEquals("小数", FieldTypeEnum.DECIMAL.getDesc());
        assertEquals("日期", FieldTypeEnum.DATE.getDesc());
        assertEquals("日期时间", FieldTypeEnum.DATETIME.getDesc());
    }

    @Test
    public void testGetByCode() {
        assertEquals(FieldTypeEnum.STRING, FieldTypeEnum.getByCode("STRING"));
        assertEquals(FieldTypeEnum.INTEGER, FieldTypeEnum.getByCode("INTEGER"));
        assertNull(FieldTypeEnum.getByCode("UNKNOWN"));
    }
}
