package com.report.util;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class FileNameDateExtractor {

    private FileNameDateExtractor() {}

    private static final List<DateFormatPattern> SUPPORTED_PATTERNS = Arrays.asList(
        new DateFormatPattern(
            ".*?(\\d{4})-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01]).*",
            "yyyy-MM-dd"
        ),
        new DateFormatPattern(
            ".*?(\\d{4})(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])(?:_(?:[01][0-9]|2[0-3])[0-5][0-9])?.*",
            "yyyyMMdd"
        )
    );

    public static LocalDate extractDate(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return null;
        }

        String baseName = getBaseName(fileName);

        for (DateFormatPattern pattern : SUPPORTED_PATTERNS) {
            LocalDate date = tryExtract(baseName, pattern);
            if (date != null) {
                log.debug("从文件名提取日期: {} -> {}", fileName, date);
                return date;
            }
        }

        log.debug("文件名未包含可识别的日期: {}", fileName);
        return null;
    }

    private static String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }

    private static LocalDate tryExtract(String text, DateFormatPattern pattern) {
        try {
            Pattern regex = Pattern.compile(pattern.getRegex());
            Matcher matcher = regex.matcher(text);

            if (matcher.find()) {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));

                if (isValidDate(year, month, day)) {
                    return LocalDate.of(year, month, day);
                }
            }
        } catch (Exception e) {
            log.warn("日期提取异常: pattern={}, error={}", pattern.getFormat(), e.getMessage());
        }

        return null;
    }

    private static boolean isValidDate(int year, int month, int day) {
        if (month < 1 || month > 12) {
            return false;
        }
        if (day < 1 || day > 31) {
            return false;
        }
        try {
            LocalDate.of(year, month, day);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    static class DateFormatPattern {
        private String regex;
        private String format;
    }
}
