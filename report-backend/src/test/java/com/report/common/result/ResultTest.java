package com.report.common.result;

import org.junit.Test;

import static org.junit.Assert.*;

public class ResultTest {

    @Test
    public void testSuccess() {
        Result<String> result = Result.success("test data");
        assertEquals(Integer.valueOf(200), result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertEquals("test data", result.getData());
        assertNotNull(result.getTimestamp());
    }

    @Test
    public void testSuccessWithNullData() {
        Result<Void> result = Result.success();
        assertEquals(Integer.valueOf(200), result.getCode());
        assertNull(result.getData());
    }

    @Test
    public void testFail() {
        Result<Void> result = Result.fail("操作失败");
        assertEquals(Integer.valueOf(500), result.getCode());
        assertEquals("操作失败", result.getMessage());
    }

    @Test
    public void testFailWithCode() {
        Result<Void> result = Result.fail(400, "参数错误");
        assertEquals(Integer.valueOf(400), result.getCode());
        assertEquals("参数错误", result.getMessage());
    }
}
