-- 报表配置表新增字段：跳过列数和日期提取规则
-- 日期: 2026-04-06
-- 功能: 智能分区日期提取 & 列跳过功能

ALTER TABLE report_config
ADD COLUMN skip_columns INT DEFAULT 0 COMMENT '跳过前N列' AFTER data_start_row,
ADD COLUMN date_extract_pattern VARCHAR(50) NULL COMMENT '日期提取规则' AFTER skip_columns;
