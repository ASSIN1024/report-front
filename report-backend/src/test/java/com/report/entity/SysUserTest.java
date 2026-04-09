package com.report.entity;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * SysUser 实体类测试
 */
public class SysUserTest {

    @Test
    public void testGetterAndSetter() {
        SysUser user = new SysUser();

        // 测试 setter 和 getter
        Long id = 1L;
        String username = "testuser";
        String password = "testpass123";
        Date lastLoginTime = new Date();
        Date createTime = new Date();
        Date updateTime = new Date();

        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setLastLoginTime(lastLoginTime);
        user.setCreateTime(createTime);
        user.setUpdateTime(updateTime);

        assertEquals(id, user.getId());
        assertEquals(username, user.getUsername());
        assertEquals(password, user.getPassword());
        assertEquals(lastLoginTime, user.getLastLoginTime());
        assertEquals(createTime, user.getCreateTime());
        assertEquals(updateTime, user.getUpdateTime());
    }

    @Test
    public void testSerialization() throws Exception {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("testpass123");
        user.setLastLoginTime(new Date());
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());

        // 序列化
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(user);
        oos.close();

        // 反序列化
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        SysUser deserializedUser = (SysUser) ois.readObject();
        ois.close();

        // 验证
        assertNotNull(deserializedUser);
        assertEquals(user.getId(), deserializedUser.getId());
        assertEquals(user.getUsername(), deserializedUser.getUsername());
        assertEquals(user.getPassword(), deserializedUser.getPassword());
    }

    @Test
    public void testNullValues() {
        SysUser user = new SysUser();

        // 测试默认值为 null
        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNull(user.getPassword());
        assertNull(user.getLastLoginTime());
        assertNull(user.getCreateTime());
        assertNull(user.getUpdateTime());
    }
}
