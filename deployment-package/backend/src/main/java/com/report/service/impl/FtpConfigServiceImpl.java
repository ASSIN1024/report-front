package com.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.common.constant.ErrorCode;
import com.report.common.exception.BusinessException;
import com.report.entity.FtpConfig;
import com.report.mapper.FtpConfigMapper;
import com.report.service.FtpConfigService;
import com.report.util.FtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class FtpConfigServiceImpl extends ServiceImpl<FtpConfigMapper, FtpConfig> implements FtpConfigService {

    @Override
    public Page<FtpConfig> pageList(Integer pageNum, Integer pageSize, String configName, Integer status) {
        LambdaQueryWrapper<FtpConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(configName)) {
            wrapper.like(FtpConfig::getConfigName, configName);
        }
        if (status != null) {
            wrapper.eq(FtpConfig::getStatus, status);
        }
        wrapper.orderByDesc(FtpConfig::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<FtpConfig> listEnabled() {
        LambdaQueryWrapper<FtpConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FtpConfig::getStatus, 1);
        return list(wrapper);
    }

    @Override
    public FtpConfig getByConfigName(String configName) {
        LambdaQueryWrapper<FtpConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FtpConfig::getConfigName, configName);
        return getOne(wrapper);
    }

    @Override
    public boolean testConnection(Long id) {
        FtpConfig config = getById(id);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "FTP配置不存在");
        }
        try {
            return FtpUtil.testConnection(config);
        } catch (Exception e) {
            log.error("FTP连接测试失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FTP_CONNECT_ERROR, "FTP连接测试失败: " + e.getMessage());
        }
    }
}
