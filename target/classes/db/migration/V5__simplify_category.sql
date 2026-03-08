-- 简化分类表结构，删除 parent_id 字段
-- 注意：执行前请备份数据

-- 1. 先删除 tb_account 表中的外键关联（如果存在）
-- 由于我们修改了字段名，需要删除旧字段

-- 2. 修改 tb_category 表，删除 parent_id 字段
ALTER TABLE tb_category DROP COLUMN IF EXISTS parent_id;

-- 3. 修改 tb_account 表，删除旧的分类字段，添加新的分类字段
ALTER TABLE tb_account DROP COLUMN IF EXISTS category_level1_id;
ALTER TABLE tb_account DROP COLUMN IF EXISTS category_level2_id;
ALTER TABLE tb_account ADD COLUMN IF NOT EXISTS category_id BIGINT COMMENT '分类ID';
