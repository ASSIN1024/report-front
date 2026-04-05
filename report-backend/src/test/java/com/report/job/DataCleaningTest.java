package com.report.job;

import cn.hutool.core.util.StrUtil;
import com.report.entity.dto.CleanRule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class DataCleaningTest {

    @Test
    public void testApplyCleanRule_DashToZero() {
        List<CleanRule> rules = Arrays.asList(createRule("-", "0"));
        String result = applyCleanRules("-", rules);
        assertEquals("0", result);
    }

    @Test
    public void testApplyCleanRule_NAToEmpty() {
        List<CleanRule> rules = Arrays.asList(createRule("N/A", ""));
        String result = applyCleanRules("N/A", rules);
        assertEquals("", result);
    }

    @Test
    public void testApplyCleanRule_NoMatch() {
        List<CleanRule> rules = Arrays.asList(createRule("-", "0"));
        String result = applyCleanRules("123", rules);
        assertEquals("123", result);
    }

    @Test
    public void testApplyCleanRule_MultipleRules() {
        List<CleanRule> rules = Arrays.asList(
            createRule("-", "0"),
            createRule("N/A", "")
        );

        assertEquals("0", applyCleanRules("-", rules));
        assertEquals("", applyCleanRules("N/A", rules));
        assertEquals("123", applyCleanRules("123", rules));
    }

    @Test
    public void testApplyCleanRule_EmptyRules() {
        String result = applyCleanRules("-", null);
        assertEquals("-", result);
    }

    @Test
    public void testApplyCleanRule_NullPattern() {
        List<CleanRule> rules = Arrays.asList(createRule(null, "0"));
        String result = applyCleanRules("-", rules);
        assertEquals("-", result);
    }

    @Test
    public void testConvertValueWithCleanRule_Decimal() {
        List<CleanRule> rules = Arrays.asList(createRule("-", "0"));
        Object value = "-";
        Object result = convertValueWithCleanRule(value, "DECIMAL", null, rules);
        assertEquals(new BigDecimal("0"), result);
    }

    @Test
    public void testConvertValueWithCleanRule_Integer() {
        List<CleanRule> rules = Arrays.asList(createRule("-", "0"));
        Object value = "-";
        Object result = convertValueWithCleanRule(value, "INTEGER", null, rules);
        assertEquals(0L, result);
    }

    @Test
    public void testConvertValueWithCleanRule_CleanRuleAppliesFirst() {
        List<CleanRule> rules = Arrays.asList(createRule("-", "100"));
        Object value = "-";
        Object result = convertValueWithCleanRule(value, "INTEGER", null, rules);
        assertEquals(100L, result);
    }

    @Test
    public void testConvertValueWithCleanRule_StringNoChange() {
        List<CleanRule> rules = Arrays.asList(createRule("-", "0"));
        Object value = "hello";
        Object result = convertValueWithCleanRule(value, "STRING", null, rules);
        assertEquals("hello", result);
    }

    @Test
    public void testCleanRuleScenario_DashInDecimalField() {
        List<CleanRule> rules = Arrays.asList(createRule("-", "0"));

        Object value = "-";
        String cleaned = applyCleanRules(String.valueOf(value), rules);
        assertEquals("0", cleaned);

        BigDecimal decimalResult = new BigDecimal(cleaned);
        assertEquals(new BigDecimal("0"), decimalResult);
    }

    private CleanRule createRule(String pattern, String replace) {
        CleanRule rule = new CleanRule();
        rule.setPattern(pattern);
        rule.setReplace(replace);
        return rule;
    }

    private String applyCleanRules(String value, List<CleanRule> rules) {
        if (value == null) {
            return null;
        }
        String strValue = value.toString().trim();
        if (rules != null && !rules.isEmpty()) {
            for (CleanRule rule : rules) {
                if (rule.getPattern() != null && strValue.equals(rule.getPattern())) {
                    strValue = rule.getReplace() != null ? rule.getReplace() : "";
                }
            }
        }
        return strValue;
    }

    private Object convertValueWithCleanRule(Object value, String fieldType, String dateFormat, List<CleanRule> cleanRules) {
        if (value == null) {
            return null;
        }
        if (StrUtil.isBlank(String.valueOf(value))) {
            return null;
        }
        String strValue = String.valueOf(value).trim();

        if (cleanRules != null && !cleanRules.isEmpty()) {
            for (CleanRule rule : cleanRules) {
                if (rule.getPattern() != null && strValue.equals(rule.getPattern())) {
                    strValue = rule.getReplace() != null ? rule.getReplace() : "";
                    value = strValue;
                }
            }
        }

        if (StrUtil.isBlank(strValue)) {
            return null;
        }

        try {
            switch (fieldType) {
                case "STRING":
                    return strValue;
                case "INTEGER":
                    if (strValue.contains(".")) {
                        return new BigDecimal(strValue).intValue();
                    }
                    return Long.parseLong(strValue);
                case "DECIMAL":
                    return new BigDecimal(strValue);
                default:
                    return strValue;
            }
        } catch (Exception e) {
            return strValue;
        }
    }
}
