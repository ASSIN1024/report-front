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
            "(\\d{4})-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])",
            "yyyy-MM-dd"
        ),
        new DateFormatPattern(
            "(\\d{4})(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])",
            "yyyyMMdd"
        )
    );

    public static LocalDate extractDate(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return null;
        }

        String baseName = getBaseName(fileName);
        LocalDate bestDate = null;

        for (DateFormatPattern pattern : SUPPORTED_PATTERNS) {
            LocalDate date = tryExtractAll(baseName, pattern);
            if (date != null) {
                if (bestDate == null || date.isAfter(bestDate)) {
                    bestDate = date;
                }
            }
        }

        if (bestDate != null) {
            log.debug("从文件名提取日期: {} -> {}", fileName, bestDate);
            return bestDate;
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

    private static LocalDate tryExtractAll(String text, DateFormatPattern pattern) {
        try {
            Pattern regex = Pattern.compile(pattern.getRegex());
            Matcher matcher = regex.matcher(text);

            LocalDate validDate = null;
            while (matcher.find()) {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));

                if (isValidDate(year, month, day)) {
                    LocalDate date = LocalDate.of(year, month, day);
                    if (validDate == null || date.isAfter(validDate)) {
                        validDate = date;
                    }
                }
            }
            return validDate;
        } catch (Exception e) {
            log.warn("日期提取异常: pattern={}, error={}", pattern.getFormat(), e.getMessage());
        }

        return null;
    }

    private static boolean isValidDate(int year, int month, int day) {
        if (year < 2020 || year > 2100) {
            return false;
        }
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
