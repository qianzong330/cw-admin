-- 删除审批管理相关菜单权限
DELETE FROM tb_role_menu 
WHERE menu_id IN (
    SELECT id FROM (
        SELECT id FROM tb_menu WHERE menu_code LIKE 'approval%'
    ) AS tmp
);

-- 删除审批管理二级菜单
DELETE FROM tb_menu 
WHERE menu_code LIKE 'approval:%';

-- 删除审批管理一级菜单
DELETE FROM tb_menu 
WHERE menu_code = 'approval';
