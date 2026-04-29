-- =============================================================================
-- 报表数据处理平台数据库初始化脚本 (GaussDB 5.x 版本)
-- =============================================================================
-- 数据库: report_db
-- 版本: V1.5
-- 更新日期: 2026-04-10
-- 说明: 适用于 GaussDB 5.x (PostgreSQL 兼容模式)
-- =============================================================================

-- 创建数据库 (需要超级管理员权限)
-- CREATE DATABASE report_db WITH ENCODING='UTF8';

-- \c report_db

-- =============================================================================
-- 创建更新时间触发器函数
-- =============================================================================
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- 系统用户表
-- =============================================================================
DROP TABLE IF EXISTS sys_user CASCADE;
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    last_login_time TIMESTAMP DEFAULT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_username UNIQUE (username)
);

COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.id IS '用户ID';
COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码(BCrypt加密)';
COMMENT ON COLUMN sys_user.last_login_time IS '最后登录时间';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';

CREATE TRIGGER trigger_sys_user_update
    BEFORE UPDATE ON sys_user
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- =============================================================================
-- 系统配置表
-- =============================================================================
DROP TABLE IF EXISTS sys_config CASCADE;
CREATE TABLE sys_config (
    id BIGINT NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(500),
    config_type VARCHAR(50),
    description VARCHAR(200),
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_config_key UNIQUE (config_key)
);

COMMENT ON TABLE sys_config IS '系统配置表';
COMMENT ON COLUMN sys_config.id IS '主键ID';
COMMENT ON COLUMN sys_config.config_key IS '配置键';
COMMENT ON COLUMN sys_config.config_value IS '配置值';
COMMENT ON COLUMN sys_config.config_type IS '配置类型';
COMMENT ON COLUMN sys_config.description IS '描述';
COMMENT ON COLUMN sys_config.deleted IS '删除标记: 0-未删除, 1-已删除';
COMMENT ON COLUMN sys_config.create_time IS '创建时间';
COMMENT ON COLUMN sys_config.update_time IS '更新时间';

CREATE TRIGGER trigger_sys_config_update
    BEFORE UPDATE ON sys_config
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- =============================================================================
-- 操作日志表
-- =============================================================================
DROP TABLE IF EXISTS operation_log CASCADE;
CREATE TABLE operation_log (
    id BIGSERIAL PRIMARY KEY,
    module VARCHAR(50) NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    operation_desc VARCHAR(200) NOT NULL,
    target_id VARCHAR(50),
    target_name VARCHAR(100),
    before_data TEXT,
    after_data TEXT,
    result SMALLINT NOT NULL DEFAULT 1,
    error_msg VARCHAR(500),
    operator_ip VARCHAR(50),
    operator_name VARCHAR(50),
    duration BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_operation_log_module ON operation_log(module);
CREATE INDEX idx_operation_log_type ON operation_log(operation_type);
CREATE INDEX idx_operation_log_target ON operation_log(target_id);
CREATE INDEX idx_operation_log_result ON operation_log(result);
CREATE INDEX idx_operation_log_create_time ON operation_log(create_time);

COMMENT ON TABLE operation_log IS '操作日志表';
COMMENT ON COLUMN operation_log.id IS '主键ID';
COMMENT ON COLUMN operation_log.module IS '操作模块';
COMMENT ON COLUMN operation_log.operation_type IS '操作类型: CREATE-创建, UPDATE-修改, DELETE-删除, TEST-测试';
COMMENT ON COLUMN operation_log.operation_desc IS '操作描述';
COMMENT ON COLUMN operation_log.target_id IS '目标ID';
COMMENT ON COLUMN operation_log.target_name IS '目标名称';
COMMENT ON COLUMN operation_log.before_data IS '操作前数据(JSON)';
COMMENT ON COLUMN operation_log.after_data IS '操作后数据(JSON)';
COMMENT ON COLUMN operation_log.result IS '操作结果: 0-失败, 1-成功';
COMMENT ON COLUMN operation_log.error_msg IS '错误信息';
COMMENT ON COLUMN operation_log.operator_ip IS '操作者IP';
COMMENT ON COLUMN operation_log.operator_name IS '操作者名称';
COMMENT ON COLUMN operation_log.duration IS '执行时长(毫秒)';
COMMENT ON COLUMN operation_log.create_time IS '创建时间';

-- =============================================================================
-- 内置FTP配置表
-- =============================================================================
DROP TABLE IF EXISTS built_in_ftp_config CASCADE;
CREATE TABLE built_in_ftp_config (
    id SERIAL PRIMARY KEY,
    enabled SMALLINT NOT NULL DEFAULT 0,
    port INT NOT NULL DEFAULT 2021,
    username VARCHAR(64) NOT NULL DEFAULT 'rpa_user',
    password VARCHAR(128) NOT NULL DEFAULT 'rpa_password',
    root_directory VARCHAR(256) NOT NULL DEFAULT '/data/ftp-root',
    max_connections INT NOT NULL DEFAULT 10,
    idle_timeout INT NOT NULL DEFAULT 300,
    passive_mode SMALLINT NOT NULL DEFAULT 1,
    passive_port_start INT DEFAULT 50000,
    passive_port_end INT DEFAULT 50100,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE built_in_ftp_config IS '内置FTP配置';
COMMENT ON COLUMN built_in_ftp_config.id IS '主键';
COMMENT ON COLUMN built_in_ftp_config.enabled IS '是否启用';
COMMENT ON COLUMN built_in_ftp_config.port IS 'FTP端口';
COMMENT ON COLUMN built_in_ftp_config.username IS '用户名';
COMMENT ON COLUMN built_in_ftp_config.password IS '密码';
COMMENT ON COLUMN built_in_ftp_config.root_directory IS '根目录';
COMMENT ON COLUMN built_in_ftp_config.max_connections IS '最大连接数';
COMMENT ON COLUMN built_in_ftp_config.idle_timeout IS '空闲超时(秒)';
COMMENT ON COLUMN built_in_ftp_config.passive_mode IS '是否被动模式';
COMMENT ON COLUMN built_in_ftp_config.passive_port_start IS '被动端口起始';
COMMENT ON COLUMN built_in_ftp_config.passive_port_end IS '被动端口结束';
COMMENT ON COLUMN built_in_ftp_config.create_time IS '创建时间';
COMMENT ON COLUMN built_in_ftp_config.update_time IS '更新时间';

CREATE TRIGGER trigger_built_in_ftp_config_update
    BEFORE UPDATE ON built_in_ftp_config
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- =============================================================================
-- 报表配置表
-- =============================================================================
DROP TABLE IF EXISTS report_config CASCADE;
CREATE TABLE report_config (
    id BIGINT NOT NULL,
    report_code VARCHAR(50) NOT NULL,
    report_name VARCHAR(100) NOT NULL,
    ftp_config_id BIGINT,
    scan_path VARCHAR(200) DEFAULT '/upload',
    file_pattern VARCHAR(100),
    sheet_index INT NOT NULL DEFAULT 0,
    header_row INT NOT NULL DEFAULT 0,
    data_start_row INT NOT NULL DEFAULT 1,
    skip_columns INT NOT NULL DEFAULT 0,
    date_extract_pattern VARCHAR(50),
    column_mapping TEXT NOT NULL,
    output_table VARCHAR(50) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    remark VARCHAR(500),
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_report_code UNIQUE (report_code)
);

CREATE INDEX idx_report_config_name ON report_config(report_name);
CREATE INDEX idx_report_config_ftp_id ON report_config(ftp_config_id);
CREATE INDEX idx_report_config_status ON report_config(status);

COMMENT ON TABLE report_config IS '报表配置表';
COMMENT ON COLUMN report_config.id IS '主键ID';
COMMENT ON COLUMN report_config.report_code IS '报表编码';
COMMENT ON COLUMN report_config.report_name IS '报表名称';
COMMENT ON COLUMN report_config.ftp_config_id IS '关联FTP配置ID(已废弃,仅保留兼容)';
COMMENT ON COLUMN report_config.scan_path IS '扫描路径';
COMMENT ON COLUMN report_config.file_pattern IS '文件匹配模式';
COMMENT ON COLUMN report_config.sheet_index IS 'Sheet索引';
COMMENT ON COLUMN report_config.header_row IS '表头行号';
COMMENT ON COLUMN report_config.data_start_row IS '数据起始行';
COMMENT ON COLUMN report_config.skip_columns IS '跳过前N列';
COMMENT ON COLUMN report_config.date_extract_pattern IS '日期提取规则';
COMMENT ON COLUMN report_config.column_mapping IS '列映射配置(JSON)';
COMMENT ON COLUMN report_config.output_table IS '输出表名';
COMMENT ON COLUMN report_config.status IS '状态: 0-禁用, 1-启用';
COMMENT ON COLUMN report_config.remark IS '备注';
COMMENT ON COLUMN report_config.deleted IS '删除标记';
COMMENT ON COLUMN report_config.create_time IS '创建时间';
COMMENT ON COLUMN report_config.update_time IS '更新时间';

CREATE TRIGGER trigger_report_config_update
    BEFORE UPDATE ON report_config
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- =============================================================================
-- 已处理文件记录表
-- =============================================================================
DROP TABLE IF EXISTS processed_file CASCADE;
CREATE TABLE processed_file (
    id BIGSERIAL PRIMARY KEY,
    report_config_id BIGINT NOT NULL,
    file_name VARCHAR(200) NOT NULL,
    file_size BIGINT DEFAULT NULL,
    pt_dt DATE DEFAULT NULL,
    status VARCHAR(20) DEFAULT 'PROCESSED',
    task_id BIGINT DEFAULT NULL,
    error_message TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_report_file UNIQUE (report_config_id, file_name)
);

CREATE INDEX idx_processed_file_pt_dt ON processed_file(pt_dt);
CREATE INDEX idx_processed_file_status ON processed_file(status);

COMMENT ON TABLE processed_file IS '已处理文件记录表';
COMMENT ON COLUMN processed_file.id IS '主键ID';
COMMENT ON COLUMN processed_file.report_config_id IS '报表配置ID';
COMMENT ON COLUMN processed_file.file_name IS '文件名（不含路径）';
COMMENT ON COLUMN processed_file.file_size IS '文件大小（字节）';
COMMENT ON COLUMN processed_file.pt_dt IS '数据分区日期';
COMMENT ON COLUMN processed_file.status IS '处理状态：PROCESSED-已处理，FAILED-处理失败';
COMMENT ON COLUMN processed_file.task_id IS '关联任务ID';
COMMENT ON COLUMN processed_file.error_message IS '错误信息';
COMMENT ON COLUMN processed_file.create_time IS '创建时间';

-- =============================================================================
-- 任务执行记录表
-- =============================================================================
DROP TABLE IF EXISTS task_execution CASCADE;
CREATE TABLE task_execution (
    id BIGINT NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    task_name VARCHAR(100) NOT NULL,
    report_config_id BIGINT,
    file_name VARCHAR(200),
    file_path VARCHAR(500),
    pipeline_code VARCHAR(100),
    partition_value VARCHAR(50),
    step_name VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    total_rows INT DEFAULT 0,
    success_rows INT DEFAULT 0,
    failed_rows INT DEFAULT 0,
    error_message TEXT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration BIGINT,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_task_execution_type ON task_execution(task_type);
CREATE INDEX idx_task_execution_report_id ON task_execution(report_config_id);
CREATE INDEX idx_task_execution_status ON task_execution(status);
CREATE INDEX idx_task_execution_create_time ON task_execution(create_time);

COMMENT ON TABLE task_execution IS '任务执行记录表';
COMMENT ON COLUMN task_execution.id IS '主键ID';
COMMENT ON COLUMN task_execution.task_type IS '任务类型';
COMMENT ON COLUMN task_execution.task_name IS '任务名称';
COMMENT ON COLUMN task_execution.report_config_id IS '关联报表配置ID';
COMMENT ON COLUMN task_execution.file_name IS '处理文件名';
COMMENT ON COLUMN task_execution.file_path IS '文件路径';
COMMENT ON COLUMN task_execution.pipeline_code IS '流水线编码';
COMMENT ON COLUMN task_execution.partition_value IS '分区值';
COMMENT ON COLUMN task_execution.step_name IS '当前步骤名称';
COMMENT ON COLUMN task_execution.status IS '状态: PENDING-待执行, RUNNING-执行中, SUCCESS-成功, FAILED-失败';
COMMENT ON COLUMN task_execution.total_rows IS '总行数';
COMMENT ON COLUMN task_execution.success_rows IS '成功行数';
COMMENT ON COLUMN task_execution.failed_rows IS '失败行数';
COMMENT ON COLUMN task_execution.error_message IS '错误信息';
COMMENT ON COLUMN task_execution.start_time IS '开始时间';
COMMENT ON COLUMN task_execution.end_time IS '结束时间';
COMMENT ON COLUMN task_execution.duration IS '执行时长(毫秒)';
COMMENT ON COLUMN task_execution.deleted IS '删除标记';
COMMENT ON COLUMN task_execution.create_time IS '创建时间';
COMMENT ON COLUMN task_execution.update_time IS '更新时间';

CREATE TRIGGER trigger_task_execution_update
    BEFORE UPDATE ON task_execution
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- =============================================================================
-- 任务执行日志表
-- =============================================================================
DROP TABLE IF EXISTS task_execution_log CASCADE;
CREATE TABLE task_execution_log (
    id BIGSERIAL PRIMARY KEY,
    task_execution_id BIGINT NOT NULL,
    log_level VARCHAR(20) NOT NULL,
    log_message TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_task_execution_log_id ON task_execution_log(task_execution_id);
CREATE INDEX idx_task_execution_log_create_time ON task_execution_log(create_time);

COMMENT ON TABLE task_execution_log IS '任务执行日志表';
COMMENT ON COLUMN task_execution_log.id IS '主键ID';
COMMENT ON COLUMN task_execution_log.task_execution_id IS '任务执行ID';
COMMENT ON COLUMN task_execution_log.log_level IS '日志级别: INFO, WARN, ERROR';
COMMENT ON COLUMN task_execution_log.log_message IS '日志内容';
COMMENT ON COLUMN task_execution_log.create_time IS '创建时间';

-- =============================================================================
-- 触发器配置表
-- =============================================================================
DROP TABLE IF EXISTS trigger_config CASCADE;
CREATE TABLE trigger_config (
    id SERIAL PRIMARY KEY,
    trigger_code VARCHAR(100) NOT NULL,
    trigger_name VARCHAR(200) NOT NULL,
    source_table VARCHAR(100) NOT NULL,
    partition_column VARCHAR(50) NOT NULL DEFAULT 'pt_dt',
    partition_pattern VARCHAR(50),
    poll_interval_seconds INT NOT NULL DEFAULT 60,
    max_retries INT NOT NULL DEFAULT 60,
    pipeline_code VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    last_trigger_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_trigger_code UNIQUE (trigger_code)
);

CREATE INDEX idx_trigger_config_status ON trigger_config(status);

COMMENT ON TABLE trigger_config IS '触发器配置表';
COMMENT ON COLUMN trigger_config.id IS '主键ID';
COMMENT ON COLUMN trigger_config.trigger_code IS '触发器编码';
COMMENT ON COLUMN trigger_config.trigger_name IS '触发器名称';
COMMENT ON COLUMN trigger_config.source_table IS '监听目标表';
COMMENT ON COLUMN trigger_config.partition_column IS '分区字段';
COMMENT ON COLUMN trigger_config.partition_pattern IS '分区值模式，支持日期格式如 yyyy-MM-dd';
COMMENT ON COLUMN trigger_config.poll_interval_seconds IS '轮询间隔(秒)';
COMMENT ON COLUMN trigger_config.max_retries IS '最大重试次数';
COMMENT ON COLUMN trigger_config.pipeline_code IS '触发执行的Pipeline编码';
COMMENT ON COLUMN trigger_config.status IS '状态: ENABLED/DISABLED';
COMMENT ON COLUMN trigger_config.last_trigger_time IS '最后触发时间';
COMMENT ON COLUMN trigger_config.create_time IS '创建时间';
COMMENT ON COLUMN trigger_config.update_time IS '更新时间';

CREATE TRIGGER trigger_trigger_config_update
    BEFORE UPDATE ON trigger_config
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- =============================================================================
-- 触发器状态持久化表（支持集群模式）
-- =============================================================================
DROP TABLE IF EXISTS trigger_state_record CASCADE;
CREATE TABLE trigger_state_record (
    id SERIAL PRIMARY KEY,
    trigger_code VARCHAR(100) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_check_time TIMESTAMP,
    triggered SMALLINT NOT NULL DEFAULT 0,
    instance_id VARCHAR(200),
    version INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_trigger_state_code UNIQUE (trigger_code)
);

CREATE INDEX idx_trigger_state_triggered ON trigger_state_record(triggered);
CREATE INDEX idx_trigger_state_instance ON trigger_state_record(instance_id);

COMMENT ON TABLE trigger_state_record IS '触发器状态持久化表';
COMMENT ON COLUMN trigger_state_record.id IS '主键ID';
COMMENT ON COLUMN trigger_state_record.trigger_code IS '触发器编码';
COMMENT ON COLUMN trigger_state_record.retry_count IS '重试次数';
COMMENT ON COLUMN trigger_state_record.last_check_time IS '最后检查时间';
COMMENT ON COLUMN trigger_state_record.triggered IS '是否已触发: 0-未触发, 1-已触发';
COMMENT ON COLUMN trigger_state_record.instance_id IS '实例标识';
COMMENT ON COLUMN trigger_state_record.version IS '乐观锁版本号';
COMMENT ON COLUMN trigger_state_record.create_time IS '创建时间';
COMMENT ON COLUMN trigger_state_record.update_time IS '更新时间';

CREATE TRIGGER trigger_trigger_state_record_update
    BEFORE UPDATE ON trigger_state_record
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- =============================================================================
-- 触发器执行日志表
-- =============================================================================
DROP TABLE IF EXISTS trigger_execution_log CASCADE;
CREATE TABLE trigger_execution_log (
    id BIGSERIAL PRIMARY KEY,
    trigger_code VARCHAR(100) NOT NULL,
    trigger_name VARCHAR(200) NOT NULL,
    partition_date DATE NOT NULL,
    data_count INT DEFAULT 0,
    trigger_status VARCHAR(20) NOT NULL,
    pipeline_task_id BIGINT DEFAULT NULL,
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
-- 触发器分区记录表（防止同一分区重复触发）
-- =============================================================================
DROP TABLE IF EXISTS trigger_partition_record CASCADE;
CREATE TABLE trigger_partition_record (
    id              BIGSERIAL PRIMARY KEY,
    trigger_code     VARCHAR(100) NOT NULL,
    partition_date   DATE NOT NULL,
    triggered        SMALLINT DEFAULT 0,
    pipeline_task_id BIGINT,
    trigger_time     TIMESTAMP,
    status           VARCHAR(20) DEFAULT 'TRIGGERING',
    instance_id      VARCHAR(200),
    version          INT NOT NULL DEFAULT 0,
    create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_trigger_partition UNIQUE (trigger_code, partition_date)
);

CREATE INDEX idx_partition_status ON trigger_partition_record(status);
CREATE INDEX idx_partition_time ON trigger_partition_record(trigger_time);

COMMENT ON TABLE trigger_partition_record IS '触发器分区记录表';
COMMENT ON COLUMN trigger_partition_record.id IS '主键ID';
COMMENT ON COLUMN trigger_partition_record.trigger_code IS '触发器编码';
COMMENT ON COLUMN trigger_partition_record.partition_date IS '分区日期';
COMMENT ON COLUMN trigger_partition_record.triggered IS '是否已触发: 0-未触发, 1-已触发';
COMMENT ON COLUMN trigger_partition_record.pipeline_task_id IS 'Pipeline任务ID';
COMMENT ON COLUMN trigger_partition_record.trigger_time IS '触发时间';
COMMENT ON COLUMN trigger_partition_record.status IS '状态: TRIGGERING/TRIGGERED';
COMMENT ON COLUMN trigger_partition_record.instance_id IS '实例标识';
COMMENT ON COLUMN trigger_partition_record.version IS '乐观锁版本号';
COMMENT ON COLUMN trigger_partition_record.create_time IS '创建时间';
COMMENT ON COLUMN trigger_partition_record.update_time IS '更新时间';

CREATE TRIGGER trigger_partition_record_update
    BEFORE UPDATE ON trigger_partition_record
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- =============================================================================
-- 流水线配置表
-- =============================================================================
DROP TABLE IF EXISTS pipeline_config CASCADE;
CREATE TABLE pipeline_config (
    id SERIAL PRIMARY KEY,
    pipeline_code VARCHAR(100) NOT NULL,
    pipeline_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    idempotent_mode VARCHAR(20) NOT NULL DEFAULT 'OVERWRITE',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_pipeline_code UNIQUE (pipeline_code)
);

COMMENT ON TABLE pipeline_config IS '流水线配置表';
COMMENT ON COLUMN pipeline_config.id IS '主键ID';
COMMENT ON COLUMN pipeline_config.pipeline_code IS '流水线编码';
COMMENT ON COLUMN pipeline_config.pipeline_name IS '流水线名称';
COMMENT ON COLUMN pipeline_config.description IS '流水线描述';
COMMENT ON COLUMN pipeline_config.idempotent_mode IS '幂等模式: OVERWRITE/APPEND';
COMMENT ON COLUMN pipeline_config.status IS '状态: ENABLED/DISABLED';
COMMENT ON COLUMN pipeline_config.create_time IS '创建时间';
COMMENT ON COLUMN pipeline_config.update_time IS '更新时间';

CREATE TRIGGER trigger_pipeline_config_update
    BEFORE UPDATE ON pipeline_config
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- =============================================================================
-- 表分层映射表
-- =============================================================================
DROP TABLE IF EXISTS table_layer_mapping CASCADE;
CREATE TABLE table_layer_mapping (
    id SERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    table_layer VARCHAR(20),
    source_type VARCHAR(50),
    source_id BIGINT,
    source_name VARCHAR(200),
    business_domain VARCHAR(200),
    description VARCHAR(500),
    tags JSONB,
    marked SMALLINT NOT NULL DEFAULT 0,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_table_name UNIQUE (table_name)
);

COMMENT ON TABLE table_layer_mapping IS '表分层映射表';
COMMENT ON COLUMN table_layer_mapping.id IS '主键ID';
COMMENT ON COLUMN table_layer_mapping.table_name IS '数据库表名';
COMMENT ON COLUMN table_layer_mapping.table_layer IS '流向分层: ODS/DWD/DWS/ADS';
COMMENT ON COLUMN table_layer_mapping.source_type IS '来源类型';
COMMENT ON COLUMN table_layer_mapping.source_id IS '关联来源ID';
COMMENT ON COLUMN table_layer_mapping.source_name IS '关联来源名称';
COMMENT ON COLUMN table_layer_mapping.business_domain IS '业务域描述';
COMMENT ON COLUMN table_layer_mapping.description IS '表描述';
COMMENT ON COLUMN table_layer_mapping.tags IS '自定义标签';
COMMENT ON COLUMN table_layer_mapping.marked IS '是否已标记: 0-未标记, 1-已标记';
COMMENT ON COLUMN table_layer_mapping.deleted IS '删除标记: 0-未删除, 1-已删除';
COMMENT ON COLUMN table_layer_mapping.create_time IS '创建时间';
COMMENT ON COLUMN table_layer_mapping.update_time IS '更新时间';

CREATE TRIGGER trigger_table_layer_mapping_update
    BEFORE UPDATE ON table_layer_mapping
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- =============================================================================
-- 序列创建 (用于非 SERIAL 主键的表)
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS sys_config_id_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS report_config_id_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS task_execution_id_seq START WITH 1;

-- =============================================================================
-- 初始化数据
-- =============================================================================

-- 默认管理员用户 (密码: admin123, BCrypt加密)
INSERT INTO sys_user (id, username, password, last_login_time) VALUES
(nextval('sys_user_id_seq'), 'admin', '$2a$10$zg.SpcNkqcH2g65SLjwZeO1KBObp6waE2bjm2pCrF5bsgED6Mwd.2', NULL);

-- 重置序列
SELECT setval('sys_user_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM sys_user));

-- 内置FTP配置
INSERT INTO built_in_ftp_config (enabled, port, username, password, root_directory, max_connections, idle_timeout, passive_mode, passive_port_start, passive_port_end) VALUES
(0, 2021, 'rpa_user', 'rpa_password', '/data/ftp-root', 10, 300, 1, 50000, 50100);

-- 示例报表配置
INSERT INTO report_config (id, report_code, report_name, ftp_config_id, scan_path, file_pattern, sheet_index, header_row, data_start_row, skip_columns, date_extract_pattern, column_mapping, output_table, status, remark) VALUES
(nextval('report_config_id_seq'), 'SALES_REPORT', '销售报表', -1, '/upload', 'sales_*.xlsx', 0, 0, 1, 0, NULL, '[{"excelColumn":"A","fieldName":"order_id","fieldType":"STRING"},{"excelColumn":"B","fieldName":"product_name","fieldType":"STRING"},{"excelColumn":"C","fieldName":"quantity","fieldType":"INTEGER"},{"excelColumn":"D","fieldName":"amount","fieldType":"DECIMAL"},{"excelColumn":"E","fieldName":"order_date","fieldType":"DATE"}]', 't_sales_data', 1, '销售数据报表配置');

SELECT setval('report_config_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM report_config));

-- =============================================================================
-- 完成提示
-- =============================================================================
-- 数据库初始化完成！
-- 默认管理员账号: admin / admin123
-- =============================================================================

-- =============================================================================
-- Quartz 集群表结构 (GaussDB/PostgreSQL)
-- =============================================================================

DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS CASCADE;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE CASCADE;
DROP TABLE IF EXISTS QRTZ_LOCKS CASCADE;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_TRIGGERS CASCADE;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS CASCADE;
DROP TABLE IF EXISTS QRTZ_CALENDARS CASCADE;

CREATE TABLE QRTZ_JOB_DETAILS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    JOB_NAME VARCHAR(190) NOT NULL,
    JOB_GROUP VARCHAR(190) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    JOB_CLASS_NAME VARCHAR(250) NOT NULL,
    IS_DURABLE VARCHAR(1) NOT NULL,
    IS_NONCONCURRENT VARCHAR(1) NOT NULL,
    IS_UPDATE_DATA VARCHAR(1) NOT NULL,
    REQUESTS_RECOVERY VARCHAR(1) NOT NULL,
    JOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE QRTZ_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    JOB_NAME VARCHAR(190) NOT NULL,
    JOB_GROUP VARCHAR(190) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT NULL,
    PREV_FIRE_TIME BIGINT NULL,
    PRIORITY INTEGER NULL,
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    START_TIME BIGINT NOT NULL,
    END_TIME BIGINT NULL,
    CALENDAR_NAME VARCHAR(190) NULL,
    MISFIRE_INSTR SMALLINT NULL,
    JOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
        REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME, JOB_NAME, JOB_GROUP)
);

CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    REPEAT_COUNT BIGINT NOT NULL,
    REPEAT_INTERVAL BIGINT NOT NULL,
    TIMES_TRIGGERED BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_CRON_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_SIMPROP_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 INTEGER NULL,
    INT_PROP_2 INTEGER NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 VARCHAR(1) NULL,
    BOOL_PROP_2 VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_BLOB_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    BLOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_CALENDARS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(190) NOT NULL,
    CALENDAR BYTEA NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
);

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
);

CREATE TABLE QRTZ_FIRED_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    ENTRY_ID VARCHAR(95) NOT NULL,
    TRIGGER_NAME VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    INSTANCE_NAME VARCHAR(190) NOT NULL,
    FIRED_TIME BIGINT NOT NULL,
    SCHED_TIME BIGINT NOT NULL,
    PRIORITY INTEGER NOT NULL,
    STATE VARCHAR(16) NOT NULL,
    JOB_NAME VARCHAR(190) NULL,
    JOB_GROUP VARCHAR(190) NULL,
    IS_NONCONCURRENT VARCHAR(1) NULL,
    REQUESTS_RECOVERY VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
);

CREATE TABLE QRTZ_SCHEDULER_STATE (
    SCHED_NAME VARCHAR(120) NOT NULL,
    INSTANCE_NAME VARCHAR(190) NOT NULL,
    LAST_CHECKIN_TIME BIGINT NOT NULL,
    CHECKIN_INTERVAL BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
);

CREATE TABLE QRTZ_LOCKS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME VARCHAR(40) NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
);

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS(SCHED_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS(SCHED_NAME, JOB_GROUP);

CREATE INDEX IDX_QRTZ_T_J ON QRTZ_TRIGGERS(SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_JG ON QRTZ_TRIGGERS(SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_C ON QRTZ_TRIGGERS(SCHED_NAME, CALENDAR_NAME);
CREATE INDEX IDX_QRTZ_T_G ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_T_STATE ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_STATE ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_G_STATE ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_GROUP, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME ON QRTZ_TRIGGERS(SCHED_NAME, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST ON QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_STATE, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON QRTZ_TRIGGERS(SCHED_NAME, MISFIRE_INSTR, NEXT_FIRE_TIME, TRIGGER_GROUP, TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, INSTANCE_NAME);
CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, INSTANCE_NAME, REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_FT_J_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, JOB_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_JG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_T_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_FT_TG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME, TRIGGER_GROUP);
