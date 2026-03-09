-- 修复BOSS角色目录权限
-- 确保BOSS可以访问所有一级目录

-- 1. 获取BOSS角色ID
SET @boss_role_id = (SELECT id FROM tb_role WHERE role_code = 'boss');

-- 2. 给BOSS分配所有目录权限（menu_type=1）
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT @boss_role_id, id FROM tb_menu WHERE menu_type = 1 AND status = 1;

-- 3. 特别确保以下目录权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id) VALUES 
(@boss_role_id, (SELECT id FROM tb_menu WHERE menu_code = 'system_settings')),
(@boss_role_id, (SELECT id FROM tb_menu WHERE menu_code = 'project_dir')),
(@boss_role_id, (SELECT id FROM tb_menu WHERE menu_code = 'basic_config'));

-- 4. 给root角色也分配所有权限（以防万一）
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT 1, id FROM tb_menu WHERE status = 1;
