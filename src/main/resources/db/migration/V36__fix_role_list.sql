-- 修复角色列表查询问题
-- 确保所有角色都有正确的权限分配

-- 1. 确保BOSS角色拥有所有菜单权限
SET @boss_id = (SELECT id FROM tb_role WHERE role_code = 'boss');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @boss_id, id FROM tb_menu WHERE status = 1;

-- 2. 确保root角色拥有所有菜单权限
SET @root_id = (SELECT id FROM tb_role WHERE role_code = 'root');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @root_id, id FROM tb_menu WHERE status = 1;

-- 3. 确保财务角色拥有基础菜单权限
SET @finance_id = (SELECT id FROM tb_role WHERE role_code = 'finance');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @finance_id, id FROM tb_menu 
WHERE status = 1 AND menu_code IN (
    'home', 'account', 'attendance', 'salary', 'employee', 'project', 
    'category', 'jobcategory', 'workhour:config', 'basic_config',
    'project_dir', 'system_settings', 'menu:list', 'role'
);

-- 4. 确保员工角色拥有基础菜单权限
SET @employee_id = (SELECT id FROM tb_role WHERE role_code = 'employee');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @employee_id, id FROM tb_menu 
WHERE status = 1 AND menu_code IN (
    'home', 'account', 'attendance'
);

-- 5. 确保HR角色拥有基础菜单权限
SET @hr_id = (SELECT id FROM tb_role WHERE role_code = 'hr');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @hr_id, id FROM tb_menu 
WHERE status = 1 AND menu_code IN (
    'home', 'employee', 'attendance', 'salary'
);

-- 6. 确保CW管理员角色拥有基础菜单权限
SET @cw_id = (SELECT id FROM tb_role WHERE role_code = 'cw_admin');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @cw_id, id FROM tb_menu 
WHERE status = 1 AND menu_code IN (
    'home', 'account', 'attendance', 'salary', 'employee', 'project'
);
