package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("ftp_config")
public class FtpConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String configName;

    private String host;

    private Integer port;

    private String username;

    private String password;

    private String scanPath;

    private String filePattern;

    private Integer scanInterval;

    private Integer status;

    private String remark;

    private String stagingDir;

    private String forUploadDir;

    private String archiveDir;

    private String errorDir;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
