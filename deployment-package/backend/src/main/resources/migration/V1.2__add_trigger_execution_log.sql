-- Trigger执行日志表
CREATE TABLE IF NOT EXISTS trigger_execution_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    trigger_code VARCHAR(100) NOT NULL COMMENT '触发器编码',
    trigger_name VARCHAR(200) NOT NULL COMMENT '触发器名称',
    partition_date DATE NOT NULL COMMENT '分区日期',
    data_count INT DEFAULT 0 COMMENT '检测到的数据行数',
    trigger_status VARCHAR(20) NOT NULL COMMENT '触发状态: TRIGGERED-已触发, WAITING-等待中, SKIPPED-已跳过, FAILED-失败',
    pipeline_task_id BIGINT COMMENT '关联的Pipeline任务ID',
    error_message TEXT COMMENT '错误信息',
    retry_count INT DEFAULT 0 COMMENT '触发时的重试次数',
    execution_time DATETIME NOT NULL COMMENT '执行时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_trigger_code (trigger_code),
    KEY idx_partition_date (partition_date),
    KEY idx_execution_time (execution_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Trigger执行日志表';