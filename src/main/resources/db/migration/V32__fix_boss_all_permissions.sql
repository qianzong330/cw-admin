-- 修复BOSS角色的所有菜单权限
-- 确保BOSS可以访问所有一级目录和子菜单

-- 1. 获取BOSS角色ID
SET @boss_role_id = (SELECT id FROM tb_role WHERE role_code = 'boss');

-- 2. 获取所有启用的一级目录（menu_type=1）
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @boss_role_id, id FROM tb_menu WHERE menu_type = 1 AND status = 1;

-- 3. 获取所有启用的页面菜单（menu_type=2）
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @boss_role_id, id FROM tb_menu WHERE menu_type = 2 AND status = 1;

-- 4. 获取所有启用的按钮菜单（menu_type=3）
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @boss_role_id, id FROM tb_menu WHERE menu_type = 3 AND status = 1;

-- 5. 确保记账管理(account)是独立一级菜单且BOSS有权限
UPDATE tb_menu SET parent_id = NULL, menu_type = 2, status = 1 WHERE menu_code = 'account';
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @boss_role_id, id FROM tb_menu WHERE menu_code = 'account';
