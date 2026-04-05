package com.report.ftp;

import org.junit.Test;

import static org.junit.Assert.*;

public class BuiltInFtpConfigTest {

    @Test
    public void testGettersAndSetters() {
        BuiltInFtpConfig config = new BuiltInFtpConfig();
        config.setId(100L);
        config.setEnabled(true);
        config.setPort(2121);
        config.setUsername("testuser");
        config.setPassword("testpass");
        config.setRootDirectory("/test/root");
        config.setMaxConnections(20);
        config.setIdleTimeout(600);
        config.setPassiveMode(false);
        config.setPassivePortStart(51000);
        config.setPassivePortEnd(51100);

        assertEquals(Long.valueOf(100L), config.getId());
        assertTrue(config.getEnabled());
        assertEquals(Integer.valueOf(2121), config.getPort());
        assertEquals("testuser", config.getUsername());
        assertEquals("testpass", config.getPassword());
        assertEquals("/test/root", config.getRootDirectory());
        assertEquals(Integer.valueOf(20), config.getMaxConnections());
        assertEquals(Integer.valueOf(600), config.getIdleTimeout());
        assertFalse(config.getPassiveMode());
        assertEquals(Integer.valueOf(51000), config.getPassivePortStart());
        assertEquals(Integer.valueOf(51100), config.getPassivePortEnd());
    }

    @Test
    public void testDefaultValues() {
        BuiltInFtpConfig config = new BuiltInFtpConfig();

        assertNull(config.getId());
        assertNull(config.getEnabled());
        assertNull(config.getPort());
        assertNull(config.getUsername());
        assertNull(config.getPassword());
        assertNull(config.getRootDirectory());
        assertNull(config.getMaxConnections());
        assertNull(config.getIdleTimeout());
        assertNull(config.getPassiveMode());
        assertNull(config.getPassivePortStart());
        assertNull(config.getPassivePortEnd());
    }

    @Test
    public void testEqualsAndHashCode() {
        BuiltInFtpConfig config1 = new BuiltInFtpConfig();
        config1.setId(1L);
        config1.setUsername("user1");

        BuiltInFtpConfig config2 = new BuiltInFtpConfig();
        config2.setId(1L);
        config2.setUsername("user1");

        BuiltInFtpConfig config3 = new BuiltInFtpConfig();
        config3.setId(2L);
        config3.setUsername("user2");

        assertEquals(config1, config2);
        assertNotEquals(config1, config3);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testToString() {
        BuiltInFtpConfig config = new BuiltInFtpConfig();
        config.setId(1L);
        config.setUsername("testuser");
        config.setPort(2021);

        String str = config.toString();

        assertNotNull(str);
        assertTrue(str.contains("BuiltInFtpConfig"));
        assertTrue(str.contains("1"));
        assertTrue(str.contains("testuser"));
    }
}