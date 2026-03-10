-- 直接插入项目管理菜单（不使用变量，避免MySQL变量问题）
-- 使用硬编码的project_dir_id=68

-- 检查并插入项目管理菜单
INSERT IGNORE INTO tb_menu (id, menu_code, menu_name, menu_type, menu_url, menu_icon, parent_id, sort_order, status)
VALUES (100, 'project', '项目管理', 2, '/project/list', 'bi-folder', 68, 1, 1);

-- 如果ID 100已被占用，使用下一个可用ID
SET @new_id = (SELECT id FROM tb_menu WHERE menu_code = 'project');

-- 给BOSS角色分配权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) 
SELECT (SELECT id FROM tb_role WHERE role_code = 'boss'), @new_id
WHERE @new_id IS NOT NULL;

-- 给财务角色分配权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) 
SELECT (SELECT id FROM tb_role WHERE role_code = 'finance'), @new_id
WHERE @new_id IS NOT NULL;

-- 给员工角色分配权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) 
SELECT (SELECT id FROM tb_role WHERE role_code = 'employee'), @new_id
WHERE @new_id IS NOT NULL;
