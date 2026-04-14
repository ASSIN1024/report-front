package com.report.ftp;

import com.baomidou.mybatisplus.extension.service.IService;

public interface BuiltInFtpConfigService extends IService<BuiltInFtpConfig> {

    BuiltInFtpConfig getConfig();

    void updateConfig(BuiltInFtpConfig config);

    boolean isServerRunning();
}