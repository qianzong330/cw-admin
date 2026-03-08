-- 删除分类表的level字段
-- 执行前请确保数据已备份

-- 1. 首先检查level字段是否存在
-- SHOW COLUMNS FROM tb_category LIKE 'level';

-- 2. 删除level字段
ALTER TABLE tb_category DROP COLUMN IF EXISTS level;

-- 3. 验证删除结果
-- SHOW COLUMNS FROM tb_category;
