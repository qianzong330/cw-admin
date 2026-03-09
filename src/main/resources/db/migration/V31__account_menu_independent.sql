-- 将记账管理设为独立一级菜单
-- 1. 将account菜单的parent_id设为NULL，sort_order设为100（在工程管理和系统设置之间）
UPDATE tb_menu 
SET parent_id = NULL, 
    sort_order = 100,
    menu_type = 2,
    status = 1
WHERE menu_code = 'account';

-- 2. 确保所有角色都有account菜单权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT r.id, m.id 
FROM tb_role r, tb_menu m 
WHERE m.menu_code = 'account';
