-- 检查并修复角色数据
-- 1. 检查BOSS角色是否存在，不存在则创建
INSERT IGNORE INTO tb_role (role_code, role_name, remark) 
VALUES ('boss', 'BOSS', '系统管理员角色，拥有所有权限');

-- 2. 检查财务角色是否存在，不存在则创建
INSERT IGNORE INTO tb_role (role_code, role_name, remark) 
VALUES ('finance', '财务', '财务角色，负责记账审批和工资发放');

-- 3. 检查员工角色是否存在，不存在则创建
INSERT IGNORE INTO tb_role (role_code, role_name, remark) 
VALUES ('employee', '员工', '普通员工角色，只能发起记账申请');

-- 4. 给所有角色分配所有菜单权限（简化处理）
-- 先获取所有启用的菜单
-- 给BOSS角色分配所有菜单权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT (SELECT id FROM tb_role WHERE role_code = 'boss'), id 
FROM tb_menu WHERE status = 1;

-- 给财务角色分配基础菜单权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT (SELECT id FROM tb_role WHERE role_code = 'finance'), id 
FROM tb_menu WHERE status = 1 AND menu_code IN (
    'home', 'account', 'attendance', 'salary', 'employee', 'project', 
    'category', 'jobcategory', 'workhour:config'
);

-- 给员工角色分配基础菜单权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT (SELECT id FROM tb_role WHERE role_code = 'employee'), id 
FROM tb_menu WHERE status = 1 AND menu_code IN (
    'home', 'account', 'attendance'
);
