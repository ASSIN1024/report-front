package com.report.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class ColumnNameValidatorTest {

    @Test
    public void testValidNames() {
        assertTrue("英文单词应通过", ColumnNameValidator.isValidColumnName("name"));
        assertTrue("英文单词+数字应通过", ColumnNameValidator.isValidColumnName("userName"));
        assertTrue("下划线分隔应通过", ColumnNameValidator.isValidColumnName("user_name"));
        assertTrue("下划线开头应通过", ColumnNameValidator.isValidColumnName("_private"));
        assertTrue("数字结尾应通过", ColumnNameValidator.isValidColumnName("field1"));
        assertTrue("驼峰命名应通过", ColumnNameValidator.isValidColumnName("userName2"));
        assertTrue("复杂命名应通过", ColumnNameValidator.isValidColumnName("user_name_2"));
    }

    @Test
    public void testInvalidNames_Chinese() {
        assertFalse("纯中文应拒绝", ColumnNameValidator.isValidColumnName("姓名"));
        assertFalse("中文开头应拒绝", ColumnNameValidator.isValidColumnName("name姓名"));
        assertFalse("中文结尾应拒绝", ColumnNameValidator.isValidColumnName("name姓名"));
        assertFalse("混合中文应拒绝", ColumnNameValidator.isValidColumnName("姓name名"));
    }

    @Test
    public void testInvalidNames_SpecialChars() {
        assertFalse("空格应拒绝", ColumnNameValidator.isValidColumnName("user name"));
        assertFalse("连字符应拒绝", ColumnNameValidator.isValidColumnName("user-name"));
        assertFalse("点号应拒绝", ColumnNameValidator.isValidColumnName("user.name"));
        assertFalse("括号应拒绝", ColumnNameValidator.isValidColumnName("user(name"));
        assertFalse("感叹号应拒绝", ColumnNameValidator.isValidColumnName("user!"));
        assertFalse("问号应拒绝", ColumnNameValidator.isValidColumnName("user?"));
        assertFalse("斜杠应拒绝", ColumnNameValidator.isValidColumnName("user/name"));
        assertFalse("反斜杠应拒绝", ColumnNameValidator.isValidColumnName("user\\name"));
    }

    @Test
    public void testInvalidNames_StartWithNumber() {
        assertFalse("数字开头应拒绝", ColumnNameValidator.isValidColumnName("1name"));
        assertFalse("纯数字应拒绝", ColumnNameValidator.isValidColumnName("123"));
    }

    @Test
    public void testInvalidNames_SpecialPatterns() {
        assertFalse("纯下划线应拒绝", ColumnNameValidator.isValidColumnName("____"));
        assertFalse("多下划线应拒绝", ColumnNameValidator.isValidColumnName("__"));
        assertFalse("单下划线应拒绝", ColumnNameValidator.isValidColumnName("_"));
    }

    @Test
    public void testInvalidNames_EmptyAndNull() {
        assertFalse("空字符串应拒绝", ColumnNameValidator.isValidColumnName(""));
        assertFalse("空白字符串应拒绝", ColumnNameValidator.isValidColumnName("   "));
        assertFalse("null应拒绝", ColumnNameValidator.isValidColumnName(null));
    }

    @Test
    public void testInvalidNames_TooLong() {
        String longName = "";
        for (int i = 0; i < 65; i++) {
            longName += "a";
        }
        assertFalse("超过64字符应拒绝", ColumnNameValidator.isValidColumnName(longName));
    }

    @Test
    public void testValidNames_EdgeCases() {
        assertTrue("单字符应通过", ColumnNameValidator.isValidColumnName("a"));
        assertTrue("单字母应通过", ColumnNameValidator.isValidColumnName("A"));
        assertTrue("下划线开头应通过", ColumnNameValidator.isValidColumnName("_name"));
        assertTrue("下划线+数字应通过", ColumnNameValidator.isValidColumnName("_1"));
        assertTrue("下划线+多数字应通过", ColumnNameValidator.isValidColumnName("_123"));
        String name64 = "";
        for (int i = 0; i < 64; i++) {
            name64 += "a";
        }
        assertTrue("64字符应通过", ColumnNameValidator.isValidColumnName(name64));
    }

    @Test
    public void testGetValidationMessage() {
        assertNotNull("应有错误消息", ColumnNameValidator.getValidationMessage("姓名"));
        assertNotNull("应有错误消息", ColumnNameValidator.getValidationMessage(""));
        assertTrue("消息应包含说明", ColumnNameValidator.getValidationMessage("姓名").contains("英文"));
    }

    @Test
    public void testValidExcelColumnName() {
        assertTrue("A列应通过", ColumnNameValidator.isValidExcelColumnName("A"));
        assertTrue("Z列应通过", ColumnNameValidator.isValidExcelColumnName("Z"));
        assertTrue("AA列应通过", ColumnNameValidator.isValidExcelColumnName("AA"));
        assertTrue("AZ列应通过", ColumnNameValidator.isValidExcelColumnName("AZ"));
        assertTrue("BA列应通过", ColumnNameValidator.isValidExcelColumnName("BA"));
    }

    @Test
    public void testInvalidExcelColumnName() {
        assertFalse("中文列名应拒绝", ColumnNameValidator.isValidExcelColumnName("姓名"));
        assertFalse("数字列名应拒绝", ColumnNameValidator.isValidExcelColumnName("1"));
        assertFalse("混合应拒绝", ColumnNameValidator.isValidExcelColumnName("A1"));
    }
}