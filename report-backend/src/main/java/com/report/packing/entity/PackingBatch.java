package com.report.packing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("packing_batch")
public class PackingBatch implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String batchNo;
    private String status;
    private Long totalSize;
    private Integer fileCount;
    private String forUploadPath;
    private String doneDirPath;
    private Date startTime;
    private Date endTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_UPLOADING = "UPLOADING";
    public static final String STATUS_CONSUMING = "CONSUMING";
    public static final String STATUS_DONE = "DONE";
}