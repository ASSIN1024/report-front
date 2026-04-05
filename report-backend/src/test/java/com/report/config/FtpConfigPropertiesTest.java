package com.report.config;

import org.junit.Test;

import static org.junit.Assert.*;

public class FtpConfigPropertiesTest {

    @Test
    public void testGettersAndSetters() {
        FtpConfigProperties props = new FtpConfigProperties();
        props.setEnabled(true);
        props.setPort(2121);
        props.setUsername("custom_user");
        props.setPassword("custom_pass");
        props.setRootDirectory("/custom/ftp");
        props.setMaxConnections(20);
        props.setIdleTimeout(600);
        props.setPassiveMode(false);
        props.setPassivePortStart(51000);
        props.setPassivePortEnd(51100);
        props.setMaxThreads(10);

        assertTrue(props.isEnabled());
        assertEquals(2121, props.getPort());
        assertEquals("custom_user", props.getUsername());
        assertEquals("custom_pass", props.getPassword());
        assertEquals("/custom/ftp", props.getRootDirectory());
        assertEquals(20, props.getMaxConnections());
        assertEquals(600, props.getIdleTimeout());
        assertFalse(props.isPassiveMode());
        assertEquals(51000, props.getPassivePortStart());
        assertEquals(51100, props.getPassivePortEnd());
        assertEquals(10, props.getMaxThreads());
    }

    @Test
    public void testDefaultValues() {
        FtpConfigProperties props = new FtpConfigProperties();

        assertFalse(props.isEnabled());
        assertEquals(2021, props.getPort());
        assertEquals("rpa_user", props.getUsername());
        assertEquals("rpa_password", props.getPassword());
        assertEquals("/data/ftp-root", props.getRootDirectory());
        assertEquals(10, props.getMaxConnections());
        assertEquals(300, props.getIdleTimeout());
        assertTrue(props.isPassiveMode());
        assertEquals(50000, props.getPassivePortStart());
        assertEquals(50100, props.getPassivePortEnd());
        assertEquals(5, props.getMaxThreads());
    }
}