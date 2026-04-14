package com.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.report.entity.ReportConfig;
import com.report.entity.dto.ReportConfigDTO;

import java.util.List;

public interface ReportConfigService extends IService<ReportConfig> {

    Page<ReportConfigDTO> pageList(Integer pageNum, Integer pageSize, String reportName, Integer status);

    List<ReportConfig> listEnabled();

    ReportConfig getByReportCode(String reportCode);

    ReportConfigDTO getDetailById(Long id);
}
