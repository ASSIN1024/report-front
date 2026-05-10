-- V1.4__add_missing_tables_for_gaussdb.sql
-- 描述: 补充GaussDB Schema缺失的表
-- 创建日期: 2026-05-07
-- 说明: MySQL Schema中的packing_config, packing_batch, alert_record, ods_backup等表在GaussDB Schema中缺失

-- =============================================================================
-- 系统用户表 (sys_user)
-- =============================================================================
DROP TABLE IF EXISTS sys_user CASCADE;
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    last_login_time TIMESTAMP DEFAULT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);

CREATE TRIGGER trigger_sys_user_update
    BEFORE UPDATE ON sys_user
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.id IS '用户ID';
COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码(BCrypt加密)';
COMMENT ON COLUMN sys_user.last_login_time IS '最后登录时间';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';

-- =============================================================================
-- 打包配置表 (packing_config)
-- =============================================================================
DROP TABLE IF EXISTS packing_config CASCADE;
CREATE TABLE packing_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(500),
    config_type VARCHAR(50),
    description VARCHAR(200),
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_packing_config_key UNIQUE (config_key)
);

CREATE TRIGGER trigger_packing_config_update
    BEFORE UPDATE ON packing_config
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE packing_config IS '打包配置表';
COMMENT ON COLUMN packing_config.id IS '主键ID';
COMMENT ON COLUMN packing_config.config_key IS '配置键';
COMMENT ON COLUMN packing_config.config_value IS '配置值';
COMMENT ON COLUMN packing_config.config_type IS '配置类型';
COMMENT ON COLUMN packing_config.description IS '描述';
COMMENT ON COLUMN packing_config.deleted IS '删除标记: 0-未删除, 1-已删除';
COMMENT ON COLUMN packing_config.create_time IS '创建时间';
COMMENT ON COLUMN packing_config.update_time IS '更新时间';

-- =============================================================================
-- 打包批次表 (packing_batch)
-- =============================================================================
DROP TABLE IF EXISTS packing_batch CASCADE;
CREATE TABLE packing_batch (
    id BIGSERIAL PRIMARY KEY,
    batch_no VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_size BIGINT DEFAULT 0,
    file_count INT DEFAULT 0,
    for_upload_path VARCHAR(500),
    done_dir_path VARCHAR(500),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_packing_batch_no UNIQUE (batch_no)
);

CREATE INDEX idx_packing_batch_status ON packing_batch(status);
CREATE INDEX idx_packing_batch_create_time ON packing_batch(create_time);

CREATE TRIGGER trigger_packing_batch_update
    BEFORE UPDATE ON packing_batch
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE packing_batch IS '打包批次表';
COMMENT ON COLUMN packing_batch.id IS '主键ID';
COMMENT ON COLUMN packing_batch.batch_no IS '批次号';
COMMENT ON COLUMN packing_batch.status IS '状态: PENDING-待打包, UPLOADING-上传中, CONSUMING-消费中, DONE-已完成';
COMMENT ON COLUMN packing_batch.total_size IS '总大小(字节)';
COMMENT ON COLUMN packing_batch.file_count IS '文件数量';
COMMENT ON COLUMN packing_batch.for_upload_path IS '上传路径';
COMMENT ON COLUMN packing_batch.done_dir_path IS 'Done目录路径';
COMMENT ON COLUMN packing_batch.start_time IS '开始时间';
COMMENT ON COLUMN packing_batch.end_time IS '结束时间';
COMMENT ON COLUMN packing_batch.create_time IS '创建时间';
COMMENT ON COLUMN packing_batch.update_time IS '更新时间';

-- =============================================================================
-- 告警记录表 (alert_record)
-- =============================================================================
DROP TABLE IF EXISTS alert_record CASCADE;
CREATE TABLE alert_record (
    id BIGSERIAL PRIMARY KEY,
    alert_type VARCHAR(20) NOT NULL,
    file_name VARCHAR(200),
    report_config_id BIGINT,
    alert_level VARCHAR(20),
    alert_message VARCHAR(500),
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolve_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT NULL
);

CREATE INDEX idx_alert_record_type ON alert_record(alert_type);
CREATE INDEX idx_alert_record_status ON alert_record(status);
CREATE INDEX idx_alert_record_create_time ON alert_record(create_time);
CREATE INDEX idx_alert_record_report_config_id ON alert_record(report_config_id);

COMMENT ON TABLE alert_record IS '告警记录表';
COMMENT ON COLUMN alert_record.id IS '主键ID';
COMMENT ON COLUMN alert_record.alert_type IS '告警类型';
COMMENT ON COLUMN alert_record.file_name IS '相关文件名';
COMMENT ON COLUMN alert_record.report_config_id IS '关联报表配置ID';
COMMENT ON COLUMN alert_record.alert_level IS '告警级别';
COMMENT ON COLUMN alert_record.alert_message IS '告警消息';
COMMENT ON COLUMN alert_record.reason IS '告警原因';
COMMENT ON COLUMN alert_record.status IS '状态: PENDING-待处理, RESOLVED-已解决, IGNORED-已忽略';
COMMENT ON COLUMN alert_record.resolve_time IS '解决时间';
COMMENT ON COLUMN alert_record.create_time IS '创建时间';
COMMENT ON COLUMN alert_record.update_time IS '更新时间';

-- =============================================================================
-- ODS备份记录表 (ods_backup)
-- =============================================================================
DROP TABLE IF EXISTS ods_backup CASCADE;
CREATE TABLE ods_backup (
    id BIGSERIAL PRIMARY KEY,
    source_file VARCHAR(200) NOT NULL,
    pt_dt VARCHAR(20),
    db_name VARCHAR(128),
    table_name VARCHAR(128),
    report_config_id BIGINT,
    file_size BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ods_backup_pt_dt ON ods_backup(pt_dt);
CREATE INDEX idx_ods_backup_source_file ON ods_backup(source_file);
CREATE INDEX idx_ods_backup_report_config_id ON ods_backup(report_config_id);

COMMENT ON TABLE ods_backup IS 'ODS备份记录表';
COMMENT ON COLUMN ods_backup.id IS '主键ID';
COMMENT ON COLUMN ods_backup.source_file IS '源文件名';
COMMENT ON COLUMN ods_backup.pt_dt IS '分区日期';
COMMENT ON COLUMN ods_backup.db_name IS '数据库名';
COMMENT ON COLUMN ods_backup.table_name IS '表名';
COMMENT ON COLUMN ods_backup.report_config_id IS '报表配置ID';
COMMENT ON COLUMN ods_backup.file_size IS '文件大小';
COMMENT ON COLUMN ods_backup.create_time IS '创建时间';

-- =============================================================================
-- 进程监控日志表 (process_monitor_log)
-- =============================================================================
DROP TABLE IF EXISTS process_monitor_log CASCADE;
CREATE TABLE process_monitor_log (
    id BIGSERIAL PRIMARY KEY,
    report_code VARCHAR(64),
    file_name VARCHAR(256),
    step VARCHAR(32),
    status VARCHAR(16),
    message TEXT,
    duration_ms BIGINT,
    start_time TIMESTAMP,
    end_time TIMESTAMP
);

CREATE INDEX idx_process_monitor_log_report_code ON process_monitor_log(report_code);
CREATE INDEX idx_process_monitor_log_file_name ON process_monitor_log(file_name);
CREATE INDEX idx_process_monitor_log_status ON process_monitor_log(status);

COMMENT ON TABLE process_monitor_log IS '进程监控日志表';
COMMENT ON COLUMN process_monitor_log.id IS '主键ID';
COMMENT ON COLUMN process_monitor_log.report_code IS '报表编码';
COMMENT ON COLUMN process_monitor_log.file_name IS '文件名';
COMMENT ON COLUMN process_monitor_log.step IS '处理步骤';
COMMENT ON COLUMN process_monitor_log.status IS '状态: RUNNING/SUCCESS/FAILED';
COMMENT ON COLUMN process_monitor_log.message IS '处理消息';
COMMENT ON COLUMN process_monitor_log.duration_ms IS '执行时长(毫秒)';
COMMENT ON COLUMN process_monitor_log.start_time IS '开始时间';
COMMENT ON COLUMN process_monitor_log.end_time IS '结束时间';

-- =============================================================================
-- 触发器执行日志表 (trigger_execution_log)
-- =============================================================================
DROP TABLE IF EXISTS trigger_execution_log CASCADE;
CREATE TABLE trigger_execution_log (
    id BIGSERIAL PRIMARY KEY,
    trigger_code VARCHAR(100) NOT NULL,
    trigger_name VARCHAR(200) NOT NULL,
    partition_date DATE NOT NULL,
    data_count INT DEFAULT 0,
    trigger_status VARCHAR(20) NOT NULL,
    pipeline_task_id BIGINT,
    error_message TEXT,
    retry_count INT DEFAULT 0,
    execution_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trigger_exec_log_code ON trigger_execution_log(trigger_code);
CREATE INDEX idx_trigger_exec_log_date ON trigger_execution_log(partition_date);
CREATE INDEX idx_trigger_exec_log_time ON trigger_execution_log(execution_time);

COMMENT ON TABLE trigger_execution_log IS '触发器执行日志表';
COMMENT ON COLUMN trigger_execution_log.id IS '主键ID';
COMMENT ON COLUMN trigger_execution_log.trigger_code IS '触发器编码';
COMMENT ON COLUMN trigger_execution_log.trigger_name IS '触发器名称';
COMMENT ON COLUMN trigger_execution_log.partition_date IS '分区日期';
COMMENT ON COLUMN trigger_execution_log.data_count IS '检测到的数据行数';
COMMENT ON COLUMN trigger_execution_log.trigger_status IS '触发状态';
COMMENT ON COLUMN trigger_execution_log.pipeline_task_id IS '关联的Pipeline任务ID';
COMMENT ON COLUMN trigger_execution_log.error_message IS '错误信息';
COMMENT ON COLUMN trigger_execution_log.retry_count IS '触发时的重试次数';
COMMENT ON COLUMN trigger_execution_log.execution_time IS '执行时间';
COMMENT ON COLUMN trigger_execution_log.create_time IS '创建时间';

-- =============================================================================
-- 初始化数据
-- =============================================================================

-- 插入默认管理员用户 (密码: admin123, BCrypt加密)
INSERT INTO sys_user (username, password)
VALUES ('admin', '$2a$10$zg.SpcNkqcH2g65SLjwZeO1KBObp6waE2bjm2pCrF5bsgED6Mwd.2');

-- 插入打包默认配置
INSERT INTO packing_config (config_key, config_value, config_type, description) VALUES
('max_package_size', '209715200', 'SIZE', '单个包最大大小(200MB)'),
('upload_dir', '/data/ftp-root/for-upload', 'PATH', '上传目录'),
('done_dir', '/data/ftp-root/done', 'PATH', '完成目录'),
('fixed_filename', 'outputs.zip', 'STRING', '固定文件名'),
('polling_interval', '30', 'INT', '消费轮询间隔(秒)'),
('scan_interval', '300', 'INT', '扫描间隔(秒)');

-- =============================================================================
-- 完成提示
-- =============================================================================
-- 数据库补充表初始化完成！
-- 默认管理员账号: admin / admin123
-- =============================================================================
