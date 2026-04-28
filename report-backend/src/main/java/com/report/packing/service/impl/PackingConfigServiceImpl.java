package com.report.packing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.packing.entity.PackingConfig;
import com.report.packing.mapper.PackingConfigMapper;
import com.report.packing.service.PackingConfigService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Primary
public class PackingConfigServiceImpl extends ServiceImpl<PackingConfigMapper, PackingConfig> implements PackingConfigService {

    @Override
    public String getStringValue(String key, String defaultValue) {
        PackingConfig config = this.getByKey(key);
        return config != null ? config.getConfigValue() : defaultValue;
    }

    @Override
    public Long getLongValue(String key, Long defaultValue) {
        String value = getStringValue(key, null);
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Integer getIntValue(String key, Integer defaultValue) {
        String value = getStringValue(key, null);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public Map<String, String> getAllConfigs() {
        Map<String, String> configs = new HashMap<>();
        this.list().forEach(c -> configs.put(c.getConfigKey(), c.getConfigValue()));
        return configs;
    }

    @Override
    public void updateConfig(String key, String value) {
        PackingConfig config = getByKey(key);
        if (config != null) {
            config.setConfigValue(value);
            this.updateById(config);
        }
    }

    private PackingConfig getByKey(String key) {
        return this.getOne(new QueryWrapper<PackingConfig>().eq("config_key", key));
    }
}