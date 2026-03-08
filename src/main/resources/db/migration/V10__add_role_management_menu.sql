-- 在系统管理目录下添加角色管理菜单
INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, url, icon, sort_order, status) VALUES
('role', '角色管理', (SELECT id FROM (SELECT id FROM tb_menu WHERE menu_code='system') AS tmp), 2, '/role/list', 'bi-shield-lock', 1, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);

-- 为 root 角色添加角色管理菜单权限
INSERT INTO tb_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM tb_role r, tb_menu m 
WHERE r.role_code = 'root' AND m.menu_code = 'role'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
