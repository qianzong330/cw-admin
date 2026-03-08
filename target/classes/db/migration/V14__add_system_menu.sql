-- V14: 添加系统设置一级目录 + 菜单管理二级菜单（仅 root 可见，通过后端代码控制）

-- 1. 插入系统设置一级目录菜单（sort_order=99 排在末尾）
INSERT IGNORE INTO tb_menu (menu_code, menu_name, menu_type, parent_id, sort_order, status)
VALUES ('system_settings', '系统设置', 1, 0, 99, 1);

-- 2. 插入菜单管理二级菜单（挂在系统设置下）
INSERT IGNORE INTO tb_menu (menu_code, menu_name, menu_type, parent_id, sort_order, status)
SELECT 'menu:list', '菜单管理', 2, id, 1, 1
FROM tb_menu WHERE menu_code = 'system_settings';

-- 3. 给 root 角色（id=1）分配这两个菜单权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT 1, id FROM tb_menu WHERE menu_code IN ('system_settings', 'menu:list');
