-- 报表数据处理平台数据库初始化脚本
-- 数据库: report_db

-- 创建数据库
CREATE DATABASE IF NOT EXISTS report_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE report_db;

-- FTP配置表
DROP TABLE IF EXISTS ftp_config;
CREATE TABLE ftp_config (
    id BIGINT NOT NULL COMMENT '主键ID',
    config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
    host VARCHAR(100) NOT NULL COMMENT 'FTP服务器地址',
    port INT NOT NULL DEFAULT 21 COMMENT 'FTP端口',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    scan_path VARCHAR(200) COMMENT '扫描路径',
    file_pattern VARCHAR(100) COMMENT '文件匹配模式',
    scan_interval INT NOT NULL DEFAULT 300 COMMENT '扫描间隔(秒)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    remark VARCHAR(500) COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_config_name (config_name),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FTP配置表';

-- 报表配置表
DROP TABLE IF EXISTS report_config;
CREATE TABLE report_config (
    id BIGINT NOT NULL COMMENT '主键ID',
    report_code VARCHAR(50) NOT NULL COMMENT '报表编码',
    report_name VARCHAR(100) NOT NULL COMMENT '报表名称',
    ftp_config_id BIGINT COMMENT '关联FTP配置ID',
    file_pattern VARCHAR(100) COMMENT '文件匹配模式',
    sheet_index INT NOT NULL DEFAULT 0 COMMENT 'Sheet索引',
    header_row INT NOT NULL DEFAULT 0 COMMENT '表头行号',
    data_start_row INT NOT NULL DEFAULT 1 COMMENT '数据起始行',
    column_mapping TEXT NOT NULL COMMENT '列映射配置(JSON)',
    output_table VARCHAR(50) NOT NULL COMMENT '输出表名',
    output_mode VARCHAR(20) NOT NULL DEFAULT 'APPEND' COMMENT '输出模式: APPEND-追加, OVERWRITE-覆盖',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    remark VARCHAR(500) COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_code (report_code),
    KEY idx_report_name (report_name),
    KEY idx_ftp_config_id (ftp_config_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表配置表';

-- 任务执行记录表
DROP TABLE IF EXISTS task_execution;
CREATE TABLE task_execution (
    id BIGINT NOT NULL COMMENT '主键ID',
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    report_config_id BIGINT COMMENT '关联报表配置ID',
    file_name VARCHAR(200) COMMENT '处理文件名',
    file_path VARCHAR(500) COMMENT '文件路径',
    status VARCHAR(20) NOT NULL COMMENT '状态: PENDING-待执行, RUNNING-执行中, SUCCESS-成功, FAILED-失败',
    total_rows INT DEFAULT 0 COMMENT '总行数',
    success_rows INT DEFAULT 0 COMMENT '成功行数',
    failed_rows INT DEFAULT 0 COMMENT '失败行数',
    error_message TEXT COMMENT '错误信息',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration BIGINT COMMENT '执行时长(毫秒)',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_task_type (task_type),
    KEY idx_report_config_id (report_config_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行记录表';

-- 任务执行日志表
DROP TABLE IF EXISTS task_execution_log;
CREATE TABLE task_execution_log (
    id BIGINT NOT NULL COMMENT '主键ID',
    task_execution_id BIGINT NOT NULL COMMENT '任务执行ID',
    log_level VARCHAR(20) NOT NULL COMMENT '日志级别: INFO, WARN, ERROR',
    log_message TEXT COMMENT '日志内容',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_task_execution_id (task_execution_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行日志表';

-- 系统配置表
DROP TABLE IF EXISTS sys_config;
CREATE TABLE sys_config (
    id BIGINT NOT NULL COMMENT '主键ID',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value VARCHAR(500) COMMENT '配置值',
    config_type VARCHAR(50) COMMENT '配置类型',
    description VARCHAR(200) COMMENT '描述',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 操作日志表
DROP TABLE IF EXISTS operation_log;
CREATE TABLE operation_log (
    id BIGINT NOT NULL COMMENT '主键ID',
    module VARCHAR(50) NOT NULL COMMENT '操作模块',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型: CREATE-创建, UPDATE-修改, DELETE-删除, TEST-测试',
    operation_desc VARCHAR(200) NOT NULL COMMENT '操作描述',
    target_id VARCHAR(50) COMMENT '目标ID',
    target_name VARCHAR(100) COMMENT '目标名称',
    before_data TEXT COMMENT '操作前数据(JSON)',
    after_data TEXT COMMENT '操作后数据(JSON)',
    result TINYINT NOT NULL DEFAULT 1 COMMENT '操作结果: 0-失败, 1-成功',
    error_msg VARCHAR(500) COMMENT '错误信息',
    operator_ip VARCHAR(50) COMMENT '操作者IP',
    operator_name VARCHAR(50) COMMENT '操作者名称',
    duration BIGINT COMMENT '执行时长(毫秒)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_module (module),
    KEY idx_operation_type (operation_type),
    KEY idx_target_id (target_id),
    KEY idx_result (result),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 内置FTP配置表
CREATE TABLE IF NOT EXISTS built_in_ftp_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用',
    port INT NOT NULL DEFAULT 2021 COMMENT 'FTP端口',
    username VARCHAR(64) NOT NULL DEFAULT 'rpa_user' COMMENT '用户名',
    password VARCHAR(128) NOT NULL DEFAULT 'rpa_password' COMMENT '密码',
    root_directory VARCHAR(256) NOT NULL DEFAULT '/data/ftp-root' COMMENT '根目录',
    max_connections INT NOT NULL DEFAULT 10 COMMENT '最大连接数',
    idle_timeout INT NOT NULL DEFAULT 300 COMMENT '空闲超时(秒)',
    passive_mode TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否被动模式',
    passive_port_start INT DEFAULT 50000 COMMENT '被动端口起始',
    passive_port_end INT DEFAULT 50100 COMMENT '被动端口结束',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内置FTP配置';

-- 初始化示例数据
INSERT INTO ftp_config (id, config_name, host, port, username, password, scan_path, file_pattern, scan_interval, status, remark) VALUES
(1, '测试FTP服务器', '192.168.1.100', 21, 'ftpuser', 'ftppass', '/data/reports', '*.xlsx', 300, 1, '测试用FTP配置');

INSERT INTO built_in_ftp_config (id, enabled, port, username, password, root_directory, max_connections, idle_timeout, passive_mode, passive_port_start, passive_port_end) VALUES
(1, 0, 2021, 'rpa_user', 'rpa_password', '/data/ftp-root', 10, 300, 1, 50000, 50100);

INSERT INTO report_config (id, report_code, report_name, ftp_config_id, file_pattern, sheet_index, header_row, data_start_row, column_mapping, output_table, output_mode, status, remark) VALUES
(1, 'SALES_REPORT', '销售报表', 1, 'sales_*.xlsx', 0, 0, 1, '[{"excelColumn":"A","fieldName":"order_id","fieldType":"STRING"},{"excelColumn":"B","fieldName":"product_name","fieldType":"STRING"},{"excelColumn":"C","fieldName":"quantity","fieldType":"INTEGER"},{"excelColumn":"D","fieldName":"amount","fieldType":"DECIMAL"},{"excelColumn":"E","fieldName":"order_date","fieldType":"DATE"}]', 't_sales_data', 'APPEND', 1, '销售数据报表配置');
