package com.report.packing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("alert_record")
public class AlertRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String alertType;
    private String fileName;
    private Long reportConfigId;
    private String reason;
    private String status;
    private Date resolveTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    public static final String TYPE_PARSE_ERROR = "PARSE_ERROR";
    public static final String TYPE_MAPPING_ERROR = "MAPPING_ERROR";
    public static final String TYPE_PACKING_ERROR = "PACKING_ERROR";
    public static final String TYPE_CONSUMPTION_TIMEOUT = "CONSUMPTION_TIMEOUT";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_IGNORED = "IGNORED";
}