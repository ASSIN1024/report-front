package com.report.ftp;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("built_in_ftp_config")
public class BuiltInFtpConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Boolean enabled;

    private Integer port;

    private String username;

    private String password;

    private String rootDirectory;

    private Integer maxConnections;

    private Integer idleTimeout;

    private Boolean passiveMode;

    private Integer passivePortStart;

    private Integer passivePortEnd;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}