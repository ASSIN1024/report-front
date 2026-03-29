package com.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.report.entity.FtpConfig;

import java.util.List;

public interface FtpConfigService extends IService<FtpConfig> {

    Page<FtpConfig> pageList(Integer pageNum, Integer pageSize, String configName, Integer status);

    List<FtpConfig> listEnabled();

    FtpConfig getByConfigName(String configName);

    boolean testConnection(Long id);
}
