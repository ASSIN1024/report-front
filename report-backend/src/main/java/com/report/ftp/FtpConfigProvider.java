package com.report.ftp;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FtpConfigProvider {

    @Autowired
    private FtpBuiltInProperties ftpProperties;

    public BuiltInFtpConfig getConfig() {
        if (ftpProperties == null) {
            return null;
        }

        BuiltInFtpConfig config = new BuiltInFtpConfig();
        config.setId(1L);
        config.setEnabled(ftpProperties.isEnabled());
        config.setPort(ftpProperties.getPort());
        config.setUsername(ftpProperties.getUsername());
        config.setPassword(ftpProperties.getPassword());
        config.setRootDirectory(ftpProperties.getRootDirectory());
        config.setMaxConnections(ftpProperties.getMaxConnections());
        config.setIdleTimeout(ftpProperties.getIdleTimeout());

        return config;
    }
}
