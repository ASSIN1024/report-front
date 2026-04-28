package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

/**
 * 已处理文件记录实体
 */
@Data
@TableName("processed_file")
public class ProcessedFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 报表配置ID
     */
    private Long reportConfigId;

    /**
     * 文件名（不含路径）
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 数据分区日期
     */
    private String ptDt;

    /**
     * 处理状态：PROCESSED-已处理，FAILED-处理失败
     */
    private String status;

    /**
     * 关联任务ID
     */
    private Long taskId;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
