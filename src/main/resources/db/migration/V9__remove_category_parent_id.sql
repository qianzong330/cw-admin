-- 删除 tb_category 表的 parent_id 字段
ALTER TABLE tb_category DROP COLUMN IF EXISTS parent_id;
