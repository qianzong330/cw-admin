-- 修复乱码的菜单数据
UPDATE tb_menu SET menu_name = '审批管理' WHERE menu_code = 'approval';
UPDATE tb_menu SET menu_name = '工时管理' WHERE menu_code = 'workhour';
UPDATE tb_menu SET menu_name = 'HR 管理' WHERE menu_code = 'hr';
UPDATE tb_menu SET menu_name = '待审批管理' WHERE menu_code = 'approval:pending';
UPDATE tb_menu SET menu_name = '工时配置审批' WHERE menu_code = 'approval:workhour';
UPDATE tb_menu SET menu_name = '考勤审批' WHERE menu_code = 'approval:attendance';
UPDATE tb_menu SET menu_name = '工资条审批' WHERE menu_code = 'approval:salary';
UPDATE tb_menu SET menu_name = '工时配置' WHERE menu_code = 'workhour:config';
UPDATE tb_menu SET menu_name = '工时配置列表' WHERE menu_code = 'workhour:config:list';
UPDATE tb_menu SET menu_name = '新增配置' WHERE menu_code = 'workhour:config:add';
UPDATE tb_menu SET menu_name = '编辑配置' WHERE menu_code = 'workhour:config:edit';
UPDATE tb_menu SET menu_name = '删除配置' WHERE menu_code = 'workhour:config:delete';
UPDATE tb_menu SET menu_name = '审批配置' WHERE menu_code = 'workhour:config:approve';
UPDATE tb_menu SET menu_name = '作废配置' WHERE menu_code = 'workhour:config:invalidate';
