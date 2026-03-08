-- V13: 更新费用分类文案 + 调整菜单排序与侧边栏一致
-- 侧边栏顺序：首页→记账管理→考勤管理→工资条管理→工时管理→角色管理→员工管理→费用分类→工种管理→项目管理

-- ============================
-- 1. 分类管理 文案改为 费用分类
-- ============================
UPDATE tb_menu SET menu_name = '费用分类' WHERE menu_code = 'category';
UPDATE tb_menu SET menu_name = '费用分类列表' WHERE menu_code = 'category:list';
UPDATE tb_menu SET menu_name = '新增费用分类' WHERE menu_code = 'category:add';
UPDATE tb_menu SET menu_name = '编辑费用分类' WHERE menu_code = 'category:edit';
UPDATE tb_menu SET menu_name = '删除费用分类' WHERE menu_code = 'category:delete';

-- ============================
-- 2. 调整一级菜单/入口 sort_order，与侧边栏顺序对齐
--    首页(index/home)=1, 记账(account)=2, 考勤(attendance)=3,
--    工资(salary)=4, 工时(workhour)=5, 角色(role)=6,
--    员工(employee)=7, 费用分类(category)=8, 工种(jobcategory)=9, 项目(project)=10
-- ============================
UPDATE tb_menu SET sort_order = 1  WHERE menu_code = 'account';
UPDATE tb_menu SET sort_order = 2  WHERE menu_code = 'attendance';
UPDATE tb_menu SET sort_order = 3  WHERE menu_code = 'salary';
UPDATE tb_menu SET sort_order = 4  WHERE menu_code = 'workhour' OR menu_code = 'workhour:config';
UPDATE tb_menu SET sort_order = 5  WHERE menu_code = 'role';
UPDATE tb_menu SET sort_order = 6  WHERE menu_code = 'employee';
UPDATE tb_menu SET sort_order = 7  WHERE menu_code = 'category';
UPDATE tb_menu SET sort_order = 8  WHERE menu_code = 'jobcategory';
UPDATE tb_menu SET sort_order = 9  WHERE menu_code = 'project';
