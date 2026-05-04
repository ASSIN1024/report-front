CREATE TABLE `process_monitor_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `report_code` varchar(64) DEFAULT NULL COMMENT '报表编码',
  `file_name` varchar(256) DEFAULT NULL COMMENT '文件名',
  `step` varchar(32) DEFAULT NULL COMMENT '处理步骤',
  `status` varchar(16) DEFAULT NULL COMMENT '状态',
  `message` text DEFAULT NULL COMMENT '详细信息',
  `duration_ms` bigint(20) DEFAULT NULL COMMENT '耗时',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`),
  KEY `idx_report_code` (`report_code`),
  KEY `idx_file_name` (`file_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;