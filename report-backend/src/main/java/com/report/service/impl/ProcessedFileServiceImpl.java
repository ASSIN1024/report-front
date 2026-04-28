package com.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.common.constant.ProcessedFileStatus;
import com.report.entity.ProcessedFile;
import com.report.mapper.ProcessedFileMapper;
import com.report.service.ProcessedFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 已处理文件服务实现类
 */
@Slf4j
@Service
public class ProcessedFileServiceImpl
        extends ServiceImpl<ProcessedFileMapper, ProcessedFile>
        implements ProcessedFileService {

    @Override
    public boolean isFileProcessed(Long reportConfigId, String fileName) {
        try {
            LambdaQueryWrapper<ProcessedFile> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProcessedFile::getReportConfigId, reportConfigId)
                   .eq(ProcessedFile::getFileName, fileName)
                   .eq(ProcessedFile::getStatus, ProcessedFileStatus.PROCESSED.getCode());

            long count = this.count(wrapper);
            log.debug("检查文件是否已处理: reportConfigId={}, fileName={}, count={}",
                      reportConfigId, fileName, count);
            return count > 0;
        } catch (Exception e) {
            log.error("检查文件处理状态异常，降级处理: reportConfigId={}, fileName={}, error={}",
                      reportConfigId, fileName, e.getMessage());
            return false;
        }
    }

    @Override
    public void markAsProcessed(Long reportConfigId, String fileName,
                                Long fileSize, Long taskId) {
        ProcessedFile record = new ProcessedFile();
        record.setReportConfigId(reportConfigId);
        record.setFileName(fileName);
        record.setFileSize(fileSize);
        record.setStatus(ProcessedFileStatus.PROCESSED.getCode());
        record.setTaskId(taskId);
        record.setPtDt(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date()));

        this.save(record);
        log.info("文件已标记为已处理: reportConfigId={}, fileName={}",
                 reportConfigId, fileName);
    }

    @Override
    public void markAsFailed(Long reportConfigId, String fileName,
                            Long taskId, String errorMessage) {
        ProcessedFile record = new ProcessedFile();
        record.setReportConfigId(reportConfigId);
        record.setFileName(fileName);
        record.setStatus(ProcessedFileStatus.FAILED.getCode());
        record.setTaskId(taskId);
        record.setErrorMessage(errorMessage);
        record.setPtDt(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date()));

        this.save(record);
        log.warn("文件已标记为处理失败: reportConfigId={}, fileName={}, error={}",
                 reportConfigId, fileName, errorMessage);
    }
}
