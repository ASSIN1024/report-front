package com.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.report.entity.ProcessedFile;

/**
 * 已处理文件服务接口
 */
public interface ProcessedFileService extends IService<ProcessedFile> {

    /**
     * 检查文件是否已处理
     * @param reportConfigId 报表配置ID
     * @param fileName 文件名
     * @return true-已处理，false-未处理
     */
    boolean isFileProcessed(Long reportConfigId, String fileName);

    /**
     * 标记文件为已处理
     * @param reportConfigId 报表配置ID
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param taskId 任务ID
     */
    void markAsProcessed(Long reportConfigId, String fileName, Long fileSize, Long taskId);

    /**
     * 标记文件处理失败
     * @param reportConfigId 报表配置ID
     * @param fileName 文件名
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    void markAsFailed(Long reportConfigId, String fileName, Long taskId, String errorMessage);
}
