-- 修复BOSS角色角色管理菜单权限 V2
-- 确保role菜单有正确的URL和parent_id

-- 1. 获取系统设置目录ID
SET @system_settings_id = (SELECT id FROM tb_menu WHERE menu_code = 'system_settings');

-- 2. 更新role菜单的URL和parent_id（如果存在）
UPDATE tb_menu 
SET menu_url = '/role/list', 
    parent_id = @system_settings_id,
    menu_type = 2,
    status = 1
WHERE menu_code = 'role';

-- 3. 获取BOSS角色ID
SET @boss_role_id = (SELECT id FROM tb_role WHERE role_code = 'boss');

-- 4. 获取role菜单ID
SET @role_menu_id = (SELECT id FROM tb_menu WHERE menu_code = 'role');

-- 5. 给BOSS角色分配系统设置目录权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) 
SELECT @boss_role_id, id FROM tb_menu WHERE menu_code = 'system_settings';

-- 6. 给BOSS角色分配角色管理菜单权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) 
VALUES (@boss_role_id, @role_menu_id);

-- 7. 给root角色分配角色管理菜单权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) 
VALUES (1, @role_menu_id);

-- 8. 同时给BOSS分配menu:list权限
SET @menu_list_id = (SELECT id FROM tb_menu WHERE menu_code = 'menu:list');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) 
VALUES (@boss_role_id, @menu_list_id);
