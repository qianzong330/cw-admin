-- 重新创建角色管理菜单和权限
-- 因为 RemoveDuplicateRoleMenu 删除了 role 菜单，需要重新创建

-- 1. 获取系统设置目录ID
SET @system_settings_id = (SELECT id FROM tb_menu WHERE menu_code = 'system_settings');

-- 2. 检查role菜单是否存在，不存在则创建
SET @role_count = (SELECT COUNT(*) FROM tb_menu WHERE menu_code = 'role');

-- 3. 如果不存在，插入新的role菜单
INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status)
SELECT 'role', '角色管理', @system_settings_id, 2, '/role/list', 'bi-person-badge', 0, 1
WHERE @role_count = 0;

-- 4. 如果已存在，更新为正确的配置
UPDATE tb_menu 
SET menu_name = '角色管理',
    parent_id = @system_settings_id,
    menu_type = 2,
    menu_url = '/role/list',
    menu_icon = 'bi-person-badge',
    sort_order = 0,
    status = 1
WHERE menu_code = 'role';

-- 5. 获取role菜单ID
SET @role_menu_id = (SELECT id FROM tb_menu WHERE menu_code = 'role');

-- 6. 获取BOSS角色ID
SET @boss_role_id = (SELECT id FROM tb_role WHERE role_code = 'boss');

-- 7. 给root角色(1)分配角色管理菜单权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (1, @role_menu_id);

-- 8. 给BOSS角色分配系统设置目录权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) 
SELECT @boss_role_id, id FROM tb_menu WHERE menu_code = 'system_settings';

-- 9. 给BOSS角色分配角色管理菜单权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (@boss_role_id, @role_menu_id);

-- 10. 给BOSS角色分配菜单管理权限
SET @menu_list_id = (SELECT id FROM tb_menu WHERE menu_code = 'menu:list');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (@boss_role_id, @menu_list_id);
