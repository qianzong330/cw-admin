-- 将工时配置菜单移到工程目录(project_dir)下
UPDATE tb_menu SET parent_id = (SELECT id FROM (SELECT id FROM tb_menu WHERE menu_code = 'project_dir') AS tmp)
WHERE menu_code = 'workhour:config'
  AND EXISTS (SELECT 1 FROM (SELECT id FROM tb_menu WHERE menu_code = 'project_dir') AS chk);
