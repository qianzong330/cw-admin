-- 修复BOSS角色的按钮权限
-- 确保BOSS角色拥有所有按钮权限

SET @boss_id = (SELECT id FROM tb_role WHERE role_code = 'boss');

-- 给BOSS角色分配所有按钮权限（menu_type=3）
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @boss_id, id FROM tb_menu WHERE menu_type = 3;

-- 同时确保root角色也拥有所有按钮权限
SET @root_id = (SELECT id FROM tb_role WHERE role_code = 'root');
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @root_id, id FROM tb_menu WHERE menu_type = 3;
