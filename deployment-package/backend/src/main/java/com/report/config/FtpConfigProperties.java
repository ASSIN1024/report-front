package com.report.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ftp.built-in")
public class FtpConfigProperties {

    private boolean enabled = false;

    private int port = 2021;

    private String username = "rpa_user";

    private String password = "rpa_password";

    private String rootDirectory = "/data/ftp-root";

    private int maxConnections = 10;

    private int idleTimeout = 300;

    private boolean passiveMode = true;

    private int passivePortStart = 50000;

    private int passivePortEnd = 50100;

    private int maxThreads = 5;
}