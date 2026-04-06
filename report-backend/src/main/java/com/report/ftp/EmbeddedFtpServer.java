package com.report.ftp;

import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class EmbeddedFtpServer {

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    private BuiltInFtpConfigMapper builtInFtpConfigMapper;

    private FtpBuiltInProperties properties;

    private FtpServer ftpServer;

    @Autowired
    public void setProperties(FtpBuiltInProperties properties) {
        this.properties = properties;
    }

    public boolean start() {
        if (running.get()) {
            log.warn("FTP服务已在运行");
            return true;
        }

        BuiltInFtpConfig config = builtInFtpConfigMapper.getConfig();
        if (config == null) {
            log.warn("内置FTP配置不存在");
            return false;
        }
        if (!config.getEnabled()) {
            log.warn("内置FTP未启用");
            return false;
        }

        return startWithProperties();
    }

    public boolean startWithProperties() {
        if (running.get()) {
            log.warn("FTP服务已在运行");
            return true;
        }

        if (properties == null) {
            log.error("FTP配置未加载");
            return false;
        }

        try {
            File rootDir = new File(properties.getRootDirectory());
            if (!rootDir.exists()) {
                rootDir.mkdirs();
            }

            File userFile = new File(rootDir, "ftp-users.properties");
            userFile.createNewFile();

            FtpServerFactory serverFactory = new FtpServerFactory();
            ListenerFactory listenerFactory = new ListenerFactory();

            listenerFactory.setPort(properties.getPort());
            listenerFactory.setIdleTimeout(properties.getIdleTimeout() * 1000);

            PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
            userManagerFactory.setFile(userFile);

            UserManager userManager = userManagerFactory.createUserManager();

            BaseUser user = new BaseUser();
            user.setName(properties.getUsername());
            user.setPassword(properties.getPassword());
            user.setHomeDirectory(rootDir.getAbsolutePath());

            List<Authority> authorities = new ArrayList<>();
            authorities.add(new WritePermission());
            user.setAuthorities(authorities);

            userManager.save(user);

            serverFactory.setUserManager(userManager);
            serverFactory.addListener("default", listenerFactory.createListener());

            ftpServer = serverFactory.createServer();
            ftpServer.start();

            running.set(true);
            log.info("内置FTP服务已启动，端口: {}, 用户: {}, 目录: {}",
                    properties.getPort(), properties.getUsername(), rootDir.getAbsolutePath());
            return true;

        } catch (Exception e) {
            log.error("启动内置FTP服务失败", e);
            return false;
        }
    }

    public void stop() {
        if (ftpServer != null && !ftpServer.isStopped()) {
            ftpServer.stop();
            running.set(false);
            log.info("内置FTP服务已停止");
        }
    }

    public boolean isRunning() {
        return running.get() && ftpServer != null && !ftpServer.isStopped();
    }

    public int getConnectedClients() {
        return 0;
    }
}