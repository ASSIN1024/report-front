package com.report.ftp.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigMapper;
import com.report.ftp.BuiltInFtpConfigService;
import org.springframework.stereotype.Service;

@Service
public class BuiltInFtpConfigServiceImpl extends ServiceImpl<BuiltInFtpConfigMapper, BuiltInFtpConfig>
        implements BuiltInFtpConfigService {

    @Override
    public BuiltInFtpConfig getConfig() {
        BuiltInFtpConfig config = this.getById(1L);
        if (config == null) {
            config = new BuiltInFtpConfig();
            config.setId(1L);
            config.setEnabled(false);
            config.setPort(2021);
            config.setUsername("rpa_user");
            config.setPassword("rpa_password");
            config.setRootDirectory("/data/ftp-root");
            config.setMaxConnections(10);
            config.setIdleTimeout(300);
            config.setPassiveMode(true);
            config.setPassivePortStart(50000);
            config.setPassivePortEnd(50100);
            this.save(config);
        }
        return config;
    }

    @Override
    public void updateConfig(BuiltInFtpConfig config) {
        config.setId(1L);
        this.updateById(config);
    }

    @Override
    public boolean isServerRunning() {
        return false;
    }
}