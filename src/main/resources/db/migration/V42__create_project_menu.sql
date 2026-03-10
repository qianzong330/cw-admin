-- 创建项目管理菜单
-- 先获取工程管理目录的ID
SET @project_dir_id = (SELECT id FROM tb_menu WHERE menu_code = 'project_dir');

-- 插入项目管理菜单
INSERT INTO tb_menu (menu_code, menu_name, menu_type, menu_url, menu_icon, parent_id, sort_order, status)
VALUES ('project', '项目管理', 2, '/project/list', 'bi-folder', @project_dir_id, 1, 1);

-- 获取新创建的项目管理菜单ID
SET @project_id = LAST_INSERT_ID();

-- 给BOSS角色分配权限
SET @boss_id = (SELECT id FROM tb_role WHERE role_code = 'boss');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (@boss_id, @project_id);

-- 给财务角色分配权限
SET @finance_id = (SELECT id FROM tb_role WHERE role_code = 'finance');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (@finance_id, @project_id);

-- 给员工角色分配权限
SET @employee_id = (SELECT id FROM tb_role WHERE role_code = 'employee');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES (@employee_id, @project_id);
