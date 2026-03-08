-- V25: 修复 BOSS 角色菜单权限缺失问题

-- 1. 给 BOSS 角色分配基础配置目录权限（如果不存在）
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM tb_role r, tb_menu m
WHERE r.role_code = 'boss' AND m.menu_code = 'basic_config';

-- 2. 给 BOSS 角色分配基础配置下的所有子菜单权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM tb_role r, tb_menu m
WHERE r.role_code = 'boss' 
  AND m.parent_id = (SELECT id FROM tb_menu WHERE menu_code = 'basic_config');

-- 3. 给 BOSS 角色分配工程管理目录权限（如果不存在）
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM tb_role r, tb_menu m
WHERE r.role_code = 'boss' AND m.menu_code = 'project_manage';

-- 4. 给 BOSS 角色分配工程管理下的所有子菜单权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM tb_role r, tb_menu m
WHERE r.role_code = 'boss' 
  AND m.parent_id = (SELECT id FROM tb_menu WHERE menu_code = 'project_manage');
