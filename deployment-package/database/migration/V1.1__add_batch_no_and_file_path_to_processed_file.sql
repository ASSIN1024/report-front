-- V1.1__add_batch_no_and_file_path_to_processed_file.sql
-- 描述: 给processed_file表添加batch_no和file_path字段
--       batch_no用于追踪文件所属打包批次，防止重复打包
--       file_path存储标准文件路径，打包时使用
-- 创建日期: 2026-05-06

-- MySQL 语法 (MySQL不支持IF NOT EXISTS在ALTER TABLE中，需手动检查)
-- 以下语句在MySQL中执行时请去掉IF NOT EXISTS前缀
ALTER TABLE processed_file ADD COLUMN batch_no VARCHAR(50) DEFAULT NULL COMMENT '打包批次号';
ALTER TABLE processed_file ADD COLUMN file_path VARCHAR(500) DEFAULT NULL COMMENT '标准文件路径';

CREATE INDEX idx_batch_no ON processed_file(batch_no);
