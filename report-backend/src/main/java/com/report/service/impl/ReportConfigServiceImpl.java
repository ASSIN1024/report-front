package com.report.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.entity.ReportConfig;
import com.report.entity.dto.ColumnMapping;
import com.report.entity.dto.ReportConfigDTO;
import com.report.mapper.ReportConfigMapper;
import com.report.service.ReportConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportConfigServiceImpl extends ServiceImpl<ReportConfigMapper, ReportConfig> implements ReportConfigService {

    @Override
    public Page<ReportConfigDTO> pageList(Integer pageNum, Integer pageSize, String reportName, Integer status) {
        LambdaQueryWrapper<ReportConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(reportName)) {
            wrapper.like(ReportConfig::getReportName, reportName);
        }
        if (status != null) {
            wrapper.eq(ReportConfig::getStatus, status);
        }
        wrapper.orderByDesc(ReportConfig::getCreateTime);
        Page<ReportConfig> page = page(new Page<>(pageNum, pageSize), wrapper);

        Page<ReportConfigDTO> dtoPage = new Page<>();
        dtoPage.setCurrent(page.getCurrent());
        dtoPage.setSize(page.getSize());
        dtoPage.setTotal(page.getTotal());
        dtoPage.setRecords(page.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
        return dtoPage;
    }

    @Override
    public List<ReportConfig> listEnabled() {
        LambdaQueryWrapper<ReportConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportConfig::getStatus, 1);
        return list(wrapper);
    }

    @Override
    public ReportConfig getByReportCode(String reportCode) {
        LambdaQueryWrapper<ReportConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportConfig::getReportCode, reportCode);
        return getOne(wrapper);
    }

    @Override
    public ReportConfigDTO getDetailById(Long id) {
        ReportConfig config = getById(id);
        if (config == null) {
            return null;
        }
        return convertToDTO(config);
    }

    private ReportConfigDTO convertToDTO(ReportConfig config) {
        ReportConfigDTO dto = new ReportConfigDTO();
        dto.setId(config.getId());
        dto.setReportCode(config.getReportCode());
        dto.setReportName(config.getReportName());
        dto.setFtpConfigId(config.getFtpConfigId());
        dto.setScanPath(config.getScanPath());
        dto.setFilePattern(config.getFilePattern());
        dto.setSheetIndex(config.getSheetIndex());
        dto.setHeaderRow(config.getHeaderRow());
        dto.setDataStartRow(config.getDataStartRow());
        dto.setSkipColumns(config.getSkipColumns());
        dto.setDateExtractPattern(config.getDateExtractPattern());
        dto.setOutputTable(config.getOutputTable());
        dto.setStartRow(config.getStartRow());
        dto.setStartCol(config.getStartCol());
        dto.setMappingMode(config.getMappingMode());
        dto.setDuplicateColStrategy(config.getDuplicateColStrategy());
        dto.setOdsBackupEnabled(config.getOdsBackupEnabled());
        dto.setOdsTableName(config.getOdsTableName());
        dto.setTargetTableType(config.getTargetTableType());
        dto.setTargetDbName(config.getTargetDbName());
        dto.setIsOverseas(config.getIsOverseas());
        dto.setLoadMode(config.getLoadMode());
        dto.setFieldTypeJson(config.getFieldTypeJson());
        dto.setSparkExecutorNum(config.getSparkExecutorNum());
        dto.setSparkExecutorCores(config.getSparkExecutorCores());
        dto.setSparkExecutorMemory(config.getSparkExecutorMemory());
        dto.setSparkDriverNum(config.getSparkDriverNum());
        dto.setSparkDriverMemory(config.getSparkDriverMemory());
        dto.setPartitionInfo(config.getPartitionInfo());
        dto.setStatus(config.getStatus());
        dto.setRemark(config.getRemark());

        if (StringUtils.hasText(config.getColumnMapping())) {
            List<ColumnMapping> mappings = JSONUtil.toList(config.getColumnMapping(), ColumnMapping.class);
            dto.setColumnMappings(mappings);
        }

        dto.setFtpConfigName("内置FTP");

        return dto;
    }
}
