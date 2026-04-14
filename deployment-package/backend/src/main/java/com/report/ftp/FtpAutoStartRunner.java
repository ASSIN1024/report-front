package com.report.ftp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FtpAutoStartRunner implements CommandLineRunner {

    private final FtpBuiltInProperties ftpProperties;
    private final EmbeddedFtpServer embeddedFtpServer;

    public FtpAutoStartRunner(FtpBuiltInProperties ftpProperties, EmbeddedFtpServer embeddedFtpServer) {
        this.ftpProperties = ftpProperties;
        this.embeddedFtpServer = embeddedFtpServer;
    }

    @Override
    public void run(String... args) {
        if (!ftpProperties.isEnabled()) {
            log.info("内置FTP服务未启用，跳过启动");
            return;
        }

        log.info("正在启动内置FTP服务，配置: port={}, username={}, rootDirectory={}",
                ftpProperties.getPort(), ftpProperties.getUsername(), ftpProperties.getRootDirectory());

        try {
            boolean started = embeddedFtpServer.startWithProperties();
            if (started) {
                log.info("内置FTP服务启动成功，监听端口: {}", ftpProperties.getPort());
            } else {
                log.warn("内置FTP服务启动失败，请检查配置和端口占用情况");
            }
        } catch (Exception e) {
            log.error("启动内置FTP服务异常", e);
        }
    }
}