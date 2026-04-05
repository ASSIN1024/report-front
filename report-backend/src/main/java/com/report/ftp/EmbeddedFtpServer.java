package com.report.ftp;

import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletContext;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class EmbeddedFtpServer {

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    private BuiltInFtpConfigService builtInFtpConfigService;

    private FtpServer ftpServer;

    public boolean start() {
        if (running.get()) {
            log.warn("FTP服务已在运行");
            return true;
        }

        BuiltInFtpConfig config = builtInFtpConfigService.getConfig();
        if (!config.getEnabled()) {
            log.warn("内置FTP未启用");
            return false;
        }

        try {
            FtpServerFactory serverFactory = new FtpServerFactory();
            ListenerFactory listenerFactory = new ListenerFactory();

            listenerFactory.setPort(config.getPort());
            listenerFactory.setIdleTimeout(config.getIdleTimeout());

            PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
            File userFile = File.createTempFile("ftp-users", ".properties");
            userFile.deleteOnExit();
            userManagerFactory.setFile(userFile);

            UserManager userManager = userManagerFactory.createUserManager();

            BaseUser user = new BaseUser();
            user.setName(config.getUsername());
            user.setPassword(config.getPassword());
            user.setHomeDirectory(config.getRootDirectory());

            List<Authority> authorities = new ArrayList<>();
            authorities.add(new WritePermission());
            user.setAuthorities(authorities);

            userManager.save(user);

            Map<String, Ftplet> ftplets = new HashMap<>();
            ftplets.put("ftpLogging", new Ftplet() {
                @Override
                public void init(FtpletContext ftpletContext) {
                }

                @Override
                public void destroy() {
                }

                @Override
                public FtpletResult beforeCommand(FtpSession session, FtpRequest request) {
                    log.info("[FTP-ACCESS] {} - {} - {} - {}",
                        session.getClientAddress(),
                        session.getUser().getName(),
                        request.getCommand(),
                        request.getArgument());
                    return FtpletResult.DEFAULT;
                }

                @Override
                public FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) {
                    return FtpletResult.DEFAULT;
                }

                @Override
                public FtpletResult onConnect(FtpSession session) {
                    log.info("[FTP-CONNECT] {}", session.getClientAddress());
                    return FtpletResult.DEFAULT;
                }

                @Override
                public FtpletResult onDisconnect(FtpSession session) {
                    log.info("[FTP-DISCONNECT] {}", session.getClientAddress());
                    return FtpletResult.DEFAULT;
                }
            });

            serverFactory.setUserManager(userManager);
            serverFactory.setFtplets(ftplets);
            serverFactory.addListener("default", listenerFactory.createListener());

            ftpServer = serverFactory.createServer();
            ftpServer.start();

            running.set(true);
            log.info("内置FTP服务已启动，端口: {}", config.getPort());
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