-- 强制创建BOSS角色并分配所有权限
-- 如果BOSS角色不存在，创建它
INSERT INTO tb_role (role_code, role_name, remark) 
SELECT 'boss', 'BOSS', '系统管理员角色，拥有所有权限'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tb_role WHERE role_code = 'boss');

-- 如果财务角色不存在，创建它
INSERT INTO tb_role (role_code, role_name, remark) 
SELECT 'finance', '财务', '财务角色，负责记账审批和工资发放'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tb_role WHERE role_code = 'finance');

-- 如果员工角色不存在，创建它
INSERT INTO tb_role (role_code, role_name, remark) 
SELECT 'employee', '员工', '普通员工角色，只能发起记账申请'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tb_role WHERE role_code = 'employee');

-- 获取BOSS角色ID
SET @boss_id = (SELECT id FROM tb_role WHERE role_code = 'boss');

-- 给BOSS角色分配所有启用菜单的权限（使用INSERT IGNORE避免重复）
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @boss_id, id FROM tb_menu WHERE status = 1;

-- 获取root角色ID
SET @root_id = (SELECT id FROM tb_role WHERE role_code = 'root');

-- 如果root角色没有权限，分配所有权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @root_id, id FROM tb_menu WHERE status = 1;

-- 获取财务角色ID
SET @finance_id = (SELECT id FROM tb_role WHERE role_code = 'finance');

-- 给财务角色分配基础权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @finance_id, id FROM tb_menu 
WHERE status = 1 AND menu_code IN (
    'home', 'account', 'attendance', 'salary', 'employee', 'project', 
    'category', 'jobcategory', 'workhour:config', 'basic_config',
    'project_dir', 'system_settings'
);

-- 获取员工角色ID
SET @employee_id = (SELECT id FROM tb_role WHERE role_code = 'employee');

-- 给员工角色分配基础权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @employee_id, id FROM tb_menu 
WHERE status = 1 AND menu_code IN (
    'home', 'account', 'attendance'
);
