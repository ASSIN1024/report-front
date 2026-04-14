package com.report.common.constant;

/**
 * 已处理文件状态枚举
 */
public enum ProcessedFileStatus {

    PROCESSED("PROCESSED", "已处理"),
    FAILED("FAILED", "处理失败"),
    SKIPPED("SKIPPED", "已跳过");

    private final String code;
    private final String desc;

    ProcessedFileStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
