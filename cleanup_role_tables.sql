-- ============================================
-- 清理并重建 tb_role 表
-- ============================================

-- 删除旧表
DROP TABLE IF EXISTS tb_role;
DROP TABLE IF EXISTS tb_role_menu;
DROP TABLE IF EXISTS tb_menu;

-- 确认表已删除
SHOW TABLES LIKE 'tb_%';
