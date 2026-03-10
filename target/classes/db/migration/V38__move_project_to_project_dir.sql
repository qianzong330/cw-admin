-- 将项目管理菜单移到工程管理目录(project_dir)下
UPDATE tb_menu 
SET parent_id = (SELECT id FROM (SELECT id FROM tb_menu WHERE menu_code = 'project_dir') AS tmp)
WHERE menu_code = 'project'
AND EXISTS (SELECT 1 FROM (SELECT id FROM tb_menu WHERE menu_code = 'project_dir') AS chk);

-- 确保工程管理目录存在
INSERT IGNORE INTO tb_menu (menu_code, menu_name, menu_type, menu_url, menu_icon, sort_order, status)
VALUES ('project_dir', '工程管理', 1, NULL, 'bi-building', 20, 1);

-- 确保项目管理菜单存在
INSERT IGNORE INTO tb_menu (menu_code, menu_name, menu_type, menu_url, menu_icon, parent_id, sort_order, status)
VALUES ('project', '项目管理', 2, '/project/list', 'bi-folder', 
        (SELECT id FROM tb_menu WHERE menu_code = 'project_dir'), 1, 1);

-- 给BOSS角色分配项目管理菜单权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT r.id, m.id 
FROM tb_role r, tb_menu m 
WHERE r.role_code = 'boss' AND m.menu_code = 'project';

-- 给财务角色分配项目管理菜单权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT r.id, m.id 
FROM tb_role r, tb_menu m 
WHERE r.role_code = 'finance' AND m.menu_code = 'project';
