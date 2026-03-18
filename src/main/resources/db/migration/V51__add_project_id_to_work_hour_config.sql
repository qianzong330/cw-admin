-- V51: 工时配置表添加 project_id 字段，支持按项目配置工时

-- 添加 project_id 字段
ALTER TABLE tb_work_hour_config ADD COLUMN project_id BIGINT COMMENT '项目ID' AFTER id;

-- 添加索引
CREATE INDEX idx_project_id ON tb_work_hour_config(project_id);
CREATE UNIQUE INDEX idx_project_calc_type ON tb_work_hour_config(project_id, calc_type);

-- 删除旧的 is_default 索引（如果存在）
DROP INDEX IF EXISTS idx_default ON tb_work_hour_config;

-- 删除 is_default 字段（不再需要）
ALTER TABLE tb_work_hour_config DROP COLUMN IF EXISTS is_default;
