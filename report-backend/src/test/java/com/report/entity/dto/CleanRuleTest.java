package com.report.entity.dto;

import org.junit.Test;

import static org.junit.Assert.*;

public class CleanRuleTest {

    @Test
    public void testCleanRuleConstruction() {
        CleanRule rule = new CleanRule();
        rule.setPattern("-");
        rule.setReplace("0");

        assertEquals("-", rule.getPattern());
        assertEquals("0", rule.getReplace());
    }

    @Test
    public void testCleanRuleEquals() {
        CleanRule rule1 = new CleanRule();
        rule1.setPattern("-");
        rule1.setReplace("0");

        CleanRule rule2 = new CleanRule();
        rule2.setPattern("-");
        rule2.setReplace("0");

        assertEquals(rule1.getPattern(), rule2.getPattern());
        assertEquals(rule1.getReplace(), rule2.getReplace());
    }

    @Test
    public void testCleanRuleWithEmptyReplace() {
        CleanRule rule = new CleanRule();
        rule.setPattern("N/A");
        rule.setReplace("");

        assertEquals("N/A", rule.getPattern());
        assertEquals("", rule.getReplace());
    }

    @Test
    public void testCleanRuleNullReplace() {
        CleanRule rule = new CleanRule();
        rule.setPattern("-");
        rule.setReplace(null);

        assertEquals("-", rule.getPattern());
        assertNull(rule.getReplace());
    }
}
