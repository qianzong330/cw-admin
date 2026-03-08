-- V27: 给 BOSS 角色分配基础配置目录及其子菜单权限

-- 1. 给 BOSS 角色分配基础配置目录权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM tb_role r, tb_menu m
WHERE r.role_code = 'boss' AND m.menu_code = 'basic_config';

-- 2. 给 BOSS 角色分配基础配置下的所有子菜单权限（角色管理、工种管理、项目管理、费用分类、工时配置）
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM tb_role r, tb_menu m
WHERE r.role_code = 'boss' 
  AND m.parent_id = (SELECT id FROM tb_menu WHERE menu_code = 'basic_config');

-- 3. 给 BOSS 角色分配角色管理相关按钮权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM tb_role r, tb_menu m
WHERE r.role_code = 'boss' 
  AND m.menu_code IN ('role:add', 'role:edit', 'role:delete', 'role:assign');
