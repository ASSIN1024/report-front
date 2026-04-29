-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: report_db
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `QRTZ_BLOB_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_BLOB_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_BLOB_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `BLOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `SCHED_NAME` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_BLOB_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_CALENDARS`
--

DROP TABLE IF EXISTS `QRTZ_CALENDARS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_CALENDARS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `CALENDAR_NAME` varchar(190) NOT NULL,
  `CALENDAR` blob NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`CALENDAR_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_CRON_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_CRON_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_CRON_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `CRON_EXPRESSION` varchar(120) NOT NULL,
  `TIME_ZONE_ID` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_CRON_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_FIRED_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_FIRED_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_FIRED_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `ENTRY_ID` varchar(95) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `INSTANCE_NAME` varchar(190) NOT NULL,
  `FIRED_TIME` bigint NOT NULL,
  `SCHED_TIME` bigint NOT NULL,
  `PRIORITY` int NOT NULL,
  `STATE` varchar(16) NOT NULL,
  `JOB_NAME` varchar(190) DEFAULT NULL,
  `JOB_GROUP` varchar(190) DEFAULT NULL,
  `IS_NONCONCURRENT` varchar(1) DEFAULT NULL,
  `REQUESTS_RECOVERY` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`ENTRY_ID`),
  KEY `IDX_QRTZ_FT_TRIG_INST_NAME` (`SCHED_NAME`,`INSTANCE_NAME`),
  KEY `IDX_QRTZ_FT_INST_JOB_REQ_RCVRY` (`SCHED_NAME`,`INSTANCE_NAME`,`REQUESTS_RECOVERY`),
  KEY `IDX_QRTZ_FT_J_G` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_FT_JG` (`SCHED_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_FT_T_G` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_FT_TG` (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_JOB_DETAILS`
--

DROP TABLE IF EXISTS `QRTZ_JOB_DETAILS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_JOB_DETAILS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `JOB_NAME` varchar(190) NOT NULL,
  `JOB_GROUP` varchar(190) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `JOB_CLASS_NAME` varchar(250) NOT NULL,
  `IS_DURABLE` varchar(1) NOT NULL,
  `IS_NONCONCURRENT` varchar(1) NOT NULL,
  `IS_UPDATE_DATA` varchar(1) NOT NULL,
  `REQUESTS_RECOVERY` varchar(1) NOT NULL,
  `JOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_J_REQ_RECOVERY` (`SCHED_NAME`,`REQUESTS_RECOVERY`),
  KEY `IDX_QRTZ_J_GRP` (`SCHED_NAME`,`JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_LOCKS`
--

DROP TABLE IF EXISTS `QRTZ_LOCKS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_LOCKS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `LOCK_NAME` varchar(40) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`LOCK_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_PAUSED_TRIGGER_GRPS`
--

DROP TABLE IF EXISTS `QRTZ_PAUSED_TRIGGER_GRPS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_PAUSED_TRIGGER_GRPS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_SCHEDULER_STATE`
--

DROP TABLE IF EXISTS `QRTZ_SCHEDULER_STATE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_SCHEDULER_STATE` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `INSTANCE_NAME` varchar(190) NOT NULL,
  `LAST_CHECKIN_TIME` bigint NOT NULL,
  `CHECKIN_INTERVAL` bigint NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`INSTANCE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_SIMPLE_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_SIMPLE_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_SIMPLE_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `REPEAT_COUNT` bigint NOT NULL,
  `REPEAT_INTERVAL` bigint NOT NULL,
  `TIMES_TRIGGERED` bigint NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_SIMPLE_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_SIMPROP_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_SIMPROP_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_SIMPROP_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `STR_PROP_1` varchar(512) DEFAULT NULL,
  `STR_PROP_2` varchar(512) DEFAULT NULL,
  `STR_PROP_3` varchar(512) DEFAULT NULL,
  `INT_PROP_1` int DEFAULT NULL,
  `INT_PROP_2` int DEFAULT NULL,
  `LONG_PROP_1` bigint DEFAULT NULL,
  `LONG_PROP_2` bigint DEFAULT NULL,
  `DEC_PROP_1` decimal(13,4) DEFAULT NULL,
  `DEC_PROP_2` decimal(13,4) DEFAULT NULL,
  `BOOL_PROP_1` varchar(1) DEFAULT NULL,
  `BOOL_PROP_2` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `QRTZ_SIMPROP_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `QRTZ_TRIGGERS` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `QRTZ_TRIGGERS`
--

DROP TABLE IF EXISTS `QRTZ_TRIGGERS`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `QRTZ_TRIGGERS` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `JOB_NAME` varchar(190) NOT NULL,
  `JOB_GROUP` varchar(190) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `NEXT_FIRE_TIME` bigint DEFAULT NULL,
  `PREV_FIRE_TIME` bigint DEFAULT NULL,
  `PRIORITY` int DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) NOT NULL,
  `TRIGGER_TYPE` varchar(8) NOT NULL,
  `START_TIME` bigint NOT NULL,
  `END_TIME` bigint DEFAULT NULL,
  `CALENDAR_NAME` varchar(190) DEFAULT NULL,
  `MISFIRE_INSTR` smallint DEFAULT NULL,
  `JOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_T_J` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_T_JG` (`SCHED_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_T_C` (`SCHED_NAME`,`CALENDAR_NAME`),
  KEY `IDX_QRTZ_T_G` (`SCHED_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_T_STATE` (`SCHED_NAME`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_N_STATE` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_N_G_STATE` (`SCHED_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_NEXT_FIRE_TIME` (`SCHED_NAME`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_ST` (`SCHED_NAME`,`TRIGGER_STATE`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_ST_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_NFT_ST_MISFIRE_GRP` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  CONSTRAINT `QRTZ_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) REFERENCES `QRTZ_JOB_DETAILS` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `built_in_ftp_config`
--

DROP TABLE IF EXISTS `built_in_ftp_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `built_in_ftp_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enabled` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否启用',
  `port` int NOT NULL DEFAULT '2021' COMMENT 'FTP端口',
  `username` varchar(64) NOT NULL DEFAULT 'rpa_user' COMMENT '用户名',
  `password` varchar(128) NOT NULL DEFAULT 'rpa_password' COMMENT '密码',
  `root_directory` varchar(256) NOT NULL DEFAULT '/data/ftp-root' COMMENT '根目录',
  `max_connections` int NOT NULL DEFAULT '10' COMMENT '最大连接数',
  `idle_timeout` int NOT NULL DEFAULT '300' COMMENT '空闲超时(秒)',
  `passive_mode` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否被动模式',
  `passive_port_start` int DEFAULT '50000' COMMENT '被动端口起始',
  `passive_port_end` int DEFAULT '50100' COMMENT '被动端口结束',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内置FTP配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `operation_log`
--

DROP TABLE IF EXISTS `operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `module` varchar(50) NOT NULL COMMENT '操作模块',
  `operation_type` varchar(20) NOT NULL COMMENT '操作类型: CREATE-创建, UPDATE-修改, DELETE-删除, TEST-测试',
  `operation_desc` varchar(200) NOT NULL COMMENT '操作描述',
  `target_id` varchar(50) DEFAULT NULL COMMENT '目标ID',
  `target_name` varchar(100) DEFAULT NULL COMMENT '目标名称',
  `before_data` text COMMENT '操作前数据(JSON)',
  `after_data` text COMMENT '操作后数据(JSON)',
  `result` tinyint NOT NULL DEFAULT '1' COMMENT '操作结果: 0-失败, 1-成功',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `operator_ip` varchar(50) DEFAULT NULL COMMENT '操作者IP',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作者名称',
  `duration` bigint DEFAULT NULL COMMENT '执行时长(毫秒)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_module` (`module`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_target_id` (`target_id`),
  KEY `idx_result` (`result`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pipeline_config`
--

DROP TABLE IF EXISTS `pipeline_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pipeline_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `pipeline_code` varchar(100) NOT NULL COMMENT '流水线编码',
  `pipeline_name` varchar(200) NOT NULL COMMENT '流水线名称',
  `description` varchar(500) DEFAULT NULL COMMENT '流水线描述',
  `idempotent_mode` varchar(20) NOT NULL DEFAULT 'OVERWRITE' COMMENT '幂等模式: OVERWRITE/APPEND',
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pipeline_code` (`pipeline_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流水线配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `processed_file`
--

DROP TABLE IF EXISTS `processed_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `processed_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `report_config_id` bigint NOT NULL COMMENT '报表配置ID',
  `file_name` varchar(200) NOT NULL COMMENT '文件名',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小',
  `checksum` varchar(64) DEFAULT NULL COMMENT '文件校验和',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待处理, PROCESSING-处理中, PROCESSED-已处理, FAILED-失败',
  `error_message` text COMMENT '错误信息',
  `process_time` datetime DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_file` (`report_config_id`,`file_name`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='已处理文件记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report_config`
--

DROP TABLE IF EXISTS `report_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report_config` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `report_code` varchar(50) NOT NULL COMMENT '报表编码',
  `report_name` varchar(100) NOT NULL COMMENT '报表名称',
  `ftp_config_id` bigint DEFAULT NULL COMMENT '关联FTP配置ID(已废弃,仅保留兼容)',
  `scan_path` varchar(200) DEFAULT '/upload' COMMENT '扫描路径',
  `file_pattern` varchar(100) DEFAULT NULL COMMENT '文件匹配模式',
  `sheet_index` int NOT NULL DEFAULT '0' COMMENT 'Sheet索引',
  `header_row` int NOT NULL DEFAULT '0' COMMENT '表头行号',
  `data_start_row` int NOT NULL DEFAULT '1' COMMENT '数据起始行',
  `skip_columns` int NOT NULL DEFAULT '0' COMMENT '跳过前N列',
  `date_extract_pattern` varchar(50) DEFAULT NULL COMMENT '日期提取规则: AUTO/yyyyMMdd/yyyy-MM-dd等',
  `column_mapping` text NOT NULL COMMENT '列映射配置(JSON)',
  `output_table` varchar(50) NOT NULL COMMENT '输出表名',
  `output_mode` varchar(20) NOT NULL DEFAULT 'APPEND' COMMENT '输出模式: APPEND-追加, OVERWRITE-覆盖',
  `start_row` int DEFAULT '0' COMMENT '起始行',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用, 1-启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `ods_table_name` varchar(128) DEFAULT NULL COMMENT 'ODS备份表名',
  `load_mode` varchar(50) DEFAULT 'partitioned-append' COMMENT '数据载入模式',
  `start_col` int DEFAULT '0' COMMENT '起始列',
  `mapping_mode` varchar(20) DEFAULT 'AUTO' COMMENT '映射模式',
  `duplicate_col_strategy` varchar(20) DEFAULT 'SKIP' COMMENT '重复列策略',
  `ods_backup_enabled` tinyint DEFAULT '0' COMMENT '是否ODS备份',
  `target_table_type` varchar(20) DEFAULT NULL COMMENT '目标表类型: hive/mpp',
  `target_db_name` varchar(128) DEFAULT NULL COMMENT '目标库名',
  `is_overseas` tinyint DEFAULT '0' COMMENT '是否境外: 0-否, 1-是',
  `field_type_json` text COMMENT '字段类型JSON',
  `spark_executor_num` int DEFAULT '4' COMMENT 'Spark executor数量',
  `spark_executor_cores` int DEFAULT '4' COMMENT 'Spark executor核数',
  `spark_executor_memory` varchar(20) DEFAULT '8G' COMMENT 'Spark executor内存',
  `spark_driver_num` int DEFAULT '2' COMMENT 'Spark driver数量',
  `spark_driver_memory` varchar(20) DEFAULT '2G' COMMENT 'Spark driver内存',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_code` (`report_code`),
  KEY `idx_report_name` (`report_name`),
  KEY `idx_ftp_config_id` (`ftp_config_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报表配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` varchar(500) DEFAULT NULL COMMENT '配置值',
  `config_type` varchar(50) DEFAULT NULL COMMENT '配置类型',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `table_layer_mapping`
--

DROP TABLE IF EXISTS `table_layer_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `table_layer_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `table_name` varchar(100) NOT NULL COMMENT '数据库表名',
  `table_layer` varchar(20) DEFAULT NULL COMMENT '流向分层: OSD/DWD/DWS/ADS',
  `source_type` varchar(50) DEFAULT NULL COMMENT '来源类型: FTP来源/PIPELINE',
  `source_id` bigint DEFAULT NULL COMMENT '关联来源ID',
  `source_name` varchar(200) DEFAULT NULL COMMENT '关联来源名称',
  `business_domain` varchar(200) DEFAULT NULL COMMENT '业务域描述',
  `description` varchar(500) DEFAULT NULL COMMENT '表描述',
  `tags` json DEFAULT NULL COMMENT '自定义标签',
  `marked` tinyint NOT NULL DEFAULT '0' COMMENT '是否已标记: 0-未标记, 1-已标记',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table_name` (`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表分层映射表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `task_execution`
--

DROP TABLE IF EXISTS `task_execution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_execution` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_type` varchar(50) NOT NULL COMMENT '任务类型',
  `task_name` varchar(100) NOT NULL COMMENT '任务名称',
  `report_config_id` bigint DEFAULT NULL COMMENT '关联报表配置ID',
  `file_name` varchar(200) DEFAULT NULL COMMENT '处理文件名',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径',
  `pipeline_code` varchar(100) DEFAULT NULL COMMENT '流水线编码',
  `partition_value` varchar(50) DEFAULT NULL COMMENT '分区值',
  `step_name` varchar(100) DEFAULT NULL COMMENT '当前步骤名称',
  `status` varchar(20) NOT NULL COMMENT '状态: PENDING-待执行, RUNNING-执行中, SUCCESS-成功, FAILED-失败',
  `total_rows` int DEFAULT '0' COMMENT '总行数',
  `success_rows` int DEFAULT '0' COMMENT '成功行数',
  `failed_rows` int DEFAULT '0' COMMENT '失败行数',
  `error_message` text COMMENT '错误信息',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` bigint DEFAULT NULL COMMENT '执行时长(毫秒)',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记: 0-未删除, 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `output_file` varchar(500) DEFAULT NULL COMMENT '输出文件',
  `pt_dt` varchar(50) DEFAULT NULL COMMENT '分区日期',
  PRIMARY KEY (`id`),
  KEY `idx_task_type` (`task_type`),
  KEY `idx_report_config_id` (`report_config_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务执行记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `task_execution_log`
--

DROP TABLE IF EXISTS `task_execution_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_execution_log` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_execution_id` bigint NOT NULL COMMENT '任务执行ID',
  `log_level` varchar(20) NOT NULL COMMENT '日志级别: INFO, WARN, ERROR',
  `log_message` text COMMENT '日志内容',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_execution_id` (`task_execution_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='任务执行日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trigger_config`
--

DROP TABLE IF EXISTS `trigger_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trigger_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `trigger_code` varchar(100) NOT NULL COMMENT '触发器编码',
  `trigger_name` varchar(200) NOT NULL COMMENT '触发器名称',
  `source_table` varchar(100) NOT NULL COMMENT '监听目标表',
  `partition_column` varchar(50) NOT NULL DEFAULT 'pt_dt' COMMENT '分区字段',
  `partition_pattern` varchar(50) DEFAULT NULL COMMENT '分区值模式，支持日期格式如 yyyy-MM-dd',
  `poll_interval_seconds` int NOT NULL DEFAULT '60' COMMENT '轮询间隔(秒)',
  `max_retries` int NOT NULL DEFAULT '60' COMMENT '最大重试次数',
  `pipeline_code` varchar(100) NOT NULL COMMENT '触发执行的Pipeline编码',
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
  `last_trigger_time` datetime DEFAULT NULL COMMENT '最后触发时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trigger_code` (`trigger_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='触发器配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trigger_partition_record`
--

DROP TABLE IF EXISTS `trigger_partition_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trigger_partition_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `trigger_code` varchar(100) NOT NULL COMMENT '触发器编码',
  `partition_date` date NOT NULL COMMENT '分区日期',
  `triggered` tinyint(1) DEFAULT '0' COMMENT '是否已触发: 0-未触发, 1-已触发',
  `pipeline_task_id` bigint DEFAULT NULL COMMENT 'Pipeline任务ID',
  `trigger_time` datetime DEFAULT NULL COMMENT '触发时间',
  `status` varchar(20) DEFAULT 'TRIGGERING' COMMENT '状态: TRIGGERING/TRIGGERED',
  `instance_id` varchar(200) DEFAULT NULL COMMENT '执行的实例标识',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trigger_partition` (`trigger_code`,`partition_date`),
  KEY `idx_status` (`status`),
  KEY `idx_trigger_time` (`trigger_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='触发器分区记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trigger_state_record`
--

DROP TABLE IF EXISTS `trigger_state_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trigger_state_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `trigger_code` varchar(100) NOT NULL COMMENT '触发器编码',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '重试次数',
  `last_check_time` datetime DEFAULT NULL COMMENT '最后检查时间',
  `triggered` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已触发: 0-未触发, 1-已触发',
  `instance_id` varchar(200) DEFAULT NULL COMMENT '实例标识',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trigger_code` (`trigger_code`),
  KEY `idx_triggered` (`triggered`),
  KEY `idx_instance_id` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='触发器状态持久化表';

--
-- Table structure for table `packing_config`
--
DROP TABLE IF EXISTS `packing_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `packing_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` varchar(500) DEFAULT NULL COMMENT '配置值',
  `config_type` varchar(50) DEFAULT NULL COMMENT '配置类型',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打包配置表';

--
-- Table structure for table `packing_batch`
--
DROP TABLE IF EXISTS `packing_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `packing_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_no` varchar(50) NOT NULL COMMENT '批次号',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待打包, UPLOADING-上传中, CONSUMING-消费中, DONE-已完成',
  `total_size` bigint DEFAULT 0 COMMENT '总大小(字节)',
  `file_count` int DEFAULT 0 COMMENT '文件数量',
  `for_upload_path` varchar(500) DEFAULT NULL COMMENT '上传路径',
  `done_dir_path` varchar(500) DEFAULT NULL COMMENT 'Done目录路径',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_no` (`batch_no`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='打包批次表';

--
-- Table structure for table `alert_record`
--
DROP TABLE IF EXISTS `alert_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alert_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `alert_type` varchar(20) NOT NULL COMMENT '告警类型',
  `file_name` varchar(200) DEFAULT NULL COMMENT '相关文件名',
  `report_config_id` bigint DEFAULT NULL COMMENT '关联报表配置ID',
  `reason` varchar(500) DEFAULT NULL COMMENT '告警原因',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待处理, RESOLVED-已解决, IGNORED-已忽略',
  `resolve_time` datetime DEFAULT NULL COMMENT '解决时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_alert_type` (`alert_type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='告警记录表';

/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

/* Dump completed on 2026-04-28 */
