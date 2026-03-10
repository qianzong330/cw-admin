-- 检查并修复项目管理菜单
-- 1. 确保工程管理目录存在且状态正确
INSERT INTO tb_menu (menu_code, menu_name, menu_type, menu_url, menu_icon, sort_order, status)
SELECT 'project_dir', '工程管理', 1, NULL, 'bi-building', 20, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tb_menu WHERE menu_code = 'project_dir');

-- 2. 更新工程管理目录状态为启用
UPDATE tb_menu SET status = 1 WHERE menu_code = 'project_dir';

-- 3. 确保项目管理菜单存在
INSERT INTO tb_menu (menu_code, menu_name, menu_type, menu_url, menu_icon, parent_id, sort_order, status)
SELECT 'project', '项目管理', 2, '/project/list', 'bi-folder', 
       (SELECT id FROM tb_menu WHERE menu_code = 'project_dir'), 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tb_menu WHERE menu_code = 'project');

-- 4. 更新项目管理菜单的父菜单和状态
UPDATE tb_menu 
SET parent_id = (SELECT id FROM tb_menu WHERE menu_code = 'project_dir'),
    status = 1,
    menu_type = 2,
    menu_url = '/project/list'
WHERE menu_code = 'project';

-- 5. 获取BOSS角色ID并分配权限
SET @boss_role_id = (SELECT id FROM tb_role WHERE role_code = 'boss');
SET @project_dir_id = (SELECT id FROM tb_menu WHERE menu_code = 'project_dir');
SET @project_id = (SELECT id FROM tb_menu WHERE menu_code = 'project');

-- 6. 给BOSS角色分配工程管理目录权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) 
SELECT @boss_role_id, @project_dir_id 
WHERE @boss_role_id IS NOT NULL AND @project_dir_id IS NOT NULL;

-- 7. 给BOSS角色分配项目管理菜单权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) 
SELECT @boss_role_id, @project_id 
WHERE @boss_role_id IS NOT NULL AND @project_id IS NOT NULL;

-- 8. 输出调试信息（查看菜单结构）
-- SELECT m.id, m.menu_code, m.menu_name, m.menu_type, m.parent_id, m.status, m.sort_order
-- FROM tb_menu m
-- WHERE m.menu_code IN ('project_dir', 'project')
-- ORDER BY m.sort_order;
