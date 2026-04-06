package com.report.ftp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ftp.built-in")
public class FtpBuiltInProperties {

    private boolean enabled = false;
    private int port = 2121;
    private String username = "rpa_user";
    private String password = "rpa_password";
    private String rootDirectory = "/data/ftp-root";
    private int idleTimeout = 300;
    private int maxConnections = 10;
}