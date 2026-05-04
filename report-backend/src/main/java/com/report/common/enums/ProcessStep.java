package com.report.common.enums;

public enum ProcessStep {
    DIR_CREATING("目录创建中"),
    DIR_CREATED("目录创建完成"),
    FILE_UPLOADING("文件上传中"),
    FILE_UPLOADED("文件上传完成"),
    FILE_DETECTED("文件被检测到"),
    PARSING("解析中"),
    PARSED("解析完成"),
    PROCESSING("处理中"),
    PROCESSED("处理完成"),
    EXCEPTION("异常");

    private final String description;

    ProcessStep(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}