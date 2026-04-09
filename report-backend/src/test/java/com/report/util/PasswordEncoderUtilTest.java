package com.report.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * PasswordEncoderUtil 测试类
 * 测试BCrypt密码加密和验证功能
 */
public class PasswordEncoderUtilTest {

    @Test
    public void testEncode() {
        // 测试加密功能
        String rawPassword = "password123";
        String encodedPassword = PasswordEncoderUtil.encode(rawPassword);
        
        // 加密后的密码不应为null
        assertNotNull("加密后的密码不应为null", encodedPassword);
        // 加密后的密码不应等于原始密码
        assertNotEquals("加密后的密码不应等于原始密码", rawPassword, encodedPassword);
        // BCrypt加密后的密码长度应为60
        assertEquals("BCrypt加密后的密码长度应为60", 60, encodedPassword.length());
    }

    @Test
    public void testMatches_CorrectPassword() {
        // 测试正确密码验证
        String rawPassword = "password123";
        String encodedPassword = PasswordEncoderUtil.encode(rawPassword);
        
        // 正确密码应该匹配
        assertTrue("正确密码应该匹配", 
            PasswordEncoderUtil.matches(rawPassword, encodedPassword));
    }

    @Test
    public void testMatches_WrongPassword() {
        // 测试错误密码验证
        String rawPassword = "password123";
        String wrongPassword = "wrongpassword";
        String encodedPassword = PasswordEncoderUtil.encode(rawPassword);
        
        // 错误密码不应该匹配
        assertFalse("错误密码不应该匹配", 
            PasswordEncoderUtil.matches(wrongPassword, encodedPassword));
    }

    @Test
    public void testEncode_DifferentHashForSamePassword() {
        // 测试相同密码每次加密结果不同（BCrypt特性）
        String rawPassword = "password123";
        String encoded1 = PasswordEncoderUtil.encode(rawPassword);
        String encoded2 = PasswordEncoderUtil.encode(rawPassword);
        
        // 两次加密结果应该不同（因为BCrypt会生成随机盐）
        assertNotEquals("相同密码每次加密结果应该不同", encoded1, encoded2);
        
        // 但两个加密结果都应该能匹配原始密码
        assertTrue("第一个加密结果应该匹配原始密码", 
            PasswordEncoderUtil.matches(rawPassword, encoded1));
        assertTrue("第二个加密结果应该匹配原始密码", 
            PasswordEncoderUtil.matches(rawPassword, encoded2));
    }

    @Test
    public void testEncode_EmptyPassword() {
        // 测试空密码
        String emptyPassword = "";
        String encodedPassword = PasswordEncoderUtil.encode(emptyPassword);
        
        assertNotNull("空密码也能被加密", encodedPassword);
        assertEquals("空密码加密后长度应为60", 60, encodedPassword.length());
        assertTrue("空密码应该匹配", 
            PasswordEncoderUtil.matches(emptyPassword, encodedPassword));
    }

    @Test
    public void testMatches_NullRawPassword() {
        // 测试null原始密码
        String encodedPassword = PasswordEncoderUtil.encode("password123");
        assertFalse("null密码不应该匹配", 
            PasswordEncoderUtil.matches(null, encodedPassword));
    }

    @Test
    public void testMatches_NullEncodedPassword() {
        // 测试null加密密码
        assertFalse("null加密密码不应该匹配", 
            PasswordEncoderUtil.matches("password123", null));
    }
}
