-- 强制创建项目管理菜单（如果不存在）
-- 使用INSERT IGNORE避免重复插入错误

-- 先获取工程管理目录的ID
SET @project_dir_id = (SELECT id FROM tb_menu WHERE menu_code = 'project_dir');

-- 检查项目管理菜单是否已存在
SET @project_exists = (SELECT COUNT(*) FROM tb_menu WHERE menu_code = 'project');

-- 如果不存在，则创建
INSERT INTO tb_menu (menu_code, menu_name, menu_type, menu_url, menu_icon, parent_id, sort_order, status)
SELECT 'project', '项目管理', 2, '/project/list', 'bi-folder', @project_dir_id, 1, 1
FROM DUAL
WHERE @project_exists = 0;

-- 获取项目管理菜单ID（无论是否刚创建）
SET @project_id = (SELECT id FROM tb_menu WHERE menu_code = 'project');

-- 给BOSS角色分配权限（如果不存在）
SET @boss_id = (SELECT id FROM tb_role WHERE role_code = 'boss');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) 
SELECT @boss_id, @project_id 
WHERE @boss_id IS NOT NULL AND @project_id IS NOT NULL;

-- 给财务角色分配权限（如果不存在）
SET @finance_id = (SELECT id FROM tb_role WHERE role_code = 'finance');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) 
SELECT @finance_id, @project_id 
WHERE @finance_id IS NOT NULL AND @project_id IS NOT NULL;

-- 给员工角色分配权限（如果不存在）
SET @employee_id = (SELECT id FROM tb_role WHERE role_code = 'employee');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) 
SELECT @employee_id, @project_id 
WHERE @employee_id IS NOT NULL AND @project_id IS NOT NULL;
