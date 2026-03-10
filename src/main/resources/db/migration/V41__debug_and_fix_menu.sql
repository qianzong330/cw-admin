-- 调试并修复项目管理菜单问题

-- 1. 删除可能重复或错误的project菜单记录（保留一条）
DELETE FROM tb_role_menu WHERE menu_id IN (SELECT id FROM tb_menu WHERE menu_code = 'project');
DELETE FROM tb_menu WHERE menu_code = 'project';

-- 2. 确保工程管理目录存在
INSERT INTO tb_menu (menu_code, menu_name, menu_type, menu_url, menu_icon, sort_order, status)
SELECT 'project_dir', '工程管理', 1, NULL, 'bi-building', 20, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tb_menu WHERE menu_code = 'project_dir');

-- 3. 获取工程管理目录的ID
SET @project_dir_id = (SELECT id FROM tb_menu WHERE menu_code = 'project_dir');

-- 4. 创建项目管理菜单
INSERT INTO tb_menu (menu_code, menu_name, menu_type, menu_url, menu_icon, parent_id, sort_order, status)
VALUES ('project', '项目管理', 2, '/project/list', 'bi-folder', @project_dir_id, 1, 1);

-- 5. 获取项目管理菜单的ID
SET @project_id = (SELECT id FROM tb_menu WHERE menu_code = 'project');

-- 6. 给BOSS角色分配权限
SET @boss_id = (SELECT id FROM tb_role WHERE role_code = 'boss');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (@boss_id, @project_dir_id);
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (@boss_id, @project_id);

-- 7. 给财务角色分配权限
SET @finance_id = (SELECT id FROM tb_role WHERE role_code = 'finance');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (@finance_id, @project_dir_id);
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (@finance_id, @project_id);

-- 8. 给员工角色分配权限
SET @employee_id = (SELECT id FROM tb_role WHERE role_code = 'employee');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (@employee_id, @project_dir_id);
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (@employee_id, @project_id);

-- 9. 验证结果
-- SELECT '工程管理目录' as info, id, menu_code, menu_name, menu_type, parent_id, status FROM tb_menu WHERE menu_code = 'project_dir'
-- UNION ALL
-- SELECT '项目管理菜单' as info, id, menu_code, menu_name, menu_type, parent_id, status FROM tb_menu WHERE menu_code = 'project';
