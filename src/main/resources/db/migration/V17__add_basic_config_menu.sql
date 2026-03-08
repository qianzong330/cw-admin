-- V17: 添加基础配置目录菜单，并将相关菜单调整为其子菜单

-- 1. 创建"基础配置"目录菜单（parent_id=0 表示一级目录）
INSERT INTO tb_menu (menu_code, menu_name, parent_id, menu_type, menu_url, menu_icon, sort_order, status) 
VALUES ('basic_config', '基础配置', 0, 1, NULL, 'bi-gear-wide-connected', 90, 1)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), parent_id = VALUES(parent_id), menu_type = VALUES(menu_type), menu_icon = VALUES(menu_icon);

-- 获取基础配置菜单的ID
SET @basic_config_id = (SELECT id FROM tb_menu WHERE menu_code = 'basic_config');

-- 2. 将工种管理、项目管理、权限分配、工时配置、费用分类调整为基础配置的子菜单
-- 工种管理
UPDATE tb_menu SET parent_id = @basic_config_id, menu_type = 2, sort_order = 1 
WHERE menu_code = 'jobcategory:list';

-- 项目管理
UPDATE tb_menu SET parent_id = @basic_config_id, menu_type = 2, sort_order = 2 
WHERE menu_code = 'project';

-- 权限分配（角色管理）
UPDATE tb_menu SET parent_id = @basic_config_id, menu_type = 2, sort_order = 3 
WHERE menu_code = 'role';

-- 工时配置
UPDATE tb_menu SET parent_id = @basic_config_id, menu_type = 2, sort_order = 4 
WHERE menu_code = 'workhour:config';

-- 费用分类
UPDATE tb_menu SET parent_id = @basic_config_id, menu_type = 2, sort_order = 5 
WHERE menu_code = 'category';

-- 3. 给 root 角色分配基础配置目录权限（如果不存在）
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT 1, id FROM tb_menu WHERE menu_code = 'basic_config';
