-- V1.0__create_processed_file.sql
-- 描述: 创建已处理文件记录表，用于FTP文件去重
-- 创建日期: 2026-04-03

CREATE TABLE IF NOT EXISTS processed_file (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    report_config_id    BIGINT NOT NULL COMMENT '报表配置ID',
    file_name           VARCHAR(200) NOT NULL COMMENT '文件名（不含路径）',
    file_size           BIGINT COMMENT '文件大小（字节）',
    pt_dt               DATE COMMENT '数据分区日期',
    status              VARCHAR(20) DEFAULT 'PROCESSED' COMMENT '处理状态：PROCESSED-已处理，FAILED-处理失败',
    task_id             BIGINT COMMENT '关联任务ID',
    error_message       TEXT COMMENT '错误信息',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_report_file (report_config_id, file_name),
    INDEX idx_pt_dt (pt_dt),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已处理文件记录表';
