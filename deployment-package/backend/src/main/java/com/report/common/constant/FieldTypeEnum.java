package com.report.common.constant;

import lombok.Getter;

@Getter
public enum FieldTypeEnum {

    STRING("STRING", "字符串"),
    INTEGER("INTEGER", "整数"),
    DECIMAL("DECIMAL", "小数"),
    DATE("DATE", "日期"),
    DATETIME("DATETIME", "日期时间");

    private final String code;
    private final String desc;

    FieldTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static FieldTypeEnum getByCode(String code) {
        for (FieldTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
