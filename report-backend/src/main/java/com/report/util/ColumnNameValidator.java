package com.report.util;

import java.util.regex.Pattern;

public class ColumnNameValidator {

    private static final Pattern VALID_COLUMN_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final Pattern VALID_COLUMN_NAME_WITH_UNDERSCORE_ONLY = Pattern.compile("^_+$");
    private static final Pattern VALID_EXCEL_COLUMN_PATTERN = Pattern.compile("^[A-Z]+$");

    private static final int MAX_COLUMN_NAME_LENGTH = 64;

    public static boolean isValidColumnName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        String trimmedName = name.trim();

        if (trimmedName.length() > MAX_COLUMN_NAME_LENGTH) {
            return false;
        }

        if (VALID_COLUMN_NAME_PATTERN.matcher(trimmedName).matches()) {
            if (VALID_COLUMN_NAME_WITH_UNDERSCORE_ONLY.matcher(trimmedName).matches()) {
                return false;
            }
            return true;
        }

        return false;
    }

    public static boolean isValidExcelColumnName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        String trimmedName = name.trim();

        if (trimmedName.length() > MAX_COLUMN_NAME_LENGTH) {
            return false;
        }

        return VALID_EXCEL_COLUMN_PATTERN.matcher(trimmedName).matches();
    }

    public static String getValidationMessage(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "字段名不能为空";
        }

        String trimmedName = name.trim();

        if (trimmedName.length() > MAX_COLUMN_NAME_LENGTH) {
            return "字段名不能超过" + MAX_COLUMN_NAME_LENGTH + "个字符";
        }

        if (VALID_COLUMN_NAME_WITH_UNDERSCORE_ONLY.matcher(trimmedName).matches()) {
            return "字段名不能为纯下划线";
        }

        if (!VALID_COLUMN_NAME_PATTERN.matcher(trimmedName).matches()) {
            return "字段名只能包含英文字母、数字和下划线，且必须以字母或下划线开头";
        }

        return null;
    }

    public static String getExcelColumnValidationMessage(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Excel列名不能为空";
        }

        String trimmedName = name.trim();

        if (!VALID_EXCEL_COLUMN_PATTERN.matcher(trimmedName).matches()) {
            return "Excel列名只能是英文字母（A-Z），如A、B、C等";
        }

        return null;
    }
}