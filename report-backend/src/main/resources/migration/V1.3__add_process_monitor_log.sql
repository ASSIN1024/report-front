-- V1.3__add_process_monitor_log.sql
-- 描述: 创建process_monitor_log表
-- 创建日期: 2026-05-07
-- 说明: 进程监控日志表，用于记录数据处理各步骤的耗时和状态

CREATE TABLE IF NOT EXISTS process_monitor_log (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    report_code varchar(64) DEFAULT NULL COMMENT '报表编码',
    file_name varchar(256) DEFAULT NULL COMMENT '文件名',
    step varchar(32) DEFAULT NULL COMMENT '处理步骤',
    status varchar(16) DEFAULT NULL COMMENT '状态: RUNNING/SUCCESS/FAILED',
    message text COMMENT '处理消息',
    duration_ms bigint DEFAULT NULL COMMENT '执行时长(毫秒)',
    start_time datetime DEFAULT NULL COMMENT '开始时间',
    end_time datetime DEFAULT NULL COMMENT '结束时间',
    PRIMARY KEY (id),
    KEY idx_report_code (report_code),
    KEY idx_file_name (file_name),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进程监控日志表';
