-- V28: 确保 tb_account 表有 category_id 字段
-- 解决保存帐条时 Unknown column 'category_id' 错误

ALTER TABLE tb_account ADD COLUMN IF NOT EXISTS category_id BIGINT COMMENT '分类ID';
