-- V1.1__add_batch_no_to_processed_file.sql
-- 描述: 给processed_file表添加batch_no字段，用于追踪文件所属打包批次
-- 创建日期: 2026-05-06

ALTER TABLE processed_file ADD COLUMN batch_no VARCHAR(50) DEFAULT NULL COMMENT '打包批次号';

CREATE INDEX idx_batch_no ON processed_file(batch_no);
