-- FTP配置简化重构数据迁移脚本
-- 执行时间: 2026-04-29
-- 关联任务: H-FTP-REFACTOR

-- =============================================================================
-- 迁移步骤
-- 1. 新增 scan_path 字段
-- 2. 迁移现有数据（如果有外部FTP配置，将其scan_path迁移过来）
-- 3. 将所有报表的ftp_config_id设置为-1（内置FTP）
-- 4. 删除外部FTP配置表（确认迁移完成后执行）
-- =============================================================================

-- 1. 新增 scan_path 字段
ALTER TABLE report_config ADD COLUMN IF NOT EXISTS scan_path VARCHAR(200) DEFAULT '/upload' COMMENT '扫描路径' AFTER ftp_config_id;

-- 2. 迁移现有数据
-- 如果有外部FTP配置关联，将其scan_path迁移过来
-- 注意：这里假设外部FTP已经被废弃，scan_path统一使用默认值 /upload
-- 如果需要保留原有的scan_path配置，需要单独处理：
-- UPDATE report_config rc
-- INNER JOIN ftp_config fc ON rc.ftp_config_id = fc.id
-- SET rc.scan_path = fc.scan_path;

UPDATE report_config SET scan_path = '/upload' WHERE scan_path IS NULL OR scan_path = '';

-- 3. 将所有报表的ftp_config_id设置为-1（表示内置FTP）
UPDATE report_config SET ftp_config_id = -1 WHERE ftp_config_id IS NOT NULL;

-- 4. 删除外部FTP配置表（确认迁移完成后执行）
-- DROP TABLE IF EXISTS ftp_config;

-- =============================================================================
-- 回滚脚本（如果需要回滚）
-- =============================================================================
-- ALTER TABLE report_config DROP COLUMN IF EXISTS scan_path;
-- UPDATE report_config SET ftp_config_id = NULL WHERE ftp_config_id = -1;
