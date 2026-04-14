package com.report.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class ColumnMappingValidator {

    private static final List<String> VALID_FIELD_TYPES = Arrays.asList(
        "STRING", "INTEGER", "DECIMAL", "DATE", "DATETIME"
    );

    public static class ValidationError {
        private int line;
        private String message;
        private String suggestion;

        public ValidationError(int line, String message, String suggestion) {
            this.line = line;
            this.message = message;
            this.suggestion = suggestion;
        }

        public int getLine() { return line; }
        public String getMessage() { return message; }
        public String getSuggestion() { return suggestion; }
    }

    public static List<ValidationError> validate(String json) {
        List<ValidationError> errors = new ArrayList<>();

        if (StrUtil.isBlank(json)) {
            errors.add(new ValidationError(0, "JSON不能为空", null));
            return errors;
        }

        try {
            JSONArray array = JSONUtil.parseArray(json);

            for (int i = 0; i < array.size(); i++) {
                int lineNum = i + 1;
                JSONObject obj = array.getJSONObject(i);

                if (obj.getStr("excelColumn") == null) {
                    errors.add(new ValidationError(lineNum, "缺少excelColumn字段", "添加 \"excelColumn\": \"A\""));
                }

                if (obj.getStr("fieldName") == null) {
                    errors.add(new ValidationError(lineNum, "缺少fieldName字段", "添加 \"fieldName\": \"column_name\""));
                }

                String fieldType = obj.getStr("fieldType");
                if (fieldType == null) {
                    errors.add(new ValidationError(lineNum, "缺少fieldType字段", "添加 \"fieldType\": \"STRING\""));
                } else if (!VALID_FIELD_TYPES.contains(fieldType)) {
                    errors.add(new ValidationError(lineNum, "无效的字段类型: " + fieldType,
                        "有效类型: " + String.join(", ", VALID_FIELD_TYPES)));
                }

                if (obj.containsKey("cleanRules") && obj.get("cleanRules") != null) {
                    JSONArray rules = obj.getJSONArray("cleanRules");
                    for (int j = 0; j < rules.size(); j++) {
                        JSONObject rule = rules.getJSONObject(j);
                        if (rule.getStr("pattern") == null) {
                            errors.add(new ValidationError(lineNum, "第" + (j+1) + "条规则缺少pattern字段", null));
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("JSON解析失败", e);
            errors.add(new ValidationError(0, "JSON格式错误: " + e.getMessage(), "检查JSON语法"));
        }

        return errors;
    }

    public static int countMappings(String json) {
        try {
            JSONArray array = JSONUtil.parseArray(json);
            return array.size();
        } catch (Exception e) {
            return 0;
        }
    }
}
