package com.report.packing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.report.packing.entity.PackingConfig;
import java.util.Map;

public interface PackingConfigService extends IService<PackingConfig> {
    String getStringValue(String key, String defaultValue);
    Long getLongValue(String key, Long defaultValue);
    Integer getIntValue(String key, Integer defaultValue);
    Map<String, String> getAllConfigs();
    void updateConfig(String key, String value);
}