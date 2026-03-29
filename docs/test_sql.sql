-- 数据库集成测试脚本
-- 报表数据处理平台

USE report_db;

-- =============================================
-- 1. 测试FTP配置表
-- =============================================
SELECT '=== 1. FTP配置表 ===' AS '';
SELECT COUNT(*) AS ftp_config_count FROM ftp_config;
SELECT * FROM ftp_config WHERE deleted = 0;

-- =============================================
-- 2. 测试报表配置表
-- =============================================
SELECT '=== 2. 报表配置表 ===' AS '';
SELECT COUNT(*) AS report_config_count FROM report_config;
SELECT * FROM report_config WHERE deleted = 0;

-- =============================================
-- 3. 测试任务执行表
-- =============================================
SELECT '=== 3. 任务执行表 ===' AS '';
SELECT COUNT(*) AS task_count FROM task_execution;
SELECT id, task_type, task_name, status, total_rows, success_rows, failed_rows, start_time
FROM task_execution
WHERE deleted = 0
ORDER BY create_time DESC
LIMIT 10;

-- =============================================
-- 4. 测试任务日志表
-- =============================================
SELECT '=== 4. 任务日志表 ===' AS '';
SELECT COUNT(*) AS log_count FROM task_execution_log;
SELECT l.id, l.task_execution_id, l.log_level, l.log_message, l.create_time
FROM task_execution_log l
ORDER BY l.create_time DESC
LIMIT 20;

-- =============================================
-- 5. 测试系统配置表
-- =============================================
SELECT '=== 5. 系统配置表 ===' AS '';
SELECT COUNT(*) AS sys_config_count FROM sys_config;
SELECT * FROM sys_config WHERE deleted = 0;

-- =============================================
-- 6. 测试表结构完整性
-- =============================================
SELECT '=== 6. 表结构完整性 ===' AS '';
SELECT
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_KEY
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'report_db'
ORDER BY TABLE_NAME, ORDINAL_POSITION;

-- =============================================
-- 7. 测试外键关系
-- =============================================
SELECT '=== 7. 外键关系 ===' AS '';
-- 报表配置关联FTP配置
SELECT r.id, r.report_name, f.config_name AS ftp_config
FROM report_config r
LEFT JOIN ftp_config f ON r.ftp_config_id = f.id
WHERE r.deleted = 0;

-- 任务关联报表配置
SELECT t.id, t.task_name, r.report_name
FROM task_execution t
LEFT JOIN report_config r ON t.report_config_id = r.id
WHERE t.deleted = 0;

-- =============================================
-- 8. 测试索引
-- =============================================
SELECT '=== 8. 索引检查 ===' AS '';
SELECT DISTINCT
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    NON_UNIQUE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = 'report_db'
ORDER BY TABLE_NAME, INDEX_NAME;

-- =============================================
-- 9. 测试数据一致性
-- =============================================
SELECT '=== 9. 数据一致性检查 ===' AS '';

-- 检查无效的报表配置(关联不存在的FTP)
SELECT r.id, r.report_name, r.ftp_config_id
FROM report_config r
WHERE r.ftp_config_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM ftp_config f WHERE f.id = r.ftp_config_id AND f.deleted = 0);

-- 检查无效的任务(关联不存在的报表)
SELECT t.id, t.task_name, t.report_config_id
FROM task_execution t
WHERE t.report_config_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM report_config r WHERE r.id = t.report_config_id AND r.deleted = 0);

-- =============================================
-- 10. 插入测试数据
-- =============================================
SELECT '=== 10. 测试数据插入 ===' AS '';

-- 测试FTP配置
INSERT INTO ftp_config (id, config_name, host, port, username, password, scan_path, file_pattern, scan_interval, status, remark)
VALUES (999999, '测试FTP', '127.0.0.1', 21, 'test', 'test', '/test', '*.xlsx', 60, 1, '集成测试用')
ON DUPLICATE KEY UPDATE config_name = '测试FTP';

SELECT 'FTP配置插入成功' AS result;

-- 测试报表配置
INSERT INTO report_config (id, report_code, report_name, ftp_config_id, file_pattern, sheet_index, header_row, data_start_row, column_mapping, output_table, output_mode, status, remark)
VALUES (999999, 'TEST_REPORT', '测试报表', 999999, 'test_*.xlsx', 0, 0, 1, '[{"excelColumn":"A","fieldName":"col1","fieldType":"STRING"}]', 't_test_data', 'APPEND', 1, '集成测试用')
ON DUPLICATE KEY UPDATE report_name = '测试报表';

SELECT '报表配置插入成功' AS result;

-- 测试系统配置
INSERT INTO sys_config (id, config_key, config_value, config_type, description)
VALUES (999999, 'test_key', 'test_value', 'STRING', '集成测试用')
ON DUPLICATE KEY UPDATE config_value = 'test_value';

SELECT '系统配置插入成功' AS result;

-- 清理测试数据
DELETE FROM ftp_config WHERE id = 999999;
DELETE FROM report_config WHERE id = 999999;
DELETE FROM sys_config WHERE id = 999999;

SELECT '测试数据清理完成' AS result;

-- =============================================
-- 测试完成
-- =============================================
SELECT '=== 数据库集成测试完成 ===' AS '';
