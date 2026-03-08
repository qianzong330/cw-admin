-- 删除审批管理菜单及其子菜单（审批功能移至原页面操作）
-- 先删除角色菜单关联
DELETE FROM tb_role_menu WHERE menu_id IN (SELECT id FROM tb_menu WHERE menu_code LIKE 'approval%');

-- 再删除菜单
DELETE FROM tb_menu WHERE menu_code LIKE 'approval%';
