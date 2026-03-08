-- V12: 重构菜单权限体系
-- 将各模块统一为"模块入口 + 操作按钮"两层结构
-- 每个列表页面的增删改查都可通过权限配置精细控制

-- ============================
-- 1. 将 account:add 改为按钮权限（type=3，挂在 account:list id=3 下）
-- ============================
UPDATE tb_menu SET menu_type=3, parent_id=3, url=NULL WHERE menu_code='account:add';

-- ============================
-- 2. 补充 account 模块的编辑/删除按钮（parent=account:list id=3）
-- ============================
INSERT INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES
('account:edit',   '编辑帐条', 3, 3, 2, 1),
('account:delete', '删除帐条', 3, 3, 3, 1),
('account:approve','审批帐条', 3, 3, 4, 1);

-- ============================
-- 3. 考勤管理按钮（attendance:list id=12）
-- ============================
INSERT INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES
('attendance:add',    '新增考勤', 3, 12, 1, 1),
('attendance:edit',   '编辑考勤', 3, 12, 2, 1),
('attendance:delete', '删除考勤', 3, 12, 3, 1);

-- ============================
-- 4. 工资条管理按钮（salary:list id=14）
-- ============================
INSERT INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES
('salary:add',    '新增工资条', 3, 14, 1, 1),
('salary:edit',   '编辑工资条', 3, 14, 2, 1),
('salary:delete', '删除工资条', 3, 14, 3, 1),
('salary:pay',    '发放工资条', 3, 14, 4, 1);

-- ============================
-- 5. 工时管理 - 将旧权限码重命名为更直观的形式（parent=workhour:config:list id=16）
-- ============================
UPDATE tb_menu SET menu_code='workhour:add',        menu_name='新增配置' WHERE menu_code='workhour:config:add';
UPDATE tb_menu SET menu_code='workhour:edit',       menu_name='编辑配置' WHERE menu_code='workhour:config:edit';
UPDATE tb_menu SET menu_code='workhour:delete',     menu_name='删除配置' WHERE menu_code='workhour:config:delete';
UPDATE tb_menu SET menu_code='workhour:approve',    menu_name='审批配置' WHERE menu_code='workhour:config:approve';
UPDATE tb_menu SET menu_code='workhour:invalidate', menu_name='作废配置' WHERE menu_code='workhour:config:invalidate';

-- ============================
-- 6. 员工管理按钮（employee:list id=8）
-- ============================
INSERT INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES
('employee:add',    '新增员工', 3, 8, 1, 1),
('employee:edit',   '编辑员工', 3, 8, 2, 1),
('employee:delete', '删除员工', 3, 8, 3, 1);

-- ============================
-- 7. 项目管理按钮（project:list id=10）
-- ============================
INSERT INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES
('project:add',    '新增项目', 3, 10, 1, 1),
('project:edit',   '编辑项目', 3, 10, 2, 1),
('project:delete', '删除项目', 3, 10, 3, 1),
('project:assign', '关联员工', 3, 10, 4, 1);

-- ============================
-- 8. 分类管理按钮（category:list id=6）
-- ============================
INSERT INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES
('category:add',    '新增分类', 3, 6, 1, 1),
('category:edit',   '编辑分类', 3, 6, 2, 1),
('category:delete', '删除分类', 3, 6, 3, 1);

-- ============================
-- 9. 角色管理按钮（role id=36）
-- ============================
INSERT INTO tb_menu(menu_code, menu_name, menu_type, parent_id, sort_order, status) VALUES
('role:edit', '配置权限', 3, 36, 1, 1);

-- ============================
-- 10. 为 root 角色（id=1）同步分配所有新增的按钮权限
-- ============================
INSERT IGNORE INTO tb_role_menu(role_id, menu_id)
SELECT 1, id FROM tb_menu WHERE menu_code IN (
  'account:add', 'account:edit', 'account:delete', 'account:approve',
  'attendance:add', 'attendance:edit', 'attendance:delete',
  'salary:add', 'salary:edit', 'salary:delete', 'salary:pay',
  'workhour:add', 'workhour:edit', 'workhour:delete', 'workhour:approve', 'workhour:invalidate',
  'employee:add', 'employee:edit', 'employee:delete',
  'project:add', 'project:edit', 'project:delete', 'project:assign',
  'category:add', 'category:edit', 'category:delete',
  'role:edit'
);
