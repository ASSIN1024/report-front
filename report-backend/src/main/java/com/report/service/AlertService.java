package com.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.report.entity.AlertRecord;

public interface AlertService extends IService<AlertRecord> {

    void createAlert(Long reportConfigId, String fileName, String level, String type, String message);

    Page<AlertRecord> pageList(Integer pageNum, Integer pageSize, String level, String type, String status);

    void resolveAlert(Long alertId, String resolvedBy);
}
