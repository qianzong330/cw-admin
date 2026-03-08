-- ============================================
-- 修复 tb_role 表结构
-- ============================================

-- 检查 role_code 字段是否存在，不存在则添加
ALTER TABLE tb_role ADD COLUMN IF NOT EXISTS role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码' AFTER id;

-- 查看表结构
DESC tb_role;

-- 查看现有数据
SELECT * FROM tb_role;
