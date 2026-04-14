package com.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.report.entity.SystemConfig;

public interface SystemConfigService extends IService<SystemConfig> {

    String getConfigValue(String configKey);

    void setConfigValue(String configKey, String configValue);
}
