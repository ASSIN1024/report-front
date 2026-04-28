package com.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.report.entity.BatchRecord;

public interface BatchService extends IService<BatchRecord> {

    Page<BatchRecord> pageList(Integer pageNum, Integer pageSize, String status);

    void deliverZipIfReady(Long ftpConfigId);
}
