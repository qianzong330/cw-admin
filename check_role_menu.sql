-- 检查角色管理菜单和权限
-- 1. 检查role菜单
SELECT id, menu_code, menu_name, menu_url, parent_id, menu_type, status 
FROM tb_menu 
WHERE menu_code = 'role';

-- 2. 检查系统设置目录
SELECT id, menu_code, menu_name, menu_url, parent_id, menu_type, status 
FROM tb_menu 
WHERE menu_code = 'sys_settings';

-- 3. 检查BOSS角色的权限
SELECT r.role_name, m.menu_code, m.menu_name 
FROM tb_role_menu rm
JOIN tb_role r ON rm.role_id = r.id
JOIN tb_menu m ON rm.menu_id = m.id
WHERE r.role_code = 'boss'
ORDER BY m.menu_code;

-- 4. 检查root角色的权限（role相关）
SELECT r.role_name, m.menu_code, m.menu_name 
FROM tb_role_menu rm
JOIN tb_role r ON rm.role_id = r.id
JOIN tb_menu m ON rm.menu_id = m.id
WHERE r.id = 1 AND m.menu_code LIKE '%role%'
ORDER BY m.menu_code;
