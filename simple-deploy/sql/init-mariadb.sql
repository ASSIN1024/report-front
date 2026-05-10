-- 报表数据处理平台 - MariaDB初始化脚本
-- 版本: V2.0 (For MariaDB 10.3+)
-- 说明: 在MariaDB中执行此脚本创建数据库和表结构

-- ============================================
-- 1. 创建数据库
-- ============================================
CREATE DATABASE IF NOT EXISTS report_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE report_db;

-- ============================================
-- 2. 系统配置表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_config (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    config_key varchar(100) NOT NULL COMMENT '配置键',
    config_value varchar(500) DEFAULT NULL COMMENT '配置值',
    config_type varchar(50) DEFAULT NULL COMMENT '配置类型',
    description varchar(200) DEFAULT NULL COMMENT '描述',
    deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ============================================
-- 3. 内置FTP配置表
-- ============================================
CREATE TABLE IF NOT EXISTS built_in_ftp_config (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    enabled tinyint NOT NULL DEFAULT 0 COMMENT '是否启用',
    port int NOT NULL DEFAULT 2021 COMMENT 'FTP端口',
    username varchar(50) NOT NULL COMMENT '用户名',
    password varchar(100) NOT NULL COMMENT '密码',
    root_directory varchar(200) NOT NULL DEFAULT '/data/ftp-root' COMMENT '根目录',
    max_connections int NOT NULL DEFAULT 10 COMMENT '最大连接数',
    idle_timeout int NOT NULL DEFAULT 300 COMMENT '空闲超时(秒)',
    passive_mode tinyint NOT NULL DEFAULT 1 COMMENT '被动模式',
    passive_port_start int DEFAULT 50000 COMMENT '被动端口起始',
    passive_port_end int DEFAULT 50100 COMMENT '被动端口结束',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内置FTP配置表';

-- ============================================
-- 4. 报表配置表
-- ============================================
CREATE TABLE IF NOT EXISTS report_config (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    report_code varchar(50) NOT NULL COMMENT '报表编码',
    report_name varchar(100) NOT NULL COMMENT '报表名称',
    ftp_config_id bigint DEFAULT NULL COMMENT 'FTP配置ID',
    scan_path varchar(200) DEFAULT '/upload' COMMENT '扫描路径',
    file_pattern varchar(100) DEFAULT NULL COMMENT '文件匹配模式',
    sheet_index int NOT NULL DEFAULT 0 COMMENT 'Sheet索引',
    header_row int NOT NULL DEFAULT 0 COMMENT '表头行号',
    data_start_row int NOT NULL DEFAULT 1 COMMENT '数据起始行',
    skip_columns int NOT NULL DEFAULT 0 COMMENT '跳过列数',
    date_extract_pattern varchar(50) DEFAULT NULL COMMENT '日期提取模式',
    column_mapping text NOT NULL COMMENT '列映射JSON',
    output_table varchar(50) NOT NULL COMMENT '输出表名',
    start_row int DEFAULT 0 COMMENT '起始行',
    status tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    remark varchar(500) DEFAULT NULL COMMENT '备注',
    deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    ods_table_name varchar(128) DEFAULT NULL COMMENT 'ODS表名',
    load_mode varchar(50) DEFAULT 'partitioned-append' COMMENT '加载模式',
    start_col int DEFAULT 0 COMMENT '起始列',
    mapping_mode varchar(20) DEFAULT 'AUTO' COMMENT '映射模式',
    duplicate_col_strategy varchar(20) DEFAULT 'SKIP' COMMENT '重复列策略',
    ods_backup_enabled tinyint DEFAULT 0 COMMENT '是否启用ODS备份',
    target_table_type varchar(20) DEFAULT NULL COMMENT '目标表类型',
    target_db_name varchar(128) DEFAULT NULL COMMENT '目标数据库',
    partition_info varchar(500) DEFAULT NULL COMMENT '分区信息',
    is_overseas tinyint DEFAULT 0 COMMENT '是否海外版',
    field_type_json text COMMENT '字段类型JSON',
    spark_executor_num int DEFAULT 4 COMMENT 'Spark执行器数量',
    spark_executor_cores int DEFAULT 4 COMMENT 'Spark执行器核心数',
    spark_executor_memory varchar(20) DEFAULT '8G' COMMENT 'Spark执行器内存',
    spark_driver_num int DEFAULT 2 COMMENT 'Spark驱动数量',
    spark_driver_memory varchar(20) DEFAULT '2G' COMMENT 'Spark驱动内存',
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_code (report_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表配置表';

-- ============================================
-- 5. 已处理文件记录表
-- ============================================
CREATE TABLE IF NOT EXISTS processed_file (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    report_config_id bigint NOT NULL COMMENT '报表配置ID',
    file_name varchar(200) NOT NULL COMMENT '文件名',
    file_path varchar(500) DEFAULT NULL COMMENT '文件路径',
    file_size bigint DEFAULT NULL COMMENT '文件大小',
    pt_dt varchar(20) DEFAULT NULL COMMENT '分区日期',
    checksum varchar(64) DEFAULT NULL COMMENT '文件校验和',
    status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    batch_no varchar(50) DEFAULT NULL COMMENT '批次号',
    error_message text COMMENT '错误信息',
    process_time datetime DEFAULT NULL COMMENT '处理时间',
    task_id bigint DEFAULT NULL COMMENT '任务ID',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_report_config_id (report_config_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time),
    KEY idx_pt_dt (pt_dt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='已处理文件记录表';

-- ============================================
-- 6. 任务执行记录表
-- ============================================
CREATE TABLE IF NOT EXISTS task_execution (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_type varchar(50) NOT NULL COMMENT '任务类型',
    task_name varchar(100) NOT NULL COMMENT '任务名称',
    report_config_id bigint DEFAULT NULL COMMENT '报表配置ID',
    file_name varchar(200) DEFAULT NULL COMMENT '文件名',
    file_path varchar(500) DEFAULT NULL COMMENT '文件路径',
    pipeline_code varchar(100) DEFAULT NULL COMMENT '流水线编码',
    partition_value varchar(50) DEFAULT NULL COMMENT '分区值',
    step_name varchar(100) DEFAULT NULL COMMENT '步骤名称',
    status varchar(20) NOT NULL COMMENT '状态',
    total_rows int DEFAULT 0 COMMENT '总行数',
    success_rows int DEFAULT 0 COMMENT '成功行数',
    failed_rows int DEFAULT 0 COMMENT '失败行数',
    error_message text COMMENT '错误信息',
    start_time datetime DEFAULT NULL COMMENT '开始时间',
    end_time datetime DEFAULT NULL COMMENT '结束时间',
    duration bigint DEFAULT NULL COMMENT '耗时(毫秒)',
    deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    output_file varchar(500) DEFAULT NULL COMMENT '输出文件',
    pt_dt varchar(50) DEFAULT NULL COMMENT '分区日期',
    PRIMARY KEY (id),
    KEY idx_task_type (task_type),
    KEY idx_status (status),
    KEY idx_create_time (create_time),
    KEY idx_report_config_id (report_config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务执行记录表';

-- ============================================
-- 7. 任务执行日志表
-- ============================================
CREATE TABLE IF NOT EXISTS task_execution_log (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_execution_id bigint NOT NULL COMMENT '任务执行ID',
    log_level varchar(20) DEFAULT 'INFO' COMMENT '日志级别',
    log_message text NOT NULL COMMENT '日志消息',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_task_execution_id (task_execution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务执行日志表';

-- ============================================
-- 8. 触发器配置表
-- ============================================
CREATE TABLE IF NOT EXISTS trigger_config (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    trigger_code varchar(100) NOT NULL COMMENT '触发器编码',
    trigger_name varchar(200) NOT NULL COMMENT '触发器名称',
    source_table varchar(100) NOT NULL COMMENT '源表名',
    partition_column varchar(50) DEFAULT NULL COMMENT '分区列',
    partition_pattern varchar(50) DEFAULT NULL COMMENT '分区模式',
    poll_interval_seconds int DEFAULT 60 COMMENT '轮询间隔(秒)',
    max_retries int DEFAULT 3 COMMENT '最大重试次数',
    pipeline_code varchar(100) NOT NULL COMMENT '流水线编码',
    status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    last_trigger_time datetime DEFAULT NULL COMMENT '上次触发时间',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trigger_code (trigger_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='触发器配置表';

-- ============================================
-- 9. 触发器状态记录表
-- ============================================
CREATE TABLE IF NOT EXISTS trigger_state_record (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    trigger_code varchar(100) NOT NULL COMMENT '触发器编码',
    retry_count int DEFAULT 0 COMMENT '重试次数',
    last_check_time datetime DEFAULT NULL COMMENT '上次检查时间',
    triggered tinyint DEFAULT 0 COMMENT '是否已触发',
    instance_id varchar(100) DEFAULT NULL COMMENT '实例ID',
    version int DEFAULT 0 COMMENT '版本号',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_trigger_code (trigger_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='触发器状态记录表';

-- ============================================
-- 10. 触发器执行日志表
-- ============================================
CREATE TABLE IF NOT EXISTS trigger_execution_log (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    trigger_code varchar(100) NOT NULL COMMENT '触发器编码',
    trigger_name varchar(200) NOT NULL COMMENT '触发器名称',
    partition_date date NOT NULL COMMENT '分区日期',
    data_count int DEFAULT 0 COMMENT '数据行数',
    trigger_status varchar(20) NOT NULL COMMENT '触发状态',
    pipeline_task_id bigint DEFAULT NULL COMMENT '流水线任务ID',
    error_message text COMMENT '错误信息',
    retry_count int DEFAULT 0 COMMENT '重试次数',
    execution_time datetime NOT NULL COMMENT '执行时间',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_trigger_code (trigger_code),
    KEY idx_partition_date (partition_date),
    KEY idx_execution_time (execution_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='触发器执行日志表';

-- ============================================
-- 11. 触发器分区记录表
-- ============================================
CREATE TABLE IF NOT EXISTS trigger_partition_record (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    trigger_code varchar(100) NOT NULL COMMENT '触发器编码',
    partition_date date NOT NULL COMMENT '分区日期',
    triggered tinyint DEFAULT 0 COMMENT '是否已触发',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trigger_partition (trigger_code, partition_date),
    KEY idx_trigger_code (trigger_code),
    KEY idx_partition_date (partition_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='触发器分区记录表';

-- ============================================
-- 12. 流水线配置表
-- ============================================
CREATE TABLE IF NOT EXISTS pipeline_config (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    pipeline_code varchar(100) NOT NULL COMMENT '流水线编码',
    pipeline_name varchar(200) NOT NULL COMMENT '流水线名称',
    description varchar(500) DEFAULT NULL COMMENT '描述',
    idempotent_mode varchar(20) DEFAULT 'NONE' COMMENT '幂等模式',
    status varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pipeline_code (pipeline_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流水线配置表';

-- ============================================
-- 13. 表分层映射表
-- ============================================
CREATE TABLE IF NOT EXISTS table_layer_mapping (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    table_name varchar(100) NOT NULL COMMENT '表名',
    table_layer varchar(20) DEFAULT NULL COMMENT '表层级',
    source_type varchar(50) DEFAULT NULL COMMENT '来源类型',
    source_id bigint DEFAULT NULL COMMENT '来源ID',
    source_name varchar(200) DEFAULT NULL COMMENT '来源名称',
    business_domain varchar(200) DEFAULT NULL COMMENT '业务域',
    description varchar(500) DEFAULT NULL COMMENT '描述',
    tags json DEFAULT NULL COMMENT '标签',
    marked tinyint NOT NULL DEFAULT 0 COMMENT '是否标记',
    deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_table_name (table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表分层映射表';

-- ============================================
-- 14. 打包配置表
-- ============================================
CREATE TABLE IF NOT EXISTS packing_config (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    config_key varchar(100) NOT NULL COMMENT '配置键',
    config_value varchar(500) DEFAULT NULL COMMENT '配置值',
    config_type varchar(50) DEFAULT NULL COMMENT '配置类型',
    description varchar(200) DEFAULT NULL COMMENT '描述',
    deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打包配置表';

-- ============================================
-- 15. 打包批次表
-- ============================================
CREATE TABLE IF NOT EXISTS packing_batch (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    batch_no varchar(50) NOT NULL COMMENT '批次号',
    status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    total_size bigint DEFAULT 0 COMMENT '总大小',
    file_count int DEFAULT 0 COMMENT '文件数量',
    for_upload_path varchar(500) DEFAULT NULL COMMENT '待上传路径',
    done_dir_path varchar(500) DEFAULT NULL COMMENT '完成目录路径',
    start_time datetime DEFAULT NULL COMMENT '开始时间',
    end_time datetime DEFAULT NULL COMMENT '结束时间',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_no (batch_no),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打包批次表';

-- ============================================
-- 16. 告警记录表
-- ============================================
CREATE TABLE IF NOT EXISTS alert_record (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    alert_type varchar(20) NOT NULL COMMENT '告警类型',
    file_name varchar(200) DEFAULT NULL COMMENT '文件名',
    report_config_id bigint DEFAULT NULL COMMENT '报表配置ID',
    alert_level varchar(20) DEFAULT NULL COMMENT '告警级别',
    alert_message varchar(500) DEFAULT NULL COMMENT '告警消息',
    reason varchar(500) DEFAULT NULL COMMENT '原因',
    status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    resolve_time datetime DEFAULT NULL COMMENT '解决时间',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_alert_type (alert_type),
    KEY idx_status (status),
    KEY idx_create_time (create_time),
    KEY idx_report_config_id (report_config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警记录表';

-- ============================================
-- 17. ODS备份记录表
-- ============================================
CREATE TABLE IF NOT EXISTS ods_backup (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    source_file varchar(200) NOT NULL COMMENT '源文件',
    pt_dt varchar(20) DEFAULT NULL COMMENT '分区日期',
    db_name varchar(128) DEFAULT NULL COMMENT '数据库名',
    table_name varchar(128) DEFAULT NULL COMMENT '表名',
    report_config_id bigint DEFAULT NULL COMMENT '报表配置ID',
    file_size bigint DEFAULT NULL COMMENT '文件大小',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_pt_dt (pt_dt),
    KEY idx_source_file (source_file),
    KEY idx_report_config_id (report_config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ODS备份记录表';

-- ============================================
-- 18. 操作日志表
-- ============================================
CREATE TABLE IF NOT EXISTS operation_log (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    module varchar(50) DEFAULT NULL COMMENT '模块',
    operation_type varchar(50) DEFAULT NULL COMMENT '操作类型',
    operation_desc varchar(200) DEFAULT NULL COMMENT '操作描述',
    operator varchar(50) DEFAULT NULL COMMENT '操作人',
    ip_address varchar(50) DEFAULT NULL COMMENT 'IP地址',
    operation_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_module (module),
    KEY idx_operation_time (operation_time),
    KEY idx_operator (operator)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================
-- 19. 进程监控日志表
-- ============================================
CREATE TABLE IF NOT EXISTS process_monitor_log (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    report_code varchar(64) DEFAULT NULL COMMENT '报表编码',
    file_name varchar(256) DEFAULT NULL COMMENT '文件名',
    step varchar(32) DEFAULT NULL COMMENT '处理步骤',
    status varchar(16) DEFAULT NULL COMMENT '状态',
    message text COMMENT '处理消息',
    duration_ms bigint DEFAULT NULL COMMENT '执行时长(毫秒)',
    start_time datetime DEFAULT NULL COMMENT '开始时间',
    end_time datetime DEFAULT NULL COMMENT '结束时间',
    PRIMARY KEY (id),
    KEY idx_report_code (report_code),
    KEY idx_file_name (file_name),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='进程监控日志表';

-- ============================================
-- 初始化数据
-- ============================================

-- 内置FTP默认配置
INSERT INTO built_in_ftp_config (enabled, port, username, password, root_directory, max_connections, idle_timeout, passive_mode, passive_port_start, passive_port_end)
VALUES (0, 2021, 'rpa_user', 'rpa_password', '/data/ftp-root', 10, 300, 1, 50000, 50100);

-- 打包默认配置
INSERT INTO packing_config (config_key, config_value, config_type, description) VALUES
('max_package_size', '209715200', 'SIZE', '单个包最大大小(200MB)'),
('upload_dir', '/data/ftp-root/for-upload', 'PATH', '上传目录'),
('done_dir', '/data/ftp-root/done', 'PATH', '完成目录'),
('fixed_filename', 'outputs.zip', 'STRING', '固定文件名'),
('polling_interval', '30', 'INT', '消费轮询间隔(秒)'),
('scan_interval', '300', 'INT', '扫描间隔(秒)');

-- ============================================
-- 完成提示
-- ============================================
SELECT '========================================' AS '';
SELECT '数据库初始化完成!' AS 'Result';
SELECT '数据库名: report_db' AS '';
SELECT '默认FTP账号: rpa_user / rpa_password' AS '';
SELECT '========================================' AS '';
