CREATE TABLE IF NOT EXISTS dwd_tets_fdkow (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '名称',
    amount DECIMAL(15,2) DEFAULT 0 COMMENT '金额',
    pt_dt DATE NOT NULL COMMENT '分区日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_pt_dt (pt_dt),
    KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TestFlow清洗表';


CREATE TABLE IF NOT EXISTS dwd_tets_fdkow_agg (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '名称',
    total_amount DECIMAL(15,2) DEFAULT 0 COMMENT '总金额',
    pt_dt DATE NOT NULL COMMENT '分区日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_pt_dt (pt_dt),
    KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TestFlow聚合表';


INSERT INTO trigger_config (
    trigger_code,
    trigger_name,
    source_table,
    pipeline_code,
    poll_interval_seconds,
    status,
    create_time
) VALUES (
    'test_flow_trigger',
    '测试数据触发器',
    'test_flow',
    'test_flow_pipeline',
    60,
    'ENABLED',
    NOW()
);

SELECT * FROM test_flow WHERE pt_dt = '2026-04-06';

SELECT * FROM dwd_tets_fdkow WHERE pt_dt = '2026-04-06';

SELECT * FROM dwd_tets_fdkow_agg WHERE pt_dt = '2026-04-06';